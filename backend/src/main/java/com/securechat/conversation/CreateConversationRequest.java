package com.securechat.conversation;

import jakarta.validation.constraints.NotNull;

public class CreateConversationRequest {

    @NotNull
    private Long recipientId;

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }
}
