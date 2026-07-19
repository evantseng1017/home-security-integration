#!/usr/bin/env python3
"""Small plain-TCP receiver for local CoT development; not a TAK Server."""

import argparse
import socket
from pathlib import Path


def arguments():
    parser = argparse.ArgumentParser(description="Receive and display local Cursor-on-Target XML")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8087)
    parser.add_argument("--output", default="src/test/resources/output/mock-tak-events.xml.log")
    return parser.parse_args()


def main():
    args = arguments()
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    print(f"Mock TAK receiver listening on {args.host}:{args.port}")
    print(f"Appending received CoT to {output}")

    with socket.create_server((args.host, args.port)) as server:
        try:
            while True:
                connection, address = server.accept()
                with connection:
                    payload = bytearray()
                    while chunk := connection.recv(4096):
                        payload.extend(chunk)
                text = payload.decode("utf-8").strip()
                if text:
                    print(f"\nReceived from {address[0]}:{address[1]}\n{text}")
                    with output.open("a", encoding="utf-8") as log:
                        log.write(text + "\n")
        except KeyboardInterrupt:
            print("\nMock receiver stopped")


if __name__ == "__main__":
    main()
