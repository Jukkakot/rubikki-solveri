# Proposal

## Why

The browser app crashes when the user returns to a tab that was in the background for minutes: the
phone's browser drops the page's WebGL context, and the drawing engine (Skiko 0.150.1, Compose
1.12.1) has no context-loss handling, so its next frame fails (`getShaderPrecisionFormat` returns
null; user's log 2026-10-04 21:02). No upstream fix was found.

## What Changes

- The page watches for the loss of its graphics context. When it happens, it notes it in the log
  and reloads itself silently as soon as it is visible again (at once if it is visible already).
- The reload returns to the same screen (the route is already in the URL fragment). What was going
  on inside the screen is lost: the solve step, a half-done scan. The user accepted this (2026-10-05).
- No notice is shown, and the reload does not count as a crash ("crashed last time" stays off).
- Decisions taken here (no `design.md`, small change):
  - Guard against a reload loop: at most one automatic reload per 30 s (kept in session storage);
    a second loss inside that window is only logged and the crash path handles it as before.
  - The listener lives in `platform.mjs` with the other browser calls. The engine's canvas sits in
    a shadow root and the event does not leave it, so `getContext` is wrapped and every canvas that
    gets a WebGL context is watched (found while implementing).

## Capabilities

### New Capabilities

### Modified Capabilities
- `web-app`: new requirement on recovering from a lost graphics context.

## Impact

Web module only (`web/src/wasmJsMain/resources/platform.mjs`, `Main.kt` / `WebServices.kt` for the
log line and the crash flag, `web/smoke/smoke.mjs`), plus one new log event in `shared`
(`app.graphicsLost`). `cube` and `app` are untouched.
