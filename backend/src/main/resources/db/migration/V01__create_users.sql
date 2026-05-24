CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    username                VARCHAR(50) NOT NULL UNIQUE,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    display_name            VARCHAR(100),
    public_key_pem          TEXT NOT NULL,
    encrypted_private_key   TEXT NOT NULL,
    private_key_iv          TEXT NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen               TIMESTAMP DEFAULT NOW()
);
