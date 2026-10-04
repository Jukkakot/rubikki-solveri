# Proposal

## Why

In Samsung Internet on the phone the log screen's share button does nothing: the browser says it
can share files, then rejects the share, and `shareOrDownload` (`web/.../platform.mjs`) swallows
the error. The user cannot send the log for debugging.

## What Changes

- If sharing the log with the pictures fails (anything but the user cancelling), the log alone is
  shared; if that fails too, the log is downloaded as a text file. The button always does something.
- The outcome (`shared`, `log-only`, `downloaded`, `cancelled`) and the browser's error name and
  message are logged as `log.shared`, so the next shared log shows why Samsung refused.

Decision: no visible message for the fallback; the browser's own download notice is the feedback.

Modules: `web` only (platform.mjs, Share.kt, Js.kt, WebApp.kt). No Android change.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `web-app`: sharing the log falls back to the log alone, then to a download, when the browser
  refuses the share.

## Impact

`web/src/wasmJsMain/resources/platform.mjs`, `web/.../Share.kt`, `Js.kt`, `WebApp.kt`.
