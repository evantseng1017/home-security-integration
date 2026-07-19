# Hardware Integration Roadmap

This roadmap extends the existing Spring Boot event API into a portfolio project
that demonstrates embedded C++, physical-device communication, BLE, resilient
data exchange, and hardware-in-the-loop testing.

## Current baseline

Already implemented:

- Java 21 Spring Boot event ingestion API
- H2 persistence
- Request validation and clear HTTP errors
- Unit and HTTP integration tests
- Python ESP32 event simulator
- Compilable C++ ESP32 firmware for PIR, Wi-Fi, UART, BLE GATT, and RAM retry
  queue behavior
- Shared C++ event encoder and PlatformIO test image

The firmware portions of early milestones are implemented, but no hardware
milestone is complete until its acceptance criteria are verified on the
physical ESP32.

## Milestone 1: ESP32 firmware foundation

Create a `firmware/esp32-security-node` project using C++ and the Arduino ESP32
core or ESP-IDF. Establish repeatable build, flash, and serial-monitor commands.

Deliverables:

- Firmware source and configuration committed to the repository
- Stable device ID derived from configuration or device identity
- Structured serial logging for boot, errors, and state changes
- Documented board model, USB driver, COM port, and flashing procedure

Acceptance criteria:

- Firmware builds without warnings that indicate functional problems
- ESP32 can be flashed repeatedly
- Boot and device identity are visible over USB serial

## Milestone 2: PIR motion events over Wi-Fi

Connect the HC-SR501 PIR sensor to a safe ESP32 GPIO and send a
`MOTION_DETECTED` event to `POST /api/events` whenever motion is detected.

Deliverables:

- PIR wiring diagram and pin assignment
- Wi-Fi connection and reconnection logic
- JSON payload compatible with the existing backend
- Motion cooldown/debounce behavior to prevent event flooding
- NTP-synchronized UTC timestamps

Acceptance criteria:

- Walking in front of the sensor creates one valid backend event
- The event contains the expected device ID, location, coordinates, armed state,
  and timestamp
- Invalid or unavailable server responses are logged without crashing firmware

## Milestone 3: USB/UART interface

Define a small, versioned serial protocol between the ESP32 and a computer. Use
newline-delimited JSON initially, then document framing, maximum message size,
timeouts, acknowledgements, and error handling.

Deliverables:

- UART protocol specification
- C++ encoder/decoder on the ESP32
- Host-side serial gateway that forwards UART events to the Spring API
- Commands for health checks, arming, disarming, and device information

Acceptance criteria:

- Events can reach the backend with ESP32 Wi-Fi disabled
- Corrupt, partial, and unknown serial messages are rejected safely
- Disconnecting and reconnecting USB does not require restarting the backend

## Milestone 4: BLE GATT service

Expose device status and security events through a custom BLE GATT service.

Proposed characteristics:

- Device information: readable
- Armed state: readable and writable with authorization
- Latest event: readable and notifiable
- Device health: readable and notifiable

Deliverables:

- Documented service and characteristic UUIDs
- GATT server implementation on the ESP32
- Host-side discovery and subscription utility
- Connection, disconnection, and re-advertising behavior

Acceptance criteria:

- A host discovers the ESP32 by its advertised service UUID
- Subscribed clients receive motion notifications
- The device resumes advertising after a client disconnects

## Milestone 5: Offline queue and retry

Make event delivery resilient when Wi-Fi, BLE, USB, or the backend is
temporarily unavailable.

Deliverables:

- Bounded persistent queue using ESP32 nonvolatile storage or filesystem
- Exponential backoff with jitter
- Delivery acknowledgements and duplicate-safe retries using `eventId`
- Queue depth, dropped-event, and last-delivery diagnostics

Acceptance criteria:

- Events generated while the backend is offline are delivered after recovery
- Retried events do not create duplicate database records
- Queue limits and overflow behavior are documented and tested

## Milestone 6: Hardware-in-the-loop testing

Add repeatable tests that exercise actual firmware and connected hardware.

Test scenarios:

- Boot and serial readiness
- PIR idle and motion transitions
- Wi-Fi loss and recovery
- Backend loss and queued-event recovery
- USB disconnect and reconnect
- BLE discovery, notification, and reconnect
- Malformed UART commands
- Device restart with queued events

Deliverables:

- Test fixture documentation
- Automated host-side test runner where practical
- Captured logs and expected results
- Troubleshooting guide covering power, GPIO, USB drivers, networking, and time
  synchronization

Acceptance criteria:

- Tests can be repeated from documented setup instructions
- Results distinguish firmware, wiring, network, and backend failures

## Optional milestone 7: TAK/ATAK integration

Add an adapter that converts selected security events into Cursor-on-Target
(CoT) messages for a TAK-compatible environment.

Deliverables:

- Documented mapping from security event fields to CoT attributes
- CoT XML generation with unit tests
- Configurable destination and transport
- A safe local test procedure that does not require a production TAK system

Acceptance criteria:

- Generated messages conform to the chosen CoT schema and timestamp rules
- A test client or authorized TAK environment displays the expected event
- Backend event ingestion remains independent from TAK availability

## Engineering documentation to maintain

For every milestone, record:

- Requirements and assumptions
- Wiring and pin assignments
- Protocol or interface specification
- Build and flash commands
- Test evidence
- Known failure modes
- Security considerations
- Decisions and tradeoffs

## Security progression

Initial HTTP testing should remain on a trusted local network. Before using the
system outside a lab environment, add device authentication, secret management,
TLS, authorization for control commands, secure firmware-update planning, and
network exposure limits.
