-- Initialize database schemas
CREATE SCHEMA IF NOT EXISTS platform;
CREATE SCHEMA IF NOT EXISTS tsdata;

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create application user with limited privileges
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'jhub-iot') THEN
    CREATE ROLE "jhub-iot" LOGIN PASSWORD 'changeme';
  END IF;
END
$$;

GRANT USAGE ON SCHEMA platform TO "jhub-iot";
GRANT USAGE ON SCHEMA tsdata   TO "jhub-iot";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA platform TO "jhub-iot";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA tsdata   TO "jhub-iot";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA platform TO "jhub-iot";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA tsdata   TO "jhub-iot";

ALTER DEFAULT PRIVILEGES IN SCHEMA platform GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "jhub-iot";
ALTER DEFAULT PRIVILEGES IN SCHEMA tsdata   GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "jhub-iot";
