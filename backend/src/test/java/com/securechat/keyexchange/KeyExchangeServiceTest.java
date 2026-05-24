package com.securechat.keyexchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securechat.common.CryptoUtils;
import com.securechat.exception.KeyExchangeException;
import com.securechat.exception.ResourceNotFoundException;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class KeyExchangeServiceTest {

    @Mock
    private KeyMaterialRepository keyMaterialRepository;

    @Mock
    private UserRepository userRepository;

    private CryptoUtils cryptoUtils;

    private KeyExchangeService keyExchangeService;

    private KeyPair keyPair;
    private String publicKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        cryptoUtils = new CryptoUtils();
        keyExchangeService = new KeyExchangeService(keyMaterialRepository, userRepository, cryptoUtils);

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

    private String signData(byte[] data) throws Exception {
        Signature sig = Signature.getInstance("RSASSA-PSS");
        PSSParameterSpec pssSpec = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);
        sig.setParameter(pssSpec);
        sig.initSign(keyPair.getPrivate());
        sig.update(data);
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    @Test
    void storeKeyMaterial_withValidSignature_succeeds() throws Exception {
        UserEntity sender = createSender();
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        // Simulate what frontend does: encrypt AES key bytes, base64-encode, sign raw bytes
        byte[] rawEncryptedKey = new byte[256]; // RSA-2048 encrypted data size
        new java.security.SecureRandom().nextBytes(rawEncryptedKey);
        String encryptedAesKey = CryptoUtils.base64Encode(rawEncryptedKey);
        String signature = signData(rawEncryptedKey); // sign raw bytes, not base64 string

        when(keyMaterialRepository.save(any(KeyMaterialEntity.class))).thenAnswer(invocation -> {
            KeyMaterialEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        KeyMaterialEntity result = keyExchangeService.storeKeyMaterial(1L, 10L, 2L, encryptedAesKey, signature);

        assertNotNull(result);
        assertEquals(10L, result.getConversationId());
        assertEquals(2L, result.getRecipientId());
        assertEquals(1L, result.getSenderId());
        assertEquals(encryptedAesKey, result.getEncryptedAesKey());
    }

    @Test
    void storeKeyMaterial_withInvalidSignature_throwsKeyExchangeException() throws Exception {
        UserEntity sender = createSender();
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        byte[] rawEncryptedKey = new byte[256];
        new java.security.SecureRandom().nextBytes(rawEncryptedKey);
        String encryptedAesKey = CryptoUtils.base64Encode(rawEncryptedKey);
        String invalidSignature = Base64.getEncoder().encodeToString("invalid".getBytes());

        assertThrows(KeyExchangeException.class,
            () -> keyExchangeService.storeKeyMaterial(1L, 10L, 2L, encryptedAesKey, invalidSignature));
    }

    @Test
    void fetchPendingKey_returnsKeyMaterial() {
        KeyMaterialEntity entity = new KeyMaterialEntity();
        entity.setId(1L);
        entity.setConversationId(10L);
        entity.setRecipientId(2L);
        entity.setSenderId(1L);
        entity.setEncryptedAesKey("aes-key");
        entity.setIsActive(true);
        when(keyMaterialRepository.findByConversationIdAndRecipientIdAndIsActive(10L, 2L, true))
            .thenReturn(Optional.of(entity));

        KeyMaterialEntity result = keyExchangeService.fetchPendingKey(10L, 2L);

        assertNotNull(result);
        assertEquals(10L, result.getConversationId());
    }

    @Test
    void fetchPendingKey_notFound_throwsResourceNotFoundException() {
        when(keyMaterialRepository.findByConversationIdAndRecipientIdAndIsActive(10L, 2L, true))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> keyExchangeService.fetchPendingKey(10L, 2L));
    }

    @Test
    void confirmKey_setsConfirmedToTrue() {
        KeyMaterialEntity entity = new KeyMaterialEntity();
        entity.setId(1L);
        entity.setConversationId(10L);
        entity.setRecipientId(2L);
        entity.setConfirmed(false);
        when(keyMaterialRepository.findByConversationIdAndRecipientIdAndIsActive(10L, 2L, true))
            .thenReturn(Optional.of(entity));
        when(keyMaterialRepository.save(any(KeyMaterialEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        keyExchangeService.confirmKey(10L, 2L);

        assertTrue(entity.getConfirmed());
        verify(keyMaterialRepository).save(entity);
    }
}
