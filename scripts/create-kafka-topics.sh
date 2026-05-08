#!/usr/bin/env bash
set -e

KAFKA_BROKER="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"

create_topic() {
  local name=$1
  local partitions=${2:-3}
  local replication=${3:-1}
  kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" \
    --create --if-not-exists \
    --topic "$name" \
    --partitions "$partitions" \
    --replication-factor "$replication"
  echo "Topic '$name' ready."
}

create_topic "raw.ingest.af-ke-1"
create_topic "processed.ingest.af-ke-1"
create_topic "quota.events"
create_topic "alert.events"

echo "All Kafka topics created."
