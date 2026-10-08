# Tasks

## 1. Fixes

- [x] 1.1 Nothing held once complete: a test where the orange face is never shown (inferred) and the scan finishes with every sticker known; verify it passes.
- [x] 1.2 Warm centres clearly apart in hue never one face (hard rule); a JVM test with the logged camera colours (orange b53116, red 810412, a striped cube): finishes right, no orange shown red; verify it passes.
- [x] 1.3 The 180° mirror of the striped cube: find why the turn check lets it through on `202058` with a strong warm rule, fix it so it never finishes wrong.

## 2. Check

- [x] 2.1 Acceptance harness never wrong, the confirmed eleven within the bar; `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` passes.
