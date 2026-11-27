BEGIN;

-- Keep recovery data outside the public Data API. Run as the Spring JDBC owner.
CREATE SCHEMA ascend_recovery;
REVOKE ALL ON SCHEMA ascend_recovery FROM PUBLIC;

CREATE TABLE ascend_recovery.tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at = created_at + interval '15 minutes'),
    used_at timestamptz,
    revoked_at timestamptz,
    CHECK (used_at IS NULL OR revoked_at IS NULL)
);
CREATE UNIQUE INDEX recovery_one_active ON ascend_recovery.tokens(user_id)
    WHERE used_at IS NULL AND revoked_at IS NULL;
CREATE INDEX recovery_token_expiry ON ascend_recovery.tokens(expires_at);

-- Durable jobs contain no email address, password or bearer token.
CREATE TABLE ascend_recovery.deliveries (
    user_id bigint PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
    requested_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    next_attempt timestamptz NOT NULL DEFAULT clock_timestamp(),
    attempts integer NOT NULL DEFAULT 0,
    delivery_id uuid
);
CREATE INDEX recovery_next_delivery ON ascend_recovery.deliveries(next_attempt);

CREATE TABLE ascend_recovery.limits (
    bucket varchar(64) PRIMARY KEY,
    until_at timestamptz NOT NULL,
    attempts integer NOT NULL CHECK (attempts > 0)
);
CREATE INDEX recovery_limit_expiry ON ascend_recovery.limits(until_at);
COMMIT;
