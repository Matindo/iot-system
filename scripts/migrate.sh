#!/usr/bin/env bash
set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-afridata}"
DB_USER="${DB_USER:-afridata_app}"
PSQL="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"

echo "Running migrations against $DB_HOST:$DB_PORT/$DB_NAME ..."

$PSQL -f infra/postgres/01-init-schemas.sql
$PSQL -f infra/postgres/02-platform-tables.sql
$PSQL -f infra/postgres/03-tsdata-tables.sql
$PSQL -f scripts/seed-tiers.sql

echo "Migrations complete."
