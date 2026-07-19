# Home Security Event API

A Java 21 / Spring Boot service that validates, stores, and lists security events sent by devices such as an ESP32. Events are stored in an in-memory H2 database for this initial version.

## Hardware integration roadmap

The current backend and Python simulator are the software foundation. Planned
hardware work will evolve the project through these capabilities:

- ESP32 firmware written in C++
- PIR motion events transmitted over Wi-Fi
- USB/UART communication between the ESP32 and a computer
- A custom Bluetooth Low Energy (BLE) GATT service exposing security events
- Offline event storage with automatic retry after reconnection
- Hardware-in-the-loop tests and documented debugging procedures
- Optional Cursor-on-Target output for TAK/ATAK integration

See [Hardware Integration Roadmap](docs/HARDWARE_INTEGRATION_ROADMAP.md) for the
implementation order, deliverables, and completion criteria. Planned features
are not represented as complete until their corresponding acceptance criteria
have been verified with physical hardware.

## Prerequisites

- Java 21 or newer
- Maven 3.9+
- Python 3.10+ (only for the simulator)

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

## Run tests

```bash
mvn test
```

The suite contains a JUnit unit test for service behavior and Spring Boot integration tests that exercise the HTTP API against H2.

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
```
