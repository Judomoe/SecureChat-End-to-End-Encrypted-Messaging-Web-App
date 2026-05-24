package com.securechat.common;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import org.junit.jupiter.api.Test;

class CryptoUtilsTest {

    @Test
    void rsaPssVerify_withValidSignature_returnsTrue() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        byte[] data = "test data for signing".getBytes();
        Signature sig = Signature.getInstance("RSASSA-PSS");
        PSSParameterSpec pssSpec = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);
        sig.setParameter(pssSpec);
        sig.initSign(keyPair.getPrivate());
        sig.update(data);
        byte[] signatureBytes = sig.sign();

        assertTrue(CryptoUtils.rsaPssVerify(keyPair.getPublic(), data, signatureBytes));
    }

    @Test
    void rsaPssVerify_withWrongData_returnsFalse() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        byte[] data = "test data for signing".getBytes();
        Signature sig = Signature.getInstance("RSASSA-PSS");
        PSSParameterSpec pssSpec = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);
        sig.setParameter(pssSpec);
        sig.initSign(keyPair.getPrivate());
        sig.update(data);
        byte[] signatureBytes = sig.sign();

        byte[] wrongData = "wrong data".getBytes();
        assertFalse(CryptoUtils.rsaPssVerify(keyPair.getPublic(), wrongData, signatureBytes));
    }

    @Test
    void rsaPssVerify_withWrongSignature_returnsFalse() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        byte[] data = "test data".getBytes();
        byte[] wrongSignature = "this is not a valid signature at all".getBytes();

        assertFalse(CryptoUtils.rsaPssVerify(keyPair.getPublic(), data, wrongSignature));
    }

    @Test
    void hmacSha256_producesConsistentResults() {
        byte[] key = "secret-key-12345".getBytes();
        byte[] message = "hello world".getBytes();

        byte[] result1 = CryptoUtils.hmacSha256(key, message);
        byte[] result2 = CryptoUtils.hmacSha256(key, message);

        assertArrayEquals(result1, result2);
    }

    @Test
    void hmacSha256_producesDifferentResultsForDifferentInputs() {
        byte[] key = "secret-key-12345".getBytes();
        byte[] message1 = "hello world".getBytes();
        byte[] message2 = "hello world!".getBytes();

        byte[] result1 = CryptoUtils.hmacSha256(key, message1);
        byte[] result2 = CryptoUtils.hmacSha256(key, message2);

        assertFalse(java.util.Arrays.equals(result1, result2));
    }

    @Test
    void constantTimeEquals_withEqualArrays_returnsTrue() {
        byte[] a = {0x01, 0x02, 0x03, 0x04};
        byte[] b = {0x01, 0x02, 0x03, 0x04};

        assertTrue(CryptoUtils.constantTimeEquals(a, b));
    }

    @Test
    void constantTimeEquals_withDifferentArrays_returnsFalse() {
        byte[] a = {0x01, 0x02, 0x03, 0x04};
        byte[] b = {0x01, 0x02, 0x03, 0x05};

        assertFalse(CryptoUtils.constantTimeEquals(a, b));
    }

    @Test
    void constantTimeEquals_withDifferentLengthArrays_returnsFalse() {
        byte[] a = {0x01, 0x02, 0x03};
        byte[] b = {0x01, 0x02};

        assertFalse(CryptoUtils.constantTimeEquals(a, b));
    }

    @Test
    void isTimestampValid_withCurrentTimestamp_returnsTrue() {
        long now = System.currentTimeMillis();
        assertTrue(CryptoUtils.isTimestampValid(now, 300_000));
    }

    @Test
    void isTimestampValid_withOldTimestamp_returnsFalse() {
        long oldTimestamp = System.currentTimeMillis() - 600_000;
        assertFalse(CryptoUtils.isTimestampValid(oldTimestamp, 300_000));
    }

    @Test
    void isTimestampValid_withFutureTimestamp_returnsFalse() {
        long futureTimestamp = System.currentTimeMillis() + 600_000;
        assertFalse(CryptoUtils.isTimestampValid(futureTimestamp, 300_000));
    }

    @Test
    void isTimestampValid_withTimestampAtBoundary_returnsTrue() {
        long boundaryTimestamp = System.currentTimeMillis() - 300_000;
        assertTrue(CryptoUtils.isTimestampValid(boundaryTimestamp, 300_000));
    }

    @Test
    void base64EncodeAndDecode_roundTrip() {
        byte[] original = {0x01, 0x02, 0x03, 0x04, 0x05, (byte) 0xFF, (byte) 0xFE};
        String encoded = CryptoUtils.base64Encode(original);
        byte[] decoded = CryptoUtils.base64Decode(encoded);

        assertArrayEquals(original, decoded);
    }
}
