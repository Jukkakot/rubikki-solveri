# Proposal

## Why

The second phone test (web, 1.0.299, 2026-10-08 07:42–07:44 UTC; log and screen recording in
`testdata/video/2026-10-08b/`) gave four rules scans: two right (11 s, 8 s), one stuck (28 s), and
one finished with the red and orange sides missing (invalid cube). The look scanner was right in
13.5 s. The user keeps the rules scanner as the default and asks for the fixes.

- **Finished with sides missing:** `scan-rules-phone` holds red and orange stickers until both
  centres are known, and it kept holding them after the scan was complete (the orange face inferred,
  never settled), so the finished cube lacked them. A regression of that change.
- **Stuck:** the log's `centres` show the camera's orange centre (b53116) and red (810412) both taken
  for the red face. The hue order is only a cost (8) the misread stickers outweigh; at 12 or more
  `202058` finished WRONG (every side turned 180°, the striped cube's mirror), which the turn check
  lets through.

## What Changes

- Nothing is held once the scan is complete.
- Two warm centres clearly apart in hue are never the same face (a hard rule, not a cost).
- The striped cube's 180° mirror is not taken as clear while views could still tell the turns.
- Tests from the camera's logged centre colours; the acceptance harness stays never wrong.
- The rules scanner stays the default (user, 2026-10-08).

No spec changes: the `video-scan` spec already asks for this.

## Impact

`cube/scan/FaceTracks.kt`, `VideoScan.rulesFrame`; tests in `cube/src/jvmTest`.

## Decisions (made while implementing)

- Held red and orange are released once the scan is complete.
- The hue order is a rule (cost 1000, 6° step). Harness: never wrong; `web_181940` 331 → 202,
  `202156` 258 → 159; the robustness run of `web_181940` (red centres pushed towards orange) no
  longer finishes (never, not wrong).
- The turn check also tries three or more faces turned half round together (the striped cube's
  mirror).
- The test with the camera's logged centre colours passes, but it passed before the fix too: the
  synthetic views do not reproduce the stuck scan (not on video; only its log). The next phone
  test tells.
