#!/usr/bin/env python3
"""Send realistic fake ESP32 security events to the local API."""

import argparse
import json
import random
import time
import uuid
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

EVENT_TYPES = (
    "MOTION_DETECTED",
    "ACCESS_GRANTED",
    "ACCESS_DENIED",
    "SYSTEM_ARMED",
    "SYSTEM_DISARMED",
    "PANIC_BUTTON",
    "DEVICE_ONLINE",
    "DEVICE_OFFLINE",
)

DEVICES = (
    ("esp32-front-door", "Front Door", 39.7392, -104.9903),
    ("esp32-garage", "Garage", 39.7393, -104.9908),
    ("esp32-back-yard", "Back Yard", 39.7389, -104.9901),
)


def build_event() -> dict:
    device_id, location, latitude, longitude = random.choice(DEVICES)
    event_type = random.choice(EVENT_TYPES)
    return {
        "eventId": str(uuid.uuid4()),
        "deviceId": device_id,
        "eventType": event_type,
        "locationName": location,
        "latitude": latitude,
        "longitude": longitude,
        "armed": event_type != "SYSTEM_DISARMED",
        "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    }


def submit(url: str, event: dict) -> None:
    request = Request(
        url,
        data=json.dumps(event).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=10) as response:
            print(f"{response.status} {event['eventType']}: {response.read().decode('utf-8')}")
    except HTTPError as error:
        print(f"HTTP {error.code}: {error.read().decode('utf-8')}")
    except URLError as error:
        raise SystemExit(f"Could not reach {url}: {error.reason}") from error


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default="http://localhost:8080/api/events")
    parser.add_argument("--count", type=int, default=5, help="number of events to send")
    parser.add_argument("--interval", type=float, default=1.0, help="seconds between events")
    args = parser.parse_args()

    if args.count < 1 or args.interval < 0:
        parser.error("--count must be positive and --interval cannot be negative")

    for index in range(args.count):
        submit(args.url, build_event())
        if index + 1 < args.count:
            time.sleep(args.interval)


if __name__ == "__main__":
    main()
