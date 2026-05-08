-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE platform.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    full_name       TEXT,
    phone           TEXT,
    country         TEXT NOT NULL DEFAULT 'KE',
    role            TEXT NOT NULL DEFAULT 'USER',
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON platform.users (email);


-- ============================================================
-- SUBSCRIPTION TIERS
-- ============================================================
CREATE TABLE platform.subscription_tiers (
    id                       SERIAL PRIMARY KEY,
    name                     TEXT NOT NULL UNIQUE,
    max_messages_per_day     BIGINT NOT NULL,
    max_devices              INTEGER NOT NULL,
    max_projects             INTEGER NOT NULL,
    data_retention_days      INTEGER NOT NULL,
    max_message_rate_per_sec INTEGER NOT NULL,
    storage_limit_mb         BIGINT NOT NULL,
    api_calls_per_minute     INTEGER NOT NULL,
    price_kes_monthly        NUMERIC(10,2) NOT NULL DEFAULT 0,
    features                 JSONB,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- USER SUBSCRIPTIONS
-- ============================================================
CREATE TABLE platform.user_subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES platform.users(id) ON DELETE CASCADE,
    tier_id     INTEGER NOT NULL REFERENCES platform.subscription_tiers(id),
    status      TEXT NOT NULL DEFAULT 'ACTIVE',
    started_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    payment_ref TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user ON platform.user_subscriptions (user_id);


-- ============================================================
-- PROJECTS
-- ============================================================
CREATE TABLE platform.projects (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID NOT NULL REFERENCES platform.users(id) ON DELETE CASCADE,
    name                     TEXT NOT NULL,
    description              TEXT,
    environment              TEXT NOT NULL DEFAULT 'production',
    region                   TEXT NOT NULL DEFAULT 'af-ke-1',
    timezone                 TEXT NOT NULL DEFAULT 'Africa/Nairobi',
    expected_device_count    INTEGER NOT NULL DEFAULT 1,
    declared_send_interval_s INTEGER,
    data_format              TEXT NOT NULL DEFAULT 'json',
    retention_override_days  INTEGER,
    alert_webhook_url        TEXT,
    alert_email              TEXT,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_project_name_per_user UNIQUE (user_id, name)
);

CREATE INDEX idx_projects_user ON platform.projects (user_id);


-- ============================================================
-- API KEYS
-- ============================================================
CREATE TABLE platform.api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES platform.projects(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    key_prefix  TEXT NOT NULL,
    key_hash    TEXT NOT NULL UNIQUE,
    environment TEXT NOT NULL DEFAULT 'live',
    scopes      TEXT[] NOT NULL DEFAULT '{ingest}',
    last_used_at TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ,
    revoke_reason TEXT
);

CREATE INDEX idx_api_keys_project ON platform.api_keys (project_id);
CREATE INDEX idx_api_keys_hash    ON platform.api_keys (key_hash);


-- ============================================================
-- DEVICES
-- ============================================================
CREATE TABLE platform.devices (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       UUID NOT NULL REFERENCES platform.projects(id) ON DELETE CASCADE,
    device_id        TEXT NOT NULL,
    name             TEXT,
    firmware_version TEXT,
    hardware_type    TEXT,
    location_label   TEXT,
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    tags             JSONB,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    first_seen_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at     TIMESTAMPTZ,
    last_ip          INET,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_device_per_project UNIQUE (project_id, device_id)
);

CREATE INDEX idx_devices_project   ON platform.devices (project_id);
CREATE INDEX idx_devices_last_seen ON platform.devices (last_seen_at);


-- ============================================================
-- ALERT RULES
-- ============================================================
CREATE TABLE platform.alert_rules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id           UUID NOT NULL REFERENCES platform.projects(id) ON DELETE CASCADE,
    name                 TEXT NOT NULL,
    description          TEXT,
    metric_name          TEXT NOT NULL,
    device_id            TEXT,
    condition            TEXT NOT NULL,
    threshold            DOUBLE PRECISION,
    absence_window_s     INTEGER,
    notification_channels TEXT[] NOT NULL,
    suppression_window_s INTEGER DEFAULT 300,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    last_fired_at        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_rules_project ON platform.alert_rules (project_id);


-- ============================================================
-- NOTIFICATIONS LOG
-- ============================================================
CREATE TABLE platform.notification_log (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES platform.users(id),
    project_id UUID REFERENCES platform.projects(id),
    type       TEXT NOT NULL,
    channel    TEXT NOT NULL,
    subject    TEXT,
    body       TEXT,
    sent_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered  BOOLEAN NOT NULL DEFAULT FALSE,
    error      TEXT
);

CREATE INDEX idx_notifications_user ON platform.notification_log (user_id, sent_at DESC);


-- ============================================================
-- ADMIN AUDIT LOG
-- ============================================================
CREATE TABLE platform.admin_audit_log (
    id            BIGSERIAL PRIMARY KEY,
    admin_user_id UUID NOT NULL REFERENCES platform.users(id),
    action        TEXT NOT NULL,
    target_type   TEXT NOT NULL,
    target_id     TEXT NOT NULL,
    details       JSONB,
    performed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address    INET
);
