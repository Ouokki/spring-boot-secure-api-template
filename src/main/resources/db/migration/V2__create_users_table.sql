CREATE TABLE app.users
(
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    email         VARCHAR(254) NOT NULL,
    password_hash TEXT        NOT NULL,
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- Case-insensitive unique constraint. Using a functional unique index because
-- a regular UNIQUE constraint is case-sensitive in Postgres; two users could
-- register with "User@Example.com" and "user@example.com" without this.
CREATE UNIQUE INDEX uk_users_email ON app.users (lower(email));
