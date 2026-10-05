CREATE TABLE claim_answers (
    claim_id BIGINT NOT NULL REFERENCES claims (id),
    challenge_id BIGINT NOT NULL REFERENCES challenges (id),
    value TEXT NOT NULL,
    PRIMARY KEY (claim_id, challenge_id)
);
