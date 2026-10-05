# Roadmap

Planned changes in order. Each is an OpenSpec change (`/opsx:propose <name>`), specified ahead in
the spec phase and then implemented. Status: **done**, **specced**, **planned**. Ideas not yet scheduled are in the backlog at the end.

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
| 13 | `scan-flow` | done | Scan guides but never blocks: centre is a hint, only "previous face still in view" stops; raw camera colours in the review, colours decided at the end; tap-to-fix removed (from phone testing) |
| 14 | `scan-layout` | done | Scan fits one screen: actions always visible at the bottom, status and review texts on the camera view (from phone testing) |
| 15 | `scan-cube-check` | done | No auto-capture without a cube in the grid; a small picture of every capture, shared with the log (from phone testing) |
| 16 | `scan-grid-check` | done | The cube check looks for the dark gaps between stickers instead of colours; the first successful phone scan is a regression test |
| 17 | `scan-polish` | done | Exposure locked at the first capture (fixes the overexposed scan), no main-thread work at capture and stall logging, the check page fits one screen and shows the camera picture |
| 18 | `screen-gallery` | done | Every screen in light and dark from the screenshot tests, published as a private gallery page and refreshed after UI changes |
| 19 | `gallery-numbers` | done | Gallery screens numbered with Finnish names and a "→" line of where each leads |
| 20 | `gallery-map` | done | Navigation map at the top of the gallery, drawn from the screen table; the solve screen's sub-states in one box |
| 21 | `scan-check-faces` | done | Colour check goes face by face: "looks right" per face, fix a sticker or rescan just that face; says plainly when the cube cannot be right and which faces to suspect (gallery feedback 8) |
| 21b | `scan-any-order` | done | Faces scanned in any order and rotation: the face from its centre (confirmed in the review), rotations found by search assuming the real cube is valid; no suggested order; inserted before 22 (decided 2026-10-03) |
| 22 | `about-log-polish` | done | About shows only the version and the one-line description; log lines coloured by level, times in the phone's local time and format (gallery feedback 3, 4) |
| 22b | `turn-steps` | done | Solver turns animated in steps: a quarter turn as one clear step, a half turn as two quarter steps with a pause, so it is obvious how far to turn (user priority 2026-10-03: more important than polish) |
| 23 | `look-refresh` | done | Karkki look (shapes, Fredoka/Nunito) on Material You colours; scan and solve screens restyled after mockup A; scan screens always dark |
| 24 | `home-design` | done | Home screen with a spinning 3D cube, scan as the one primary action, a 2×2 tile grid and a best-time line (gallery feedback 1) |
| 25 | `lesson-visuals` | done | Lessons as swipe pages with pictures: grey goal cube per stage, case pictures instead of "how" text, algorithm pages that show what moves, goal card in practice and guided solve |
| 26 | `phone-install` | done | Every green push publishes a signed APK at one fixed address (signed with the Android Studio key so it updates the installed app and keeps its data); About → "Lataa uusin versio" |
| 27 | `web-app` | done | The same app in the browser (GitHub Pages, installable, offline) from one Kotlin Multiplatform code base: cube multiplatform with min2phase ported to Kotlin, screens in a shared Compose Multiplatform module, Android app unchanged |
| 28 | `steady-cube` | done | Shortest-solution guide never turns the view: holding view, a small mirror cube, words for that view, reset button after dragging (user priority 2026-10-04; learn method unchanged) |
| 29 | `share-log-fix`, `share-log-zip` | done | Browser log sharing: at most ten files to the share sheet, zip download when refused (Samsung Internet) |
| 30 | `scan-needs-cube` | done | Auto capture only when every grid cell is one sticker; green cell outlines; camera follow ignores frames without a face |
| 31 | `fit-screen` | done | Screens with actions fit in portrait without scrolling: trimmed spacing, the big element shrinks (200 dp floor) |
| 32 | `real-mirror` | done | A framed mirror behind the guide cube in the 3D scene, a true reflection of the back; replaces the mirror card; one steady view and one move wording everywhere, learn method included (user decisions 2026-10-04) |
| 33 | `scan-dim-light` | done | Forgiving scan in dim, warm light: bigger averaged cell reading, brightness-independent face naming, the six centres named together at the end (silent fix, next-best namings), the colour check after every scan (user, 2026-10-04) |
| 34 | `web-context-lost` | done | Browser app reloads itself silently on the same screen when the phone takes its graphics away in a background tab, instead of crashing on return |
| 35 | `step-settle` | done | Guide demo stays after the move, "Tein sen" nods instead of replaying the turn, back unchanged; no arrow in the mirror, a hidden face's arrow goes around the outside of its layer; solved cube hops, spins and bursts confetti (phone testing 2026-10-05) |
| 36 | `scan-quick-flow` | done | Each captured face is accepted by itself after 2 s (a touch stops it); a sure scan skips the colour check and opens the solution, with the check behind it; the guide shows the arrow 1.5 s before the demo (phone testing 2026-10-05) |
| 37 | `guide-fixes` | done | Arrow shown the whole time a move is presented (also during and after the demo), 0.5 s pause before the demo, no nod (vibration stays), show always plays: a tap during a turn no longer stopped the animator for good (phone testing 2026-10-05) |
| 38 | `arrow-count-loop` | done | A "×1"/"×2" badge in the middle of the guide arrow; the demo repeats 3 s after it ends until the user moves on (phone testing 2026-10-05) |
| 39 | `video-scan-spike` | done | Offline face finder (pure Kotlin) measured on the user's test videos: both assemble the true cube, ~90 % of faces read exactly, two faces in a quarter of angled frames; proposal "go" (findings in the archive) |
| 40 | `video-scan` | done | Scan by turning the cube in front of the camera: a grey 3D progress cube in the camera corner fills sticker by sticker, a turning-hint arrow on it; offered beside the guided scan (default) until it proves reliable on the phone |

## Backlog

Ideas kept for later, not ordered (moved here 2026-10-03: the look and the home screen matter more).

- camera follow: notice when the cube is held differently, or keep helping in any orientation
  (after the user has tried camera follow; from `steady-cube`).

- `web-solver-worker`: run the solver in a Web Worker (a second instance of the wasm module) if the
  phone's browser is slow: warm-up over 4 s or a solve over 2 s measured with `?selftest`
  (web-app design, Risks).

- `solve-challenge`: learning mode on the solve screen: show the position to reach next, the user tries it themselves, a "hint" button reveals the moves step by step; the target shown as the turnable 3D cube
