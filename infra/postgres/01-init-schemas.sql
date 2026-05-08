-- Initialize database schemas
CREATE SCHEMA IF NOT EXISTS platform;
CREATE SCHEMA IF NOT EXISTS tsdata;

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create application user with limited privileges
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'afridata_app') THEN
    CREATE ROLE afridata_app LOGIN PASSWORD 'changeme';
  END IF;
END
$$;

GRANT USAGE ON SCHEMA platform TO afridata_app;
GRANT USAGE ON SCHEMA tsdata   TO afridata_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA platform TO afridata_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA tsdata   TO afridata_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA platform TO afridata_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA tsdata   TO afridata_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA platform GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO afridata_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA tsdata   GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO afridata_app;
