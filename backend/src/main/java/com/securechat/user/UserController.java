package com.securechat.user;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Long userId = getCurrentUserId();
        UserEntity user = userService.getUserById(userId);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("publicKeyPem", user.getPublicKeyPem());
        userMap.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

        return ResponseEntity.ok(userMap);
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam("q") String email) {
        Long currentUserId = getCurrentUserId();
        List<UserEntity> users = userService.searchUsers(email, currentUserId);

        List<Map<String, Object>> results = users.stream()
            .map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("username", user.getUsername());
                map.put("email", user.getEmail());
                map.put("displayName", user.getDisplayName());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/users/{id}/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey(@PathVariable Long id) {
        String publicKeyPem = userService.getPublicKey(id);
        return ResponseEntity.ok(Map.of("publicKeyPem", publicKeyPem));
    }

    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
