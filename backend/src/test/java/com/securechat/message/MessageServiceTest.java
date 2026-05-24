package com.securechat.message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.securechat.common.CryptoUtils;
import com.securechat.conversation.ConversationMemberRepository;
import com.securechat.keyexchange.KeyMaterialRepository;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private KeyMaterialRepository keyMaterialRepository;

    private CryptoUtils cryptoUtils;

    private MessageService messageService;

    private KeyPair keyPair;
    private String publicKeyPem;

    private static final Long CONVERSATION_ID = 10L;

    @BeforeEach
    void setUp() throws Exception {
        cryptoUtils = new CryptoUtils();
        messageService = new MessageService(messageRepository, userRepository, conversationMemberRepository, cryptoUtils, keyMaterialRepository);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        String base64Key = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded());
        publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + base64Key + "\n-----END PUBLIC KEY-----";
    }

    private UserEntity createSender() {
        UserEntity sender = new UserEntity();
        sender.setId(1L);
        sender.setUsername("alice");
        sender.setEmail("alice@example.com");
        sender.setPublicKeyPem(publicKeyPem);
        return sender;
    }

    private SendMessageRequest createValidRequest() throws Exception {
        byte[] hmacBytes = CryptoUtils.hmacSha256("key".getBytes(), "message".getBytes());
        String hmacBase64 = CryptoUtils.base64Encode(hmacBytes);

        Signature sig = Signature.getInstance("RSASSA-PSS");
        PSSParameterSpec pssSpec = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);
        sig.setParameter(pssSpec);
        sig.initSign(keyPair.getPrivate());
        sig.update(hmacBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(sig.sign());

        SendMessageRequest req = new SendMessageRequest();
        req.setCiphertext("encrypted-message-ciphertext");
        req.setIv("initialization-vector");
        req.setHmac(hmacBase64);
        req.setSignature(signatureBase64);
        req.setTimestamp(System.currentTimeMillis());
        return req;
    }

    @Test
    void sendMessage_withValidData_succeeds() throws Exception {
        SendMessageRequest req = createValidRequest();
        when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(createSender()));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> {
            MessageEntity m = invocation.getArgument(0);
            m.setId(100L);
            return m;
        });

        MessageEntity result = messageService.sendMessage(1L, CONVERSATION_ID, req);

        assertNotNull(result);
        assertEquals(CONVERSATION_ID, result.getConversationId());
        assertEquals(1L, result.getSenderId());
        assertEquals("encrypted-message-ciphertext", result.getCiphertext());
    }

    @Test
    void sendMessage_withExpiredTimestamp_throwsException() throws Exception {
        SendMessageRequest req = createValidRequest();
        req.setTimestamp(System.currentTimeMillis() - 600_000);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, CONVERSATION_ID, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void sendMessage_withNonMember_throwsException() throws Exception {
        SendMessageRequest req = createValidRequest();
        when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, 1L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, CONVERSATION_ID, req));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void sendMessage_withInvalidSignature_throwsException() throws Exception {
        SendMessageRequest req = createValidRequest();
        req.setSignature(Base64.getEncoder().encodeToString("totally-invalid-signature".getBytes()));

        when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(createSender()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.sendMessage(1L, CONVERSATION_ID, req));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getMessages_returnsPaginatedResults() {
        when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, 1L)).thenReturn(true);

        MessageEntity msg1 = new MessageEntity();
        msg1.setId(1L);
        msg1.setConversationId(CONVERSATION_ID);
        msg1.setSenderId(2L);
        MessageEntity msg2 = new MessageEntity();
        msg2.setId(2L);
        msg2.setConversationId(CONVERSATION_ID);
        msg2.setSenderId(1L);

        Page<MessageEntity> page = new PageImpl<>(List.of(msg2, msg1));
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(CONVERSATION_ID), any(Pageable.class))).thenReturn(page);

        List<MessageEntity> results = messageService.getMessages(CONVERSATION_ID, 1L, 50, null);

        assertEquals(2, results.size());
    }

    @Test
    void getMessages_withBeforeParameter_returnsOlderMessages() {
        when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, 1L)).thenReturn(true);

        MessageEntity msg = new MessageEntity();
        msg.setId(5L);
        msg.setConversationId(CONVERSATION_ID);
        msg.setSenderId(2L);

        when(messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(eq(CONVERSATION_ID), eq(100L), any(Pageable.class)))
            .thenReturn(List.of(msg));

        List<MessageEntity> results = messageService.getMessages(CONVERSATION_ID, 1L, 50, 100L);

        assertEquals(1, results.size());
        assertEquals(5L, results.get(0).getId());
    }

    @Test
    void getMessages_forNonMember_throwsException() {
        when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, 999L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> messageService.getMessages(CONVERSATION_ID, 999L, 50, null));
        assertEquals(403, ex.getStatusCode().value());
    }
}
