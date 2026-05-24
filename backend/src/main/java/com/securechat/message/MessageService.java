package com.securechat.message;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.securechat.common.CryptoUtils;
import com.securechat.conversation.ConversationMemberRepository;
import com.securechat.exception.ResourceNotFoundException;
import com.securechat.keyexchange.KeyMaterialRepository;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final CryptoUtils cryptoUtils;
    private final KeyMaterialRepository keyMaterialRepository;

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          ConversationMemberRepository conversationMemberRepository,
                          CryptoUtils cryptoUtils,
                          KeyMaterialRepository keyMaterialRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.cryptoUtils = cryptoUtils;
        this.keyMaterialRepository = keyMaterialRepository;
    }

    public MessageEntity sendMessage(Long senderId, Long conversationId, SendMessageRequest req) {
        if (!CryptoUtils.isTimestampValid(req.getTimestamp(), 300_000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired timestamp");
        }

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }

        UserEntity sender = userRepository.findById(senderId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PublicKey senderPublicKey = parsePemPublicKey(sender.getPublicKeyPem());

        byte[] hmacBytes = CryptoUtils.base64Decode(req.getHmac());
        byte[] signatureBytes = CryptoUtils.base64Decode(req.getSignature());

        if (!CryptoUtils.rsaPssVerify(senderPublicKey, hmacBytes, signatureBytes)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid message signature");
        }

        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setCiphertext(req.getCiphertext());
        message.setIv(req.getIv());
        message.setHmac(req.getHmac());
        message.setSignature(req.getSignature());
        message.setTimestamp(req.getTimestamp());

        return messageRepository.save(message);
    }

    public List<MessageEntity> getMessages(Long conversationId, Long userId, int limit, Long beforeId) {
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }

        if (beforeId == null) {
            return messageRepository.findByConversationIdOrderByIdDesc(conversationId, PageRequest.of(0, limit))
                .getContent();
        } else {
            return messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(
                conversationId, beforeId, PageRequest.of(0, limit));
        }
    }

    private PublicKey parsePemPublicKey(String pem) {
        try {
            String publicKeyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

            byte[] keyBytes = CryptoUtils.base64Decode(publicKeyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse public key");
        }
    }
}
