CREATE TABLE key_materials (
    id                      BIGSERIAL PRIMARY KEY,
    conversation_id         BIGINT NOT NULL REFERENCES conversations(id),
    recipient_id            BIGINT NOT NULL REFERENCES users(id),
    sender_id               BIGINT NOT NULL REFERENCES users(id),
    encrypted_aes_key       TEXT NOT NULL,
    signature               TEXT NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    confirmed               BOOLEAN NOT NULL DEFAULT FALSE,
    issued_at               TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_km_pending ON key_materials(recipient_id, confirmed);
CREATE INDEX idx_km_conv ON key_materials(conversation_id, is_active);
