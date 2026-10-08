# Tasks

## 1. Fixture

- [ ] 1.1 Cut the 2026-10-08 web recording's fixture to its two scans (no home screen frames), add the first (plain striped cube) to the acceptance harness with its truth and the second as reported only; record in the fixture's KDoc which frames are which; verify the harness prints both scanners on it.

## 2. Red and orange, the stall, the hint

- [ ] 2.1 With `RulesTimeline`, find why orange stickers are named and shown red and the orange face is never recognised on the first scan; fix (names against the references, the display guard, the faces' assignment, whichever it is); a JVM test that no orange sticker is shown red as known on that fixture and that it finishes right; verify the test passes.
- [ ] 2.2 Show "Käännä kuutiota" only when the cube is otherwise nearly read (most stickers known) and two faces could still be either way; a test that a single face followed alone does not raise it; verify it passes.
- [ ] 2.3 Run the acceptance harness: never wrong on any fixture, the confirmed eleven within the bar; `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` passes.
