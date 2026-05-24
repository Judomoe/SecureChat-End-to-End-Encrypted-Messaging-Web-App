CREATE TABLE contacts (
    id              BIGSERIAL PRIMARY KEY,
    requester_id    BIGINT NOT NULL REFERENCES users(id),
    recipient_id    BIGINT NOT NULL REFERENCES users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_contact_pair UNIQUE (requester_id, recipient_id),
    CONSTRAINT chk_no_self_contact CHECK (requester_id != recipient_id)
);
