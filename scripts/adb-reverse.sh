#!/usr/bin/env bash
# Forwards the dev server port (default 8080) from every connected device or emulator to this machine, so debug builds
# built with -Prasoi.apiBaseUrl=http://localhost:8080 work everywhere. Re-run after reconnecting a device or restarting
# an emulator.
set -euo pipefail

port="${1:-8080}"
adb="${ADB:-adb}"
if ! command -v "$adb" >/dev/null 2>&1 && [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
  adb="$HOME/Library/Android/sdk/platform-tools/adb"
fi

serials=$("$adb" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
if [ -z "$serials" ]; then
  echo "No devices connected" >&2
  exit 1
fi

for serial in $serials; do
  "$adb" -s "$serial" reverse "tcp:$port" "tcp:$port" >/dev/null
  echo "$serial: localhost:$port -> this machine"
done
