package com.securechat.contact;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securechat.user.UserEntity;
import com.securechat.user.UserService;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;
    private final UserService userService;

    public ContactController(ContactService contactService, UserService userService) {
        this.contactService = contactService;
        this.userService = userService;
    }

    @PostMapping("/request")
    public ResponseEntity<ContactResponse> sendRequest(@RequestBody ContactRequestDto request) {
        Long userId = getCurrentUserId();
        ContactEntity contact = contactService.sendRequest(userId, request.getRecipientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(contact, userId));
    }

    @GetMapping
    public ResponseEntity<List<ContactResponse>> listContacts() {
        Long userId = getCurrentUserId();
        List<ContactEntity> contacts = contactService.listContacts(userId);
        List<ContactResponse> responses = contacts.stream()
            .map(c -> toResponse(c, userId))
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ContactResponse>> pendingRequests() {
        Long userId = getCurrentUserId();
        List<ContactEntity> contacts = contactService.getPendingRequests(userId);
        List<ContactResponse> responses = contacts.stream()
            .map(c -> toResponse(c, userId))
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<ContactResponse>> sentRequests() {
        Long userId = getCurrentUserId();
        List<ContactEntity> contacts = contactService.getSentRequests(userId);
        List<ContactResponse> responses = contacts.stream()
            .map(c -> toResponse(c, userId))
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ContactResponse> acceptRequest(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        ContactEntity contact = contactService.acceptRequest(id, userId);
        return ResponseEntity.ok(toResponse(contact, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeContact(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        contactService.removeContact(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }

    private ContactResponse toResponse(ContactEntity contact, Long currentUserId) {
        ContactResponse response = new ContactResponse();
        response.setId(contact.getId());
        response.setRequesterId(contact.getRequesterId());
        response.setRecipientId(contact.getRecipientId());
        response.setStatus(contact.getStatus());
        response.setCreatedAt(contact.getCreatedAt() != null ? contact.getCreatedAt().toString() : null);

        Long otherUserId = contact.getRequesterId().equals(currentUserId) ? contact.getRecipientId() : contact.getRequesterId();
        UserEntity otherUser = userService.getUserById(otherUserId);

        ContactResponse.UserSummary summary = new ContactResponse.UserSummary();
        summary.setId(otherUser.getId());
        summary.setUsername(otherUser.getUsername());
        summary.setDisplayName(otherUser.getDisplayName());
        response.setOtherUser(summary);

        return response;
    }
}
