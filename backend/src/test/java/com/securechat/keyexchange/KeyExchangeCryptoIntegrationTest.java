package com.securechat.keyexchange;

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
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securechat.common.CryptoUtils;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class KeyExchangeCryptoIntegrationTest {

    @Mock private KeyMaterialRepository keyMaterialRepository;
    @Mock private UserRepository userRepository;

    private CryptoUtils cryptoUtils;
    private KeyExchangeService keyExchangeService;

    private KeyPair senderKeyPair;
    private KeyPair recipientKeyPair;
    private String senderPublicKeyPem;
    private String recipientPublicKeyPem;

    private SecretKey aesKey;
    private byte[] encryptedKeyBytes;
    private String encryptedAesKey;

    private static final Long SENDER_ID = 1L;
    private static final Long RECIPIENT_ID = 2L;
    private static final Long CONVERSATION_ID = 10L;

    @BeforeEach
    void setUp() throws Exception {
        cryptoUtils = new CryptoUtils();
        keyExchangeService = new KeyExchangeService(keyMaterialRepository, userRepository, cryptoUtils);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        senderKeyPair = keyGen.generateKeyPair();
        recipientKeyPair = keyGen.generateKeyPair();

        senderPublicKeyPem = toPem(senderKeyPair.getPublic());
        recipientPublicKeyPem = toPem(recipientKeyPair.getPublic());

        KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(256);
        aesKey = aesGen.generateKey();

        // Frontend: RSA-OAEP encrypt raw AES key with recipient's public key
        Cipher oaepCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        oaepCipher.init(Cipher.ENCRYPT_MODE, recipientKeyPair.getPublic());
        encryptedKeyBytes = oaepCipher.doFinal(aesKey.getEncoded());
        encryptedAesKey = CryptoUtils.base64Encode(encryptedKeyBytes);
    }

    private String toPem(PublicKey key) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----";
    }

    @Test
    void storeAndRecoverKey_fullRoundTrip_succeeds() throws Exception {
        UserEntity sender = new UserEntity();
        sender.setId(SENDER_ID);
        sender.setUsername("alice");
        sender.setEmail("alice@test.com");
        sender.setPublicKeyPem(senderPublicKeyPem);
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));

        // Frontend: sign the RAW encrypted key bytes (not base64 string)
        Signature pssSigner = Signature.getInstance("RSASSA-PSS");
        pssSigner.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        pssSigner.initSign(senderKeyPair.getPrivate());
        pssSigner.update(encryptedKeyBytes);
        String signature = CryptoUtils.base64Encode(pssSigner.sign());

        when(keyMaterialRepository.save(any(KeyMaterialEntity.class))).thenAnswer(inv -> {
            KeyMaterialEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        KeyMaterialEntity stored = keyExchangeService.storeKeyMaterial(
            SENDER_ID, CONVERSATION_ID, RECIPIENT_ID, encryptedAesKey, signature);

        assertNotNull(stored);
        assertEquals(encryptedAesKey, stored.getEncryptedAesKey());

        // Recipient can decrypt the key
        Cipher oaepDecrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        oaepDecrypt.init(Cipher.DECRYPT_MODE, recipientKeyPair.getPrivate());
        byte[] decryptedKeyBytes = oaepDecrypt.doFinal(CryptoUtils.base64Decode(stored.getEncryptedAesKey()));

        SecretKey recovered = new SecretKeySpec(decryptedKeyBytes, "AES");
        assertArrayEquals(aesKey.getEncoded(), recovered.getEncoded(),
            "Recovered AES key should match original");
    }

    @Test
    void signingBase64String_failsVerificationAgainstDecodedBytes() throws Exception {
        UserEntity sender = new UserEntity();
        sender.setId(SENDER_ID);
        sender.setUsername("alice");
        sender.setEmail("alice@test.com");
        sender.setPublicKeyPem(senderPublicKeyPem);
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));

        // BUG: sign the base64 STRING instead of raw encrypted bytes
        Signature pssSigner = Signature.getInstance("RSASSA-PSS");
        pssSigner.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        pssSigner.initSign(senderKeyPair.getPrivate());
        pssSigner.update(encryptedAesKey.getBytes()); // BUG: signing UTF-8 bytes of base64
        String buggySignature = CryptoUtils.base64Encode(pssSigner.sign());

        // Server verifies against decoded raw bytes
        assertThrows(com.securechat.exception.KeyExchangeException.class,
            () -> keyExchangeService.storeKeyMaterial(SENDER_ID, CONVERSATION_ID, RECIPIENT_ID,
                encryptedAesKey, buggySignature),
            "Key exchange should fail when signing base64 string but verifying decoded bytes");
    }
}
