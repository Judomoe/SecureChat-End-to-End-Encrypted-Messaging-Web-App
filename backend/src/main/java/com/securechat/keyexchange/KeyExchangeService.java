package com.securechat.keyexchange;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.stereotype.Service;

import com.securechat.common.CryptoUtils;
import com.securechat.exception.KeyExchangeException;
import com.securechat.exception.ResourceNotFoundException;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@Service
public class KeyExchangeService {

    private final KeyMaterialRepository keyMaterialRepository;
    private final UserRepository userRepository;
    private final CryptoUtils cryptoUtils;

    public KeyExchangeService(KeyMaterialRepository keyMaterialRepository,
                              UserRepository userRepository,
                              CryptoUtils cryptoUtils) {
        this.keyMaterialRepository = keyMaterialRepository;
        this.userRepository = userRepository;
        this.cryptoUtils = cryptoUtils;
    }

    public KeyMaterialEntity storeKeyMaterial(Long senderId, Long conversationId, Long recipientId,
                                              String encryptedAesKey, String signature) {
        UserEntity sender = userRepository.findById(senderId)
            .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        PublicKey senderPublicKey = parsePemPublicKey(sender.getPublicKeyPem());

        byte[] dataBytes = CryptoUtils.base64Decode(encryptedAesKey);
        byte[] signatureBytes = CryptoUtils.base64Decode(signature);

        if (!CryptoUtils.rsaPssVerify(senderPublicKey, dataBytes, signatureBytes)) {
            throw new KeyExchangeException("Invalid signature on key material");
        }

        KeyMaterialEntity entity = new KeyMaterialEntity();
        entity.setConversationId(conversationId);
        entity.setRecipientId(recipientId);
        entity.setSenderId(senderId);
        entity.setEncryptedAesKey(encryptedAesKey);
        entity.setSignature(signature);

        return keyMaterialRepository.save(entity);
    }

    public KeyMaterialEntity fetchPendingKey(Long conversationId, Long recipientId) {
        return keyMaterialRepository.findByConversationIdAndRecipientIdAndIsActive(conversationId, recipientId, true)
            .orElseThrow(() -> new ResourceNotFoundException("No pending key material found"));
    }

    public void confirmKey(Long conversationId, Long recipientId) {
        KeyMaterialEntity keyMaterial = keyMaterialRepository
            .findByConversationIdAndRecipientIdAndIsActive(conversationId, recipientId, true)
            .orElseThrow(() -> new ResourceNotFoundException("No pending key material found"));

        keyMaterial.setConfirmed(true);
        keyMaterialRepository.save(keyMaterial);
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
            throw new KeyExchangeException("Failed to parse public key: " + e.getMessage());
        }
    }
}
