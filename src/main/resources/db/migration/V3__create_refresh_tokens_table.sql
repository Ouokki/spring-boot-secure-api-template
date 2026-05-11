CREATE TABLE app.refresh_tokens
(
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    -- Raw token is never stored. SHA-256 hash stored for constant-time lookup.
    token_hash  TEXT        NOT NULL,
    family_id   UUID        NOT NULL,
    -- replaced_by links the chain: old token → new token (for audit trail)
    replaced_by UUID,
    revoked_at  TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES app.users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON app.refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON app.refresh_tokens (family_id);
