-- RaSaSeR Independent Social Connect -- DRAFT schema for a NEW isolated D1 database.
-- DO NOT run on the existing Premium/promo database. No secrets are stored in SQL.
-- All timestamps are UTC milliseconds since Unix epoch.
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS installations (
    id TEXT PRIMARY KEY,
    public_key_spki TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    revoked_at INTEGER
);

CREATE TABLE IF NOT EXISTS oauth_attempts (
    state_hash TEXT PRIMARY KEY,
    installation_id TEXT NOT NULL REFERENCES installations(id) ON DELETE CASCADE,
    provider TEXT NOT NULL CHECK(provider IN ('instagram','facebook','tiktok','youtube')),
    local_creator_id INTEGER NOT NULL,
    local_account_id INTEGER NOT NULL,
    pkce_verifier_ciphertext TEXT,
    pkce_verifier_iv TEXT,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    consumed_at INTEGER
);
CREATE INDEX IF NOT EXISTS idx_oauth_attempts_expiration
    ON oauth_attempts(expires_at);
CREATE INDEX IF NOT EXISTS idx_oauth_attempts_installation
    ON oauth_attempts(installation_id);

CREATE TABLE IF NOT EXISTS connected_channels (
    id TEXT PRIMARY KEY,
    installation_id TEXT NOT NULL REFERENCES installations(id) ON DELETE CASCADE,
    local_creator_id INTEGER NOT NULL,
    local_account_id INTEGER NOT NULL,
    provider TEXT NOT NULL CHECK(provider IN ('instagram','facebook','tiktok','youtube')),
    provider_channel_id TEXT NOT NULL,
    display_label TEXT NOT NULL,
    scopes_json TEXT NOT NULL DEFAULT '[]',
    state TEXT NOT NULL DEFAULT 'connected'
        CHECK(state IN ('connected','reauth_required','revoked','failed')),
    connected_at INTEGER NOT NULL,
    last_verified_at INTEGER NOT NULL,
    UNIQUE(installation_id, provider, provider_channel_id),
    UNIQUE(installation_id, local_account_id)
);
CREATE INDEX IF NOT EXISTS idx_channels_installation
    ON connected_channels(installation_id, local_creator_id);

-- Ciphertexts must be AEAD-encrypted with a Worker-held secret. Store no raw tokens.
-- IVs are independently randomized for each encryption and token refresh.
CREATE TABLE IF NOT EXISTS channel_token_vault (
    channel_id TEXT PRIMARY KEY REFERENCES connected_channels(id) ON DELETE CASCADE,
    key_version INTEGER NOT NULL,
    access_ciphertext TEXT NOT NULL,
    access_iv TEXT NOT NULL,
    refresh_ciphertext TEXT,
    refresh_iv TEXT,
    access_expires_at INTEGER,
    refresh_expires_at INTEGER,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS publish_jobs (
    id TEXT PRIMARY KEY,
    installation_id TEXT NOT NULL REFERENCES installations(id) ON DELETE CASCADE,
    channel_id TEXT NOT NULL REFERENCES connected_channels(id) ON DELETE CASCADE,
    idempotency_key TEXT NOT NULL,
    platform_post_id TEXT,
    status TEXT NOT NULL CHECK(status IN (
        'draft','queued','uploading','processing','published',
        'failed','reauth_required','manual_action_required'
    )),
    media_ref TEXT,
    content_sha256 TEXT,
    error_code TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    UNIQUE(installation_id,channel_id,idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_publish_jobs_status
    ON publish_jobs(status, updated_at);

CREATE TABLE IF NOT EXISTS incoming_platform_events (
    id TEXT PRIMARY KEY,
    provider TEXT NOT NULL,
    provider_event_id TEXT NOT NULL,
    channel_id TEXT REFERENCES connected_channels(id) ON DELETE CASCADE,
    received_at INTEGER NOT NULL,
    processed_at INTEGER,
    UNIQUE(provider,provider_event_id)
);
