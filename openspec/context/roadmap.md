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
| 41 | `video-scan-live` | done | Video scan feels live: faces with a finger over a sticker count, a dot in the read colour on every sticker on the camera picture, the progress cube follows the real cube smoothly and shows unconfirmed stickers faintly |
| 42 | `video-scan-progress` | done | Video scan finishes on the most likely possible cube (unseen stickers follow from the rest, a misread one is corrected), progress marked on the real cube in the picture with a turn arrow beside it, a restart panel with the reason when it cannot get on, log snapshots; progress cube removed (phone testing 2026-10-05) |
| 43 | `video-scan-light` | done | Video scan reads colours in dim, warm light: readings between two colours stay uncertain (soft votes) so the cube decides, glare left out, washed-out readings count little; both dim test videos now finish; the stall panel became a notice at the bottom of the picture that never blocks scanning, with a torch button; torch re-meters the camera; ticks only when confirmed; the browser shows the camera's own sharp video (phone testing 2026-10-05) |
| 44 | `camera-exposure` | done | Video scan camera measures light and focuses on the cube and goes darker while stickers wash out (torch in a dark room), then locks; about 15 pictures a second, in the browser read in a Web Worker off the page's thread; log tells fps and what the camera can do (user's log 2026-10-05, Galaxy S24) |
| 45 | `video-primary` | done | Video scan is the default: the home screen's scan button opens it (video icon, no "(kokeilu)" tile), "Kuva kerrallaan" switches to the guided scan and "Videolla" back (user, 2026-10-06) |
| 46 | `solve-to-target` | done | Choose where the cube ends: pattern gallery (11 patterns), surprise, "up to a stage", or paint one's own; target row on the solution screen, "Kuviot" tile on home; the shortest way straight from the cube to the target (user, 2026-10-06) |
| 47 | `handsfree-guide` | done | Shortest-solution guide without aiming at a button: tap the cube = done; handsfree mode after "Valmis" advances by itself at a chosen speed (bar, vibration before the next move), any touch stops it (user, 2026-10-06) |
| 48 | `ui-polish` | done | Home: the turning cube is the scan button, five icons in a row, no tagline or best-time line; a start screen before the guide (moves, target, method, hold picture, start / handsfree with pace); the guide as a media player (⏮ ▶/⏸ ⏭, ↻, ⋮ menu); back after a scan starts a new scan; handsfree time counts from when the move appears (user, 2026-10-06) |
| 49 | `scan-paint` | done | Video scan paints the real cube: a tile per sticker in its colour, grey while needed, white outline around a confirmed side; the paint glides with the cube and stays through short gaps; every camera picture read; no turn arrow or side-ball row, a small ring instead; full-screen camera with back, torch and a ⋮ menu, one status line (user, 2026-10-06) |
| 50 | `quiet-screens` | done | The symbols-over-words rule on the remaining screens: guided scan with the video scan's overlay (no title, no "n/6", hint only until the first face), colour check's instruction only until the first action, ‹ › ✓ icons in manual input, free cube layer buttons as small cube pictures with ↻/↺, timer instruction only before the first solve |
| 51 | `pattern-scan-first` | done | A pattern from home asks "Skannaa kuutio" or "Kuutio on jo ratkaistu"; the scan carries the target to the solution; a painted target fits the cube however it is held (user, 2026-10-07) |
| 52 | `scan-start` | done | Video scan reads from the first picture with a face (no wait for the camera); metering on the middle of the cube, locked within about a second; a small spinner until the first sticker (user, 2026-10-07) |
| 53 | `scan-centre-clash` | done | Video scan: two faces in one picture never take the same centre colour (a dark blue centre in shadow no longer spoils the white face), a face's oldest readings age out so a face read wrong at first is put right (web scan, 2026-10-07) |
| 54 | `scan-centre-naming` | done | Video scan: faces piled by how they look on this cube in this light and named together (a blue face first is no longer taken for white, an orange face for red); a face that could be either of two colours waits; stickers read mostly regardless of brightness (yellow in dim light no longer green) (web test, 2026-10-07) |
| 55 | `scan-paint-calm` | done | Video scan paint: only the stickers still needed are veiled in grey, known ones left bare (no read colours), a finished side gets an outline and a tick, the marks hide while the cube moves quickly (user, 2026-10-07) |
| 56 | `scan-steady-progress` | done | Video scan: a face is kept out of a pile whose neighbours it puts on the wrong side (the red face of a striped cube no longer joins the orange face's pile), progress no longer goes back on a rename; known stickers get a small dot in their colour again and the marks stay while the cube is held (web test, 2026-10-07) |
| 57 | `corner-scan-spike` | done | Spike: faces named and turned from corner views (handedness decides red against orange); right on clean video, never enough alone (no opposite corners in free scanning); as rules inside the scan they finish two runs today's scan never finishes, no wrong cube added. Go for `scan-rules` (findings in the archived change) |
| 58 | `scan-rules` | done | Rules scanner beside the earlier one: faces followed from picture to picture and known by the rules of a real cube (neighbours, sides, corner handedness, pieces), never finishes wrong on the fixtures, 9/11 with look-alike centres (earlier 1/11); Settings offers both, the new one by default; "Käännä kuutiota" while two faces could be either way. Phone test pending |
| 59 | `scan-rules-phone` | done | First phone test stalled (camera's orange read red, taken for the red face): red and orange told apart by hue against each other, held until both centres are known; hint only once four faces are settled; log carries the centres. Phone test pending. |
| 60 | `scan-rules-phone-2` | done | Second phone test: a finished scan lacked red/orange (held after complete), one stuck (orange centre taken for red). Held colours released at the finish, warm hue order a hard rule, the turn check catches the striped cube's half-turn mirror. Phone test pending |
| 61 | `scan-rules-finish` | done | Third phone test: the rules scan knew the striped cube in 20 s but never finished (each new face in view revoked the finish), and slowed from 14 to 5 fps as face tracks piled up. A new face that fits no longer holds the finish back; old tracks leave the per-frame work; today's recording a fixture; then speed-up ideas |
| 62 | `scan-speed-up` | done | Speed-ups worth their effort: the face finder twice as fast with the same faces (23.5 → 11.5 ms a frame on JVM), trial turns from per-face votes (about 1 ms less a frame), scan and paint times in the log. Phone check (browser, 2026-10-08): finder 32 → 27 ms, 15 → 17 fps, no drop over a scan; scan time grows with the centres list (4 → 19 ms in 15 s), watch it in long scans |
| 63 | `scan-feedback` | done | The scan shows its work: a face read steadily shows hollow rings in its read colours at once (dots once known), a six-colour ring of sides read/confirmed, a small 3D cube showing the turn after 2 s without progress, a buzz on a new side; in the browser the picture shown is the one read, so the marks sit on a moving cube. Phone check (browser, 16:40): scans finish right in 10–16 s; flicker left → `scan-paint-steady` |
| 64 | `scan-paint-steady` | done | Browser phone test 16:40: grey ghost veils on the table (a guessed mirror tilt), double veils on open faces, marks blinking out for a few pictures, copy 20–27 ms. Projection only from a sure tilt, no projected side under a found face, the read picture held over a faceless reading (300 ms), copy capped at 720 px. Browser check 17:15: looks good; copy still 15–22 ms → next |
| 65 | `scan-read-picture-android` | done | The phone app shows the read picture with its marks too (analysis 640×480 upright as a bitmap, drawn with the marks; marks snap, picture held over a faceless reading); the browser's copy is one `drawImage` into a pool of three canvases (desktop 6 → 1.3 ms). Phone check: copy 1.5–2.5 ms, app 17–21 fps |
| 66 | `scan-paint-found-only` | done | Recording 18:00: guessed sides floated in the air and beside the cube. Marks only on faces found in the picture; their thin outline only once read steadily. Phone check pending |
| 67 | `scan-speed-up-2` | done | Finder pictures at long side 240 on both (same faces on the recordings); debug build not debuggable (phone: finder 34–59 → 10–16 ms, 16–23 → 30 pictures/s); the phone's scan on a background thread, the browser's in the finder worker (state as text). Search window and two finders dropped after measuring |
| 68 | `scan-speed-up-3` | done | The scan logic no longer grows over a long scan: late pictures 8–11 → 1–2.5 ms (JVM), a picture without faces almost free; same results on every fixture (work skipped when nothing changed, pair tables and best-cube costs kept). Budget test 5 ms a 100-picture window. Phone/browser check of `scanMs` pending |
| 69 | `scan-track-settle` | done | Rules scan: a track never goes back to the face and turn it just left without a new reading of its own (flips without readings 493 → 0 on the fixtures, same finishes); the shown stickers stop switching back and forth in long scans. Phone check: marks calm while the cube is held |
| 70 | `scan-speed-up-4` | done | Browser: finding faces and the scan logic side by side in two workers (desktop Chromium 29–36 → 33–51 answers a second); the best cube's margins only as far as the scan looks (same decisions, little time saved; log `margin` at most 2.5). Phone check: fps with two faces in view near 1000 / `finderMs` |
| 71 | `scan-rules-only` | done | Browser phone test 2026-10-09: rules scan finished right in 4–7 s, the earlier scanner stalled once → earlier scanner and its Settings choice removed; grey veils only on faces followed from an earlier picture (no ghost tiles beside a fast-turned cube) |
| 72 | `scan-recording` | specced | Every video scan records what the scan was given (faces and colours per picture, with times), the newest three kept and shared with the log, replayed exactly in tests; a setting hides the scan marks for clean screen recordings (user 2026-10-09: a failed scan must become test material) |

## Backlog

Ideas kept for later, not ordered (moved here 2026-10-03: the look and the home screen matter more).

- video scan: straight-on readings weigh more (user, 2026-10-07). Tried in `scan-centre-clash`
  and dropped: no gain on the test videos, and a face misread straight on beat earlier right angled
  readings (design "Findings" in that archive). Worth another look only with a case it would fix.

- video scan: show a sticker still in doubt as "x or y" (e.g. half red, half orange) instead of
  only veiled or one leading colour, refined as surer readings come (user, 2026-10-07). The
  `scan-rules` scanner already keeps every possible colour internally; this is only how it is
  shown. Look again after the phone test of `scan-rules`, if the user then still misses it.

- `cloud-setup` (all projects): **global part done 2026-10-09**: public repo
  `Jukkakot/claude-config` (CLAUDE.md, hooks, skills `ui` and `quickshare`, `install.sh`; setup
  script line in its README), installed locally the same way. First cloud session (2026-10-09):
  `~/.claude/CLAUDE.md`, hooks, skills and a Quick Share download all work. Done.
  Test material done in `cloud-testdata` (all stills in git, videos local, `tools/make-stills.sh`).

- camera follow: notice when the cube is held differently, or keep helping in any orientation
  (after the user has tried camera follow; from `steady-cube`). Proposal `camera-follow-any-way`
  drafted 2026-10-06 (commit 9a0a469) and parked: hard to film the cube while turning it.

- ~~`web-solver-worker`~~ dropped 2026-10-06: `?selftest` on the user's phone (Samsung Internet)
  gave warmup 126–141 ms and solve 148–196 ms, far under the 4 s / 2 s limits.

- More ways to pick a target (after `solve-to-target`, 2026-10-06): type or paste a move sequence,
  scan the target from another cube, a partial target ("only this side matters").

- `solve-challenge`: learning mode on the solve screen: show the position to reach next, the user tries it themselves, a "hint" button reveals the moves step by step; the target shown as the turnable 3D cube
