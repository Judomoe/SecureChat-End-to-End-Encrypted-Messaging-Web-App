package com.securechat.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.securechat.exception.ResourceNotFoundException;
import com.securechat.security.JwtTokenProvider;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        UserEntity user = new UserEntity();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setPublicKeyPem(req.getPublicKeyPem());
        user.setEncryptedPrivateKey(req.getEncryptedPrivateKey());
        user.setPrivateKeyIv(req.getPrivateKeyIv());

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId());

        return AuthResponse.from(user, token);
    }

    public AuthResponse login(LoginRequest req) {
        UserEntity user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId());

        return AuthResponse.fromWithKeys(user, token);
    }

    public UserEntity getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<UserEntity> searchUsers(String email, Long excludeUserId) {
        return userRepository.findByEmailContainingIgnoreCase(email).stream()
            .filter(u -> !u.getId().equals(excludeUserId))
            .collect(java.util.stream.Collectors.toList());
    }

    public List<UserEntity> searchUsers(String email) {
        return userRepository.findByEmailContainingIgnoreCase(email);
    }

    public String getPublicKey(Long userId) {
        UserEntity user = getUserById(userId);
        return user.getPublicKeyPem();
    }
}
