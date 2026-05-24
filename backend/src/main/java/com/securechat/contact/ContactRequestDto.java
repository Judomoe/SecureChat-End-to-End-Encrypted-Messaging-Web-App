package com.securechat.contact;

import jakarta.validation.constraints.NotNull;

public class ContactRequestDto {

    @NotNull
    private Long recipientId;

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }
}
