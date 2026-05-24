package com.securechat.common;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class CryptoUtils {

    private static final String RSA_PSS_ALGORITHM = "RSASSA-PSS";
    private static final String HMAC_SHA256 = "HmacSHA256";

    public static boolean rsaPssVerify(PublicKey publicKey, byte[] data, byte[] signature) {
        try {
            Signature sig = Signature.getInstance(RSA_PSS_ALGORITHM);
            java.security.spec.PSSParameterSpec pssSpec = new java.security.spec.PSSParameterSpec(
                "SHA-256", "MGF1", java.security.spec.MGF1ParameterSpec.SHA256, 32, 1
            );
            sig.setParameter(pssSpec);
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException |
                 java.security.InvalidAlgorithmParameterException e) {
            return false;
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(key, HMAC_SHA256);
            mac.init(secretKey);
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public static boolean isTimestampValid(long timestamp, long windowMs) {
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - timestamp);
        return diff <= windowMs;
    }

    public static byte[] base64Decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
