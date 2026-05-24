package com.securechat.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.securechat.exception.ResourceNotFoundException;
import com.securechat.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private RegisterRequest createValidRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123");
        req.setDisplayName("Alice");
        req.setPublicKeyPem("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA");
        req.setEncryptedPrivateKey("encryptedKey");
        req.setPrivateKeyIv("ivValue");
        return req;
    }

    private UserEntity createUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashedPassword");
        user.setDisplayName("Alice");
        user.setPublicKeyPem("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA");
        user.setEncryptedPrivateKey("encryptedKey");
        user.setPrivateKeyIv("ivValue");
        return user;
    }

    @Test
    void register_withValidData_succeeds() {
        RegisterRequest req = createValidRegisterRequest();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(req.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtTokenProvider.generateToken(anyLong())).thenReturn("jwt-token");

        AuthResponse response = userService.register(req);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_withDuplicateEmail_throwsConflict() {
        RegisterRequest req = createValidRegisterRequest();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(createUserEntity()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.register(req));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void register_withDuplicateUsername_throwsConflict() {
        RegisterRequest req = createValidRegisterRequest();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(req.getUsername())).thenReturn(Optional.of(createUserEntity()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.register(req));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        UserEntity user = createUserEntity();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getId())).thenReturn("jwt-token");

        AuthResponse response = userService.login(req);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getEncryptedPrivateKey());
        assertNotNull(response.getPrivateKeyIv());
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("wrongpassword");

        UserEntity user = createUserEntity();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPasswordHash())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.login(req));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void login_withNonExistentEmail_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nonexistent@example.com");
        req.setPassword("password123");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.login(req));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void searchUsers_returnsMatchingUsers() {
        UserEntity user = createUserEntity();
        when(userRepository.findByEmailContainingIgnoreCase("alice")).thenReturn(List.of(user));

        List<UserEntity> results = userService.searchUsers("alice");

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).getUsername());
    }

    @Test
    void searchUsers_withExcludeUserId_returnsFilteredUsers() {
        UserEntity user1 = createUserEntity(); // id = 1L
        UserEntity user2 = new UserEntity();
        user2.setId(2L);
        user2.setUsername("alice2");
        user2.setEmail("alice2@example.com");

        when(userRepository.findByEmailContainingIgnoreCase("alice")).thenReturn(List.of(user1, user2));

        List<UserEntity> results = userService.searchUsers("alice", 1L);

        assertEquals(1, results.size());
        assertEquals("alice2", results.get(0).getUsername());
        assertEquals(2L, results.get(0).getId());
    }

    @Test
    void getPublicKey_returnsUserPublicKey() {
        UserEntity user = createUserEntity();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String publicKey = userService.getPublicKey(1L);

        assertEquals(user.getPublicKeyPem(), publicKey);
    }

    @Test
    void getUserById_whenNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }
}
