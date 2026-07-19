# ESP32 Security Node Firmware

C++ firmware for an ESP32 development board. It reads an HC-SR501 PIR sensor,
queues security events, sends them to the Spring Boot API over Wi-Fi, exposes
status through USB/UART, and publishes state through a custom BLE GATT service.

## Tooling

Install the PlatformIO extension in VS Code or the PlatformIO CLI. The default
board target is the generic `esp32dev`; update `board` in `platformio.ini` after
identifying the exact board.

The development build uses PlatformIO's `huge_app.csv` partition because the
combined Wi-Fi and BLE firmware is larger than the generic board's default app
partition. This layout is suitable for initial USB flashing but does not reserve
an OTA update slot. A production partition layout must balance application size,
persistent queue storage, and firmware-update requirements.

## Configure

Copy the example configuration:

```bash
cp include/secrets.example.h include/secrets.h
```

Edit `include/secrets.h` with the Wi-Fi credentials and the LAN IPv4 address of
the computer running Spring Boot. Do not use `localhost`: on the ESP32,
`localhost` means the ESP32 itself. `secrets.h` is ignored by Git.

## Build, flash, and monitor

```bash
pio run -e esp32dev
pio run -e esp32dev --target upload
pio device monitor --baud 115200
```

## Native C++ tests

The event JSON encoder has a native test that does not require hardware. The
native PlatformIO environment requires a host C++ compiler such as GCC:

```bash
pio test -e native
```

The same test can be compiled for the ESP32 before hardware is connected:

```bash
pio test -e esp32dev --without-uploading
```

Running that test still requires a connected board. Compilation alone does not
constitute a passing hardware test.

## Initial PIR wiring

Do not wire the sensor until the exact ESP32 board and its pin labels have been
confirmed. The initial firmware assignment is:

```text
HC-SR501 OUT -> ESP32 GPIO 27
HC-SR501 GND -> ESP32 GND
HC-SR501 VCC -> board-appropriate supply confirmed from its documentation
```

Never assume a module output is safe for a 3.3 V ESP32 GPIO without checking its
specification.

## UART command protocol

USB serial uses 115200 baud and newline-terminated ASCII commands:

```text
PING     -> ACK PONG
STATUS   -> STATUS armed=<true|false> wifi=<state> queued=<count>
ARM      -> ACK ARM
DISARM   -> ACK DISARM
unknown  -> NACK unknown_command=<command>
```

Every queued security event is also emitted as one line prefixed with `EVENT `.
This gives a host-side serial gateway a stable framing boundary.

## BLE GATT contract

Service UUID: `5a5b1000-31d4-4c21-9f1b-df4a686fbd11`

| Characteristic | UUID suffix | Access | Value |
| --- | --- | --- | --- |
| Latest event | `1001` | Read/notify | Event JSON |
| Armed state | `1002` | Read/write | `true` or `false` |
| Device health | `1003` | Read/notify | `ONLINE` or `OFFLINE` |

BLE writes are unauthenticated in this development milestone. Do not deploy
control functionality outside a trusted lab until pairing, authorization, and
secret-management requirements are implemented.

## Delivery behavior

- Queue capacity is 16 events in RAM.
- HTTP `201 Created` confirms a new event.
- HTTP `409 Conflict` confirms a retry of an event already stored by the API.
- Failed deliveries use exponential backoff with jitter.
- The queue is not persistent yet; queued events are lost on reset or power loss.
