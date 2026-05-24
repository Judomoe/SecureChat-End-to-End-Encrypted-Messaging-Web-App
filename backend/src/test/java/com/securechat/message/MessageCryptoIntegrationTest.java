package com.securechat.message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securechat.common.CryptoUtils;
import com.securechat.conversation.ConversationMemberRepository;
import com.securechat.keyexchange.KeyMaterialRepository;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class MessageCryptoIntegrationTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConversationMemberRepository conversationMemberRepository;
    @Mock private KeyMaterialRepository keyMaterialRepository;

    private CryptoUtils cryptoUtils;
    private MessageService messageService;

    // User A (sender) keys
    private KeyPair senderKeyPair;
    private String senderPublicKeyPem;

    // User B (recipient) keys
    private KeyPair recipientKeyPair;
    private String recipientPublicKeyPem;

    // Conversation AES key
    private SecretKey aesKey;
    private String aesKeyBase64;

    private static final Long SENDER_ID = 1L;
    private static final Long RECIPIENT_ID = 2L;
    private static final Long CONVERSATION_ID = 10L;

    @BeforeEach
    void setUp() throws Exception {
        cryptoUtils = new CryptoUtils();
        messageService = new MessageService(messageRepository, userRepository,
            conversationMemberRepository, cryptoUtils, keyMaterialRepository);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);

        senderKeyPair = keyGen.generateKeyPair();
        recipientKeyPair = keyGen.generateKeyPair();

        senderPublicKeyPem = toPem(senderKeyPair.getPublic());
        recipientPublicKeyPem = toPem(recipientKeyPair.getPublic());

        KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(256);
        aesKey = aesGen.generateKey();
        aesKeyBase64 = CryptoUtils.base64Encode(aesKey.getEncoded());
    }

    private String toPem(PublicKey key) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----";
    }

    private UserEntity createUser(Long id, String username, String publicKeyPem) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPublicKeyPem(publicKeyPem);
        return user;
    }

    /**
     * Simulates frontend: RSA-OAEP encrypt raw AES key bytes, base64-encode result,
     * RSA-PSS sign the RAW bytes (not the base64 string).
     */
    @Test
    void keyExchangeCryptoRoundTrip_succeeds() throws Exception {
        // Frontend-style: encrypt AES key with recipient's public key (RSA-OAEP)
        Cipher oaepCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        oaepCipher.init(Cipher.ENCRYPT_MODE, recipientKeyPair.getPublic());
        byte[] encryptedKeyBytes = oaepCipher.doFinal(aesKey.getEncoded());

        // Frontend: base64-encode encrypted bytes
        String encryptedAesKey = CryptoUtils.base64Encode(encryptedKeyBytes);

        // Frontend: RSA-PSS sign the RAW encrypted bytes
        Signature pssSigner = Signature.getInstance("RSASSA-PSS");
        pssSigner.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        pssSigner.initSign(senderKeyPair.getPrivate());
        pssSigner.update(encryptedKeyBytes);
        String signature = CryptoUtils.base64Encode(pssSigner.sign());

        // Backend: verify signature against base64Decode(encryptedAesKey)
        byte[] decodedKey = CryptoUtils.base64Decode(encryptedAesKey);
        byte[] decodedSig = CryptoUtils.base64Decode(signature);
        boolean valid = CryptoUtils.rsaPssVerify(senderKeyPair.getPublic(), decodedKey, decodedSig);
        assertTrue(valid, "Key exchange signature should verify successfully");

        // Backend: decrypt with recipient's private key to recover AES key
        Cipher oaepDecrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        oaepDecrypt.init(Cipher.DECRYPT_MODE, recipientKeyPair.getPrivate());
        byte[] decryptedKeyBytes = oaepDecrypt.doFinal(decodedKey);

        SecretKey recoveredKey = new SecretKeySpec(decryptedKeyBytes, "AES");
        assertEquals(aesKeyBase64, CryptoUtils.base64Encode(recoveredKey.getEncoded()),
            "Recovered AES key should match original");
    }

    /**
     * Full message send/receive crypto round-trip: AES-GCM encrypt, HMAC, sign HMAC raw bytes,
     * verify signature against decoded HMAC, decrypt.
     */
    @Test
    void messageCryptoRoundTrip_succeeds() throws Exception {
        String plaintext = "Hello from sender to recipient!";

        // === FRONTEND: Encrypt message ===

        // AES-GCM encrypt
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] associatedData = new byte[0];
        aesCipher.updateAAD(associatedData);
        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes());

        String ciphertextB64 = CryptoUtils.base64Encode(ciphertext);
        String ivB64 = CryptoUtils.base64Encode(iv);
        long timestamp = System.currentTimeMillis();

        // Compute HMAC-SHA256 over ciphertext+iv+timestamp
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(aesKey.getEncoded(), "HmacSHA256"));
        String hmacMessage = ciphertextB64 + ivB64 + String.valueOf(timestamp);
        byte[] hmacBytes = hmac.doFinal(hmacMessage.getBytes());
        String hmacB64 = CryptoUtils.base64Encode(hmacBytes);

        // Sign the RAW HMAC bytes (not the base64 string)
        Signature pssSigner = Signature.getInstance("RSASSA-PSS");
        pssSigner.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        pssSigner.initSign(senderKeyPair.getPrivate());
        pssSigner.update(hmacBytes);
        byte[] sigBytes = pssSigner.sign();
        String signatureB64 = CryptoUtils.base64Encode(sigBytes);

        // === BACKEND: Verify and store ===

        // Backend decodes HMAC and signature, verifies signature against decoded HMAC bytes
        byte[] serverHmacBytes = CryptoUtils.base64Decode(hmacB64);
        byte[] serverSigBytes = CryptoUtils.base64Decode(signatureB64);
        boolean valid = CryptoUtils.rsaPssVerify(senderKeyPair.getPublic(), serverHmacBytes, serverSigBytes);
        assertTrue(valid, "Message signature should verify successfully");

        // Verify HMAC integrity
        Mac serverHmac = Mac.getInstance("HmacSHA256");
        serverHmac.init(new SecretKeySpec(aesKey.getEncoded(), "HmacSHA256"));
        String serverHmacMessage = ciphertextB64 + ivB64 + String.valueOf(timestamp);
        byte[] serverHmacResult = serverHmac.doFinal(serverHmacMessage.getBytes());
        assertArrayEquals(serverHmacBytes, serverHmacResult, "HMAC should match");

        // === RECIPIENT: Decrypt message ===

        Cipher aesDecrypt = Cipher.getInstance("AES/GCM/NoPadding");
        aesDecrypt.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, CryptoUtils.base64Decode(ivB64)));
        aesDecrypt.updateAAD(new byte[0]);
        byte[] decrypted = aesDecrypt.doFinal(CryptoUtils.base64Decode(ciphertextB64));

        String decryptedText = new String(decrypted);
        assertEquals(plaintext, decryptedText, "Decrypted text should match original");
    }

    /**
     * Full sendMessage flow with mocked repos but real crypto.
     */
    @Test
    void sendMessage_fullCryptoFlow_succeeds() throws Exception {
        String plaintext = "Test message through MessageService";

        // Setup mocks
        when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, SENDER_ID))
            .thenReturn(true);
        when(userRepository.findById(SENDER_ID))
            .thenReturn(Optional.of(createUser(SENDER_ID, "alice", senderPublicKeyPem)));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> {
            MessageEntity m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        // Build SendMessageRequest like the frontend does
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes());

        String ciphertextB64 = CryptoUtils.base64Encode(ciphertext);
        String ivB64 = CryptoUtils.base64Encode(iv);
        long timestamp = System.currentTimeMillis();

        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(aesKey.getEncoded(), "HmacSHA256"));
        String hmacMessage = ciphertextB64 + ivB64 + String.valueOf(timestamp);
        byte[] hmacBytes = hmac.doFinal(hmacMessage.getBytes());
        String hmacB64 = CryptoUtils.base64Encode(hmacBytes);

        // Sign RAW HMAC bytes
        Signature pssSigner = Signature.getInstance("RSASSA-PSS");
        pssSigner.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        pssSigner.initSign(senderKeyPair.getPrivate());
        pssSigner.update(hmacBytes);
        String signatureB64 = CryptoUtils.base64Encode(pssSigner.sign());

        SendMessageRequest request = new SendMessageRequest();
        request.setCiphertext(ciphertextB64);
        request.setIv(ivB64);
        request.setHmac(hmacB64);
        request.setSignature(signatureB64);
        request.setTimestamp(timestamp);

        MessageEntity result = messageService.sendMessage(SENDER_ID, CONVERSATION_ID, request);

        assertNotNull(result);
        assertEquals(CONVERSATION_ID, result.getConversationId());
        assertEquals(SENDER_ID, result.getSenderId());
        assertEquals(ciphertextB64, result.getCiphertext());

        // Verify recipient can decrypt
        Cipher aesDecrypt = Cipher.getInstance("AES/GCM/NoPadding");
        aesDecrypt.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, CryptoUtils.base64Decode(result.getIv())));
        byte[] decrypted = aesDecrypt.doFinal(CryptoUtils.base64Decode(result.getCiphertext()));
        assertEquals(plaintext, new String(decrypted), "Recipient should be able to decrypt the message");
    }

    /**
     * Verify that signing the base64 string (old buggy behavior) does NOT match
     * the server's verification against decoded raw bytes.
     */
    @Test
    void signingBase64String_failsVerificationAgainstDecodedBytes() throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] ciphertext = aesCipher.doFinal("test".getBytes());
        long timestamp = System.currentTimeMillis();

        String ciphertextB64 = CryptoUtils.base64Encode(ciphertext);
        String ivB64 = CryptoUtils.base64Encode(iv);

        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(aesKey.getEncoded(), "HmacSHA256"));
        byte[] hmacBytes = hmac.doFinal((ciphertextB64 + ivB64 + timestamp).getBytes());
        String hmacB64 = CryptoUtils.base64Encode(hmacBytes);

        // Sign the base64 STRING (old buggy behavior) instead of raw bytes
        Signature pssSigner = Signature.getInstance("RSASSA-PSS");
        pssSigner.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        pssSigner.initSign(senderKeyPair.getPrivate());
        pssSigner.update(hmacB64.getBytes()); // BUG: signing base64 string bytes
        byte[] sigBytes = pssSigner.sign();

        // Server verifies against decoded RAW HMAC bytes
        byte[] decodedHmac = CryptoUtils.base64Decode(hmacB64);
        boolean valid = CryptoUtils.rsaPssVerify(senderKeyPair.getPublic(), decodedHmac, sigBytes);

        assertFalse(valid, "Signing base64 string should NOT verify against decoded raw bytes");
    }
}
