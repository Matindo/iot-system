-- ============================================================
-- SENSOR DATA  (core hypertable)
-- ============================================================
CREATE TABLE tsdata.sensor_data (
    time        TIMESTAMPTZ      NOT NULL,
    project_id  UUID             NOT NULL,
    device_id   TEXT             NOT NULL,
    metric      TEXT             NOT NULL,
    value       DOUBLE PRECISION,
    value_str   TEXT,
    tags        JSONB,
    ingested_at TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

SELECT create_hypertable('tsdata.sensor_data', 'time', chunk_time_interval => INTERVAL '1 day');

CREATE INDEX idx_sensor_data_project_time
    ON tsdata.sensor_data (project_id, time DESC);

CREATE INDEX idx_sensor_data_device_time
    ON tsdata.sensor_data (project_id, device_id, metric, time DESC);

ALTER TABLE tsdata.sensor_data ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tsdata.sensor_data
    USING (project_id = current_setting('app.current_project_id')::UUID);


-- ============================================================
-- CONTINUOUS AGGREGATES
-- ============================================================

-- 1-minute averages
CREATE MATERIALIZED VIEW tsdata.sensor_data_1min
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) AS bucket,
    project_id,
    device_id,
    metric,
    AVG(value)  AS avg_value,
    MIN(value)  AS min_value,
    MAX(value)  AS max_value,
    COUNT(*)    AS sample_count
FROM tsdata.sensor_data
GROUP BY bucket, project_id, device_id, metric;

SELECT add_continuous_aggregate_policy('tsdata.sensor_data_1min',
    start_offset      => INTERVAL '2 days',
    end_offset        => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');

-- 1-hour averages
CREATE MATERIALIZED VIEW tsdata.sensor_data_1hr
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    project_id,
    device_id,
    metric,
    AVG(value)  AS avg_value,
    MIN(value)  AS min_value,
    MAX(value)  AS max_value,
    COUNT(*)    AS sample_count
FROM tsdata.sensor_data
GROUP BY bucket, project_id, device_id, metric;

SELECT add_continuous_aggregate_policy('tsdata.sensor_data_1hr',
    start_offset      => INTERVAL '8 days',
    end_offset        => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- 1-day averages
CREATE MATERIALIZED VIEW tsdata.sensor_data_1day
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS bucket,
    project_id,
    device_id,
    metric,
    AVG(value)  AS avg_value,
    MIN(value)  AS min_value,
    MAX(value)  AS max_value,
    COUNT(*)    AS sample_count
FROM tsdata.sensor_data
GROUP BY bucket, project_id, device_id, metric;

SELECT add_continuous_aggregate_policy('tsdata.sensor_data_1day',
    start_offset      => INTERVAL '31 days',
    end_offset        => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day');


-- ============================================================
-- RETENTION POLICY
-- ============================================================
SELECT add_retention_policy('tsdata.sensor_data', INTERVAL '90 days');


-- ============================================================
-- PROJECT QUOTA SNAPSHOTS
-- ============================================================
CREATE TABLE tsdata.project_quota_snapshots (
    time            TIMESTAMPTZ   NOT NULL,
    project_id      UUID          NOT NULL,
    messages_today  BIGINT        NOT NULL DEFAULT 0,
    storage_mb      NUMERIC(12,4) NOT NULL DEFAULT 0,
    active_devices  INTEGER       NOT NULL DEFAULT 0,
    api_calls_today BIGINT        NOT NULL DEFAULT 0
);

SELECT create_hypertable('tsdata.project_quota_snapshots', 'time',
    chunk_time_interval => INTERVAL '7 days');

CREATE INDEX idx_quota_snapshots_project
    ON tsdata.project_quota_snapshots (project_id, time DESC);
