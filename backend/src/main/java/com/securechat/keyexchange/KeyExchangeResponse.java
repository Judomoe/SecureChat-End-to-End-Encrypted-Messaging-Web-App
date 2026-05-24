package com.securechat.keyexchange;

public class KeyExchangeResponse {

    private Long id;
    private String encryptedAesKey;
    private String signature;
    private Long senderId;
    private String issuedAt;
    private Boolean confirmed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public static KeyExchangeResponse from(KeyMaterialEntity entity) {
        KeyExchangeResponse response = new KeyExchangeResponse();
        response.setId(entity.getId());
        response.setEncryptedAesKey(entity.getEncryptedAesKey());
        response.setSignature(entity.getSignature());
        response.setSenderId(entity.getSenderId());
        response.setIssuedAt(entity.getIssuedAt() != null ? entity.getIssuedAt().toString() : null);
        response.setConfirmed(entity.getConfirmed());
        return response;
    }
}
