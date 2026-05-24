package com.securechat.message;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/api/v1/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody @Valid SendMessageRequest request) {
        Long userId = getCurrentUserId();
        MessageEntity message = messageService.sendMessage(userId, conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(message));
    }

    @GetMapping("/api/v1/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long before) {
        Long userId = getCurrentUserId();
        List<MessageEntity> messages = messageService.getMessages(id, userId, limit, before);

        List<MessageResponse> responses = messages.stream()
            .map(MessageResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    private Long getCurrentUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
