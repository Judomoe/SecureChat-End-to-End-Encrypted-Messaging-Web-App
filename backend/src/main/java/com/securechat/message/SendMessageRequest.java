package com.securechat.message;

import jakarta.validation.constraints.NotNull;

public class SendMessageRequest {

    @NotNull
    private String ciphertext;

    @NotNull
    private String iv;

    @NotNull
    private String hmac;

    @NotNull
    private String signature;

    @NotNull
    private Long timestamp;

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
}
