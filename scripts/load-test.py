"""
Simulate N devices sending MQTT messages to the IoTeca broker.
Usage:
  pip install paho-mqtt faker
  python scripts/load-test.py --devices 50 --interval 2 \
      --project-id <uuid> --api-key <key>
"""
import argparse
import json
import random
import threading
import time

import paho.mqtt.client as mqtt
from faker import Faker

fake = Faker()


def device_loop(device_id: str, project_id: str, api_key: str,
                broker: str, port: int, interval: float):
    client = mqtt.Client(client_id=device_id, protocol=mqtt.MQTTv5)
    client.username_pw_set(username=project_id, password=api_key)
    client.connect(broker, port)
    client.loop_start()
    topic = f"IoTeca/{project_id}/{device_id}/environment"

    while True:
        payload = {
            "device_id": device_id,
            "timestamp": int(time.time() * 1000),
            "metrics": {
                "temperature": round(random.uniform(18.0, 35.0), 2),
                "humidity":    round(random.uniform(40.0, 90.0), 2),
                "battery_v":   round(random.uniform(3.2, 4.2), 2),
            },
            "tags": {"location": fake.city(), "firmware": "v1.0.0"},
        }
        client.publish(topic, json.dumps(payload), qos=1)
        time.sleep(interval)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--devices",    type=int,   default=10)
    parser.add_argument("--interval",   type=float, default=5.0)
    parser.add_argument("--project-id", required=True)
    parser.add_argument("--api-key",    required=True)
    parser.add_argument("--broker",     default="localhost")
    parser.add_argument("--port",       type=int,   default=1883)
    args = parser.parse_args()

    print(f"Starting {args.devices} simulated devices ...")
    threads = []
    for i in range(args.devices):
        device_id = f"sim_device_{i:04d}"
        t = threading.Thread(
            target=device_loop,
            args=(device_id, args.project_id, args.api_key,
                  args.broker, args.port, args.interval),
            daemon=True,
        )
        threads.append(t)
        t.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("Load test stopped.")


if __name__ == "__main__":
    main()
