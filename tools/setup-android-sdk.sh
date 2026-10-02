#!/usr/bin/env bash
# Installs the Android SDK pieces this project builds with into $ANDROID_HOME (default
# /opt/android-sdk) and writes local.properties. Safe to run again: it only installs what is missing.
# Used by Claude's cloud sessions (SessionStart hook); on Windows, Android Studio does this instead.
set -euo pipefail

SDK="${ANDROID_HOME:-/opt/android-sdk}"
TOOLS_ZIP="commandlinetools-linux-16111833_latest.zip"
PACKAGES=("platforms;android-37.0" "platforms;android-37.1" "build-tools;36.0.0" "build-tools;37.0.0" "platform-tools")
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Installing Android command-line tools into $SDK"
  mkdir -p "$SDK/cmdline-tools"
  tmp="$(mktemp -d)"
  curl -sSfL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$TOOLS_ZIP"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

missing=()
for pkg in "${PACKAGES[@]}"; do
  [ -d "$SDK/${pkg//;//}" ] || missing+=("$pkg")
done
if [ ${#missing[@]} -gt 0 ]; then
  echo "Installing ${missing[*]}"
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
  "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --install "${missing[@]}" >/dev/null
fi

[ -f "$ROOT/local.properties" ] || echo "sdk.dir=$SDK" > "$ROOT/local.properties"
echo "Android SDK ready in $SDK"
