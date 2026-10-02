#!/usr/bin/env bash
# Cloud sessions start from a fresh container: install the Android SDK so Gradle can build at once.
if [ "${CLAUDE_CODE_REMOTE:-}" = "true" ]; then
  "$CLAUDE_PROJECT_DIR/tools/setup-android-sdk.sh" >&2 || echo "Android SDK setup failed; run tools/setup-android-sdk.sh" >&2
  command -v openspec >/dev/null || npm install -g @fission-ai/openspec >/dev/null 2>&1 || true
fi
exit 0
