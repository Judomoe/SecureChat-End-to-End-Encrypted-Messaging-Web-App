package com.securechat.user;

public class AuthResponse {

    private String token;
    private UserDto user;
    private String encryptedPrivateKey;
    private String privateKeyIv;

    public AuthResponse() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public void setEncryptedPrivateKey(String encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public String getPrivateKeyIv() {
        return privateKeyIv;
    }

    public void setPrivateKeyIv(String privateKeyIv) {
        this.privateKeyIv = privateKeyIv;
    }

    public static AuthResponse from(UserEntity entity, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(UserDto.from(entity));
        return response;
    }

    public static AuthResponse fromWithKeys(UserEntity entity, String token) {
        AuthResponse response = from(entity, token);
        response.setEncryptedPrivateKey(entity.getEncryptedPrivateKey());
        response.setPrivateKeyIv(entity.getPrivateKeyIv());
        return response;
    }

    public static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String displayName;
        private String createdAt;

        public UserDto() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public static UserDto from(UserEntity entity) {
            UserDto dto = new UserDto();
            dto.setId(entity.getId());
            dto.setUsername(entity.getUsername());
            dto.setEmail(entity.getEmail());
            dto.setDisplayName(entity.getDisplayName());
            dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
            return dto;
        }
    }
}
