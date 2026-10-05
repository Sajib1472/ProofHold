CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL
);

CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    open_from TIME NOT NULL,
    open_to TIME NOT NULL
);

CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES locations (id),
    status VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    found_at TIMESTAMPTZ NOT NULL,
    hold_until TIMESTAMPTZ NOT NULL,
    where_found VARCHAR(200) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE item_secrets (
    item_id BIGINT PRIMARY KEY REFERENCES items (id),
    photo_url TEXT,
    serial VARCHAR(80),
    unique_marks VARCHAR(200),
    full_description TEXT NOT NULL
);

CREATE TABLE challenges (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items (id),
    prompt VARCHAR(200) NOT NULL,
    expected_answer_hash VARCHAR(64) NOT NULL
);

CREATE TABLE claims (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items (id),
    claimer_id BIGINT NOT NULL REFERENCES users (id),
    status VARCHAR(32) NOT NULL,
    idempotency_key UUID NOT NULL,
    reason TEXT,
    answer_score INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    UNIQUE (item_id, idempotency_key)
);

CREATE TABLE handoffs (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items (id),
    claim_id BIGINT NOT NULL REFERENCES claims (id),
    slot_start TIMESTAMPTZ NOT NULL,
    slot_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key UUID,
    UNIQUE (item_id, idempotency_key)
);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items (id),
    actor_id BIGINT NOT NULL REFERENCES users (id),
    action VARCHAR(32) NOT NULL,
    at TIMESTAMPTZ NOT NULL,
    payload JSONB
);

CREATE INDEX idx_items_status ON items (status);
CREATE INDEX idx_items_location_id ON items (location_id);
CREATE INDEX idx_challenges_item_id ON challenges (item_id);
CREATE INDEX idx_claims_item_id ON claims (item_id);
CREATE INDEX idx_claims_claimer_id ON claims (claimer_id);
CREATE INDEX idx_handoffs_item_id ON handoffs (item_id);
CREATE INDEX idx_audit_events_item_id ON audit_events (item_id);
