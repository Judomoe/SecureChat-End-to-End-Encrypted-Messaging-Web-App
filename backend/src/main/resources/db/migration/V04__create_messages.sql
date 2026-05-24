CREATE TABLE messages (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT NOT NULL REFERENCES conversations(id),
    sender_id           BIGINT NOT NULL REFERENCES users(id),
    ciphertext          TEXT NOT NULL,
    iv                  TEXT NOT NULL,
    hmac                TEXT NOT NULL,
    signature           TEXT NOT NULL,
    timestamp           BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conv_created ON messages(conversation_id, created_at DESC);
