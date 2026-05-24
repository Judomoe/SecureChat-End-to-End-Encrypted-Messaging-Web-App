package com.securechat.message;

public class MessageResponse {

    private Long id;
    private Long senderId;
    private String ciphertext;
    private String iv;
    private String hmac;
    private String signature;
    private Long timestamp;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getHmac() {
        return hmac;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public static MessageResponse from(MessageEntity entity) {
        MessageResponse response = new MessageResponse();
        response.setId(entity.getId());
        response.setSenderId(entity.getSenderId());
        response.setCiphertext(entity.getCiphertext());
        response.setIv(entity.getIv());
        response.setHmac(entity.getHmac());
        response.setSignature(entity.getSignature());
        response.setTimestamp(entity.getTimestamp());
        response.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        return response;
    }
}
