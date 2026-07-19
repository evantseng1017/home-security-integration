# Home Security Event API

A Java 21 / Spring Boot service that validates, stores, and lists security events sent by devices such as an ESP32. Events are stored in an in-memory H2 database for this initial version.

## Hardware integration roadmap

The Java backend remains the server-side foundation, and the repository now
also contains an initial C++ ESP32 firmware implementation. The firmware adds:

- ESP32 firmware written in C++ with PlatformIO
- PIR motion events transmitted over Wi-Fi
- USB/UART commands and newline-framed event output
- A custom Bluetooth Low Energy (BLE) GATT service exposing security events
- An in-memory offline queue with automatic retry after reconnection
- A native C++ test for the event JSON contract
- Planned hardware-in-the-loop tests and persistent offline storage
- Cursor-on-Target generation, TAK TCP/TLS delivery, and a database-backed retry outbox

See [Hardware Integration Roadmap](docs/HARDWARE_INTEGRATION_ROADMAP.md) for the
implementation order, deliverables, and completion criteria. Firmware behavior
has not yet been verified with physical hardware.

## Prerequisites

- Java 21 or newer
- Maven 3.9+
- Python 3.10+ (only for the simulator)
- PlatformIO (for C++ firmware builds, tests, and flashing)

## Run the service

```bash
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. The H2 console is available at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:securitydb`, user `sa`, and an empty password.

Because H2 is configured in memory, events are reset whenever the application stops.

## API

### Submit an event

`POST /api/events` returns `201 Created` with the stored event.

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  --output src/test/resources/output/post-event-response.json \
  -d '{
    "eventId": "evt-1001",
    "deviceId": "esp32-front-door",
    "eventType": "MOTION_DETECTED",
    "locationName": "Front Door",
    "latitude": 39.7392,
    "longitude": -104.9903,
    "armed": true,
    "timestamp": "2026-07-18T12:00:00Z"
  }'
```

Supported `eventType` values are `MOTION_DETECTED`, `ACCESS_GRANTED`, `ACCESS_DENIED`, `SYSTEM_ARMED`, `SYSTEM_DISARMED`, `PANIC_BUTTON`, `DEVICE_ONLINE`, and `DEVICE_OFFLINE`.

Latitude must be between -90 and 90 and longitude between -180 and 180. All fields are required. Event IDs must be unique. Timestamps must be ISO-8601 instants such as `2026-07-18T12:00:00Z`.

### List events

`GET /api/events` returns all stored events, newest timestamp first.

```bash
curl http://localhost:8080/api/events \
  --output src/test/resources/output/get-events-response.json
```

All documented `curl` commands save their response bodies in
`src/test/resources/output`. Inspect the saved files from the terminal with
`cat src/test/resources/output/get-events-response.json`, or open them directly
in the editor.

Errors use a consistent JSON response. Validation errors also include a `fieldErrors` object. Invalid input returns `400 Bad Request`; duplicate event IDs return `409 Conflict`.

### Generate Cursor-on-Target XML

After creating an event, render its TAK-compatible CoT representation:

```bash
curl http://localhost:8080/api/events/evt-1001/cot \
  --output src/test/resources/output/evt-1001-cot.xml
```

Missing event IDs return `404 Not Found`. CoT output includes identity,
coordinates, timestamps, device details, armed state, and a prototype event-type
mapping.

## Local TAK integration test

Start the included plain-TCP mock receiver from the repository root:

```bash
python simulator/mock_tak_receiver.py
```

In a second terminal, enable local TAK delivery:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--tak.enabled=true --tak.host=127.0.0.1 --tak.port=8087 --tak.tls=false"
```

Submit an event with the POST command above. The mock prints the CoT and appends
it to `src/test/resources/output/mock-tak-events.xml.log`. Plain TCP is only for
local development.

For a real TAK Server, enable TLS and configure the PKCS#12 client and trust
stores with `tak.key-store`, `tak.key-store-password`, `tak.trust-store`, and
`tak.trust-store-password`. Never commit certificates or passwords. Failed
messages remain in `tak_outbox` and retry with exponential backoff. Because H2
is currently in memory, the queue survives network outages but not an application
restart; use a file-backed or production database for restart durability.

## Run tests

```bash
mvn test
```

The suite contains service and CoT unit tests plus Spring Boot API, outbox retry,
and mock TCP receiver integration tests.

## ESP32 simulator

Start the service, then in another terminal run:

```bash
python simulator/esp32_simulator.py --count 10 --interval 0.5
```

The simulator uses only the Python standard library. Change the target with `--url`; run `python simulator/esp32_simulator.py --help` for all options.

## Project structure

```text
src/main/java/.../api       HTTP error response handling
src/main/java/.../event     controller, service, validation model, entity, repository
src/test/java/.../event     unit and HTTP integration tests
simulator/                  fake ESP32 event producer
src/main/java/.../tak       CoT conversion, TAK transport, and retry outbox
firmware/esp32-security-node C++ ESP32 firmware, native tests, and device docs
docs/                       hardware roadmap
```

## ESP32 C++ firmware

The firmware is a separate PlatformIO project so the Java backend continues to
use Maven unchanged:

```bash
cd firmware/esp32-security-node
pio test -e native
pio run -e esp32dev
```

Before flashing real hardware, identify the exact board and follow the firmware
[configuration and safety instructions](firmware/esp32-security-node/README.md).
