package com.securechat.keyexchange;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/keys")
public class KeyExchangeController {

    private final KeyExchangeService keyExchangeService;
    private final com.securechat.conversation.ConversationService conversationService;

    public KeyExchangeController(KeyExchangeService keyExchangeService,
                                 com.securechat.conversation.ConversationService conversationService) {
        this.keyExchangeService = keyExchangeService;
        this.conversationService = conversationService;
    }

    @PostMapping("/exchange")
    public ResponseEntity<KeyExchangeResponse> exchangeKey(
            @PathVariable Long conversationId,
            @RequestBody KeyExchangeRequest request) {
        Long userId = getCurrentUserId();
        KeyMaterialEntity entity = keyExchangeService.storeKeyMaterial(
            userId,
            conversationId,
            request.getRecipientId(),
            request.getEncryptedAesKey(),
            request.getSignature()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(KeyExchangeResponse.from(entity));
    }

    @GetMapping("/pending")
    public ResponseEntity<KeyExchangeResponse> getPendingKey(@PathVariable Long conversationId) {
        Long userId = getCurrentUserId();
        KeyMaterialEntity entity = keyExchangeService.fetchPendingKey(conversationId, userId);
        return ResponseEntity.ok(KeyExchangeResponse.from(entity));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmKey(@PathVariable Long conversationId) {
        Long userId = getCurrentUserId();
        keyExchangeService.confirmKey(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
