# Roadmap

Planned changes in order. Each is an OpenSpec change (`/opsx:propose <name>`), specified ahead in
the spec phase and then implemented. Status: **done**, **specced**, **planned**.

| # | Change | Status | What |
|---|---|---|---|
| 0 | `app-setup` | done | Empty Compose app that runs on the phone: Material 3 theme (light/dark), Finnish/English, navigation shell, unit tests, CI build of a debug APK |
| 1 | `cube-model` | done | Cube state, moves and notation, whole-cube rotations, validity check (colour counts, pieces, twist, flip, parity); pure Kotlin with many tests |
| 2 | `cube-view` | done | 3D cube that animates moves and can be turned by dragging; manual input by painting stickers |
| 3 | `fast-solve` | done | Solver library (two-phase), solution stepper: next/back, replay, progress |
| 4 | `camera-scan` | done | Guided six-face scan with live grid, colour classification against the centres, tap to fix, validity feedback |
| 5 | `move-guide` | done | The clear move presentation: cube follows how you hold it, highlighted layer, arrows, haptics |
| 6 | `camera-follow` | done | Follow-along with the camera: arrow drawn on the real cube, move detected from colours, auto-advance |
| 7 | `beginner-solver` | done | Our own layer-by-layer solver that explains each stage |
| 8 | `lessons` | done | Teaching screens per stage and practice positions |
| 9 | `progress` | done | Timer, solve history and stats in a local database |
| 10 | `release` | done | Signed release APK installed directly on the phone (no Google Play) |
| 11 | `scan-confirm` | done | Scan holds a face 1.5 s with a progress bar, then shows the read colours to confirm or scan again (from phone testing) |
| 12 | `scan-calibrate` | done | Live reading learns the cube's own colours, tap a sticker in the review to fix it, exposure/white balance locked after the first face (from phone testing) |
