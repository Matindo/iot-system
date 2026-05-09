#!/bin/bash
set -e

/etc/kafka/docker/run &
KAFKA_PID=$!

until kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; do
    echo "Waiting for Kafka broker..."
    sleep 3
done

/scripts/create-kafka-topics.sh

wait $KAFKA_PID
