# Tasks

## 1. Fixture

- [x] 1.1 Cut the 2026-10-08 web recording's fixture to its two scans (no home screen frames), add the first (plain striped cube) to the acceptance harness with its truth and the second as reported only; record in the fixture's KDoc which frames are which; verify the harness prints both scanners on it.

## 2. Red and orange, the stall, the hint

- [x] 2.1 Orange shown as red and the orange face never recognised. Found: the fixture still held the home screen's painted cube (stills f0021–f0049), whose red calibrated red before the camera started; cut to the camera (66–455, second scan 499–998), the recording stalls just as the phone did (F and B wrong, orange face never recognised): the orange centre, looking red, was taken for the red face and became its own red reference. Done: (a) until both the red and the orange centre are known, no red or orange sticker and nothing on the red and orange faces is shown as known; (b) red and orange told apart by each other: a warm centre clearly more orange by hue than another track's is not the red face, one clearly redder not the orange face (`FaceTracks.warmOrderCost`); a piece touching a face an open track could be is not shown known; (c) the snapshots carry each track's face and centre colour (`centres`). JVM test with the camera's orange 70 % towards red: no orange sticker shown red, finishes right, the log carries it; the shared single-face tests now expect red and orange held. Tried and dropped: red/orange readings merged until calibrated (made the striped sides symmetric, wrong cube) and red/orange looks made equal (wrong cube on 132049).
- [x] 2.2 Show "Käännä kuutiota" only when the cube is otherwise nearly read (most stickers known) and two faces could still be either way; a test that a single face followed alone does not raise it; verify it passes. ("Nearly read" = four faces settled, `VideoScan.HINT_SEEN`.)
- [x] 2.3 Run the acceptance harness: never wrong on any fixture, the confirmed eleven within the bar; `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` passes.
