CREATE TABLE conversations (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE conversation_members (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT NOT NULL REFERENCES conversations(id),
    user_id             BIGINT NOT NULL REFERENCES users(id),
    joined_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_conv_member UNIQUE (conversation_id, user_id)
);
