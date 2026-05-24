package com.securechat.keyexchange;

import jakarta.validation.constraints.NotNull;

public class KeyExchangeRequest {

    @NotNull
    private Long recipientId;

    @NotNull
    private String encryptedAesKey;

    @NotNull
    private String signature;

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getEncryptedAesKey() {
        return encryptedAesKey;
    }

    public void setEncryptedAesKey(String encryptedAesKey) {
        this.encryptedAesKey = encryptedAesKey;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
