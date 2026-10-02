# Architecture

Status markers: **Implemented** = on `main`; **Planned (`change`)** = agreed, delivered by that
roadmap change.

## Modules — Implemented

| Module | Kind | Holds |
|---|---|---|
| `cube` | Kotlin/JVM library, no Android imports | Cube state, moves, validity, colour classification, solvers |
| `app` | Android application (Compose) | Screens, navigation, settings, logging, camera, 3D view |

`app` depends on `cube`, never the other way. Anything that can be computed without a phone goes
into `cube`, so it is tested by plain JVM unit tests.

## Cube model (`cube`) — Implemented

- **Stickers:** 54, in the common URFDLB order (U1…U9, R1…R9, F, D, L, B; each face row by row in
  the standard net). This is also the two-phase solver's input order. `Stickers` gives each one a
  3D position (x right, y up, z front, −1..1) and normal.
- **Holding convention:** white on top, green in front (red right, orange left, blue back,
  yellow bottom) = `ColorScheme.STANDARD` and `Cube.solved()`.
- **`Cube`** is immutable: 54 colours; `apply(move)` returns a new cube. Colours are what the user
  sees; faces are derived through the centres (`toFaceletString()`), so any held orientation works.
- **`Move`** = `Layer` × quarter turns (1, 2, 3 = '). Layers: faces, slices (M E S), wide turns,
  rotations (x y z). Each move's 54-entry permutation is generated once from geometry (rotate the
  stickers in the turned layers −90° about the axis).
- **`Notation`** parses/prints; `Sequences` inverts and simplifies; `Scramble` makes random
  face-turn scrambles.
- **`CubeCheck.validity`** returns `Validity` (sealed) with the first reason: colour counts →
  centres → impossible piece → duplicate → twisted corner → flipped edge → swapped pieces. Sticker
  indices are included for highlighting. `CubeCheck.pieces` gives the corner/edge view
  (`Corner`, `Edge`, `Pieces`) with two-phase orientation conventions.

## Solver — Implemented

- `cs.min2phase` (in `cube/src/main/java`): the two-phase solver, vendored unmodified under its
  MIT licence option (`LICENSE` there names the upstream commit).
- `solve/TwoPhaseSolver`: checks validity, then `Search().solution(facelets, 21, 100 000 probes,
  1000 min probes)`: ≤ 21 moves, about 19 on average, ~25 ms on a desktop. `warmUp()` builds the
  tables (called from `RubikkiApp` on a background thread). `randomStateScramble()` for practice.

## Camera scan — Implemented

Pipeline, all but the first step pure Kotlin in `cube/scan`:

1. `CameraPreview` (app): CameraX `LifecycleCameraController` + `PreviewView`, analysis in
   RGBA_8888 at ~640×480, keep-only-latest. The controller aligns analysis with the visible preview,
   so the frame's crop rect is what the user sees.
2. `FrameSampler`: the grid is a centred square, 72 % of the visible area's shorter side, on
   screen and in the frame; each cell's middle 40 % is read (every 2nd pixel, per-channel median),
   mapped through the frame rotation.
3. `ScanSession`: live reading per cell against `ColorClassifier.DEFAULT_PALETTE` (dots, centre
   check); 6 identical frames with the right centre → capture the per-cell median; redo; capture
   button.
4. `ColorClassifier.classify`: CIE Lab (lightness weight 0.5), balanced assignment (Hungarian,
   every colour exactly nine times) seeded by the six centres, refined twice; confidence per
   sticker; below 0.12 is uncertain.
5. `ScanOutcome`: valid and no uncertain sticker → solution; otherwise manual input with
   `fromScan`, the scanned colours and the doubtful/problem stickers marked.

## App structure — Implemented

- `RubikkiApp` (Application): sets up the log and the crash handler, logs `app.start`.
- `MainActivity` (an `AppCompatActivity` so the per-app language works on Android 12): reads the
  theme setting, builds `AppActions` (everything the screens need from the app) and hosts the
  navigation graph.
- `ui/nav`: type-safe routes (`HomeRoute`, `SettingsRoute`, `LogRoute`) and `RubikkiNavHost`.
  Screens take plain values and callbacks, so tests drive them with fakes.
- `ui/theme`: `RubikkiTheme` — Material You dynamic colour, light/dark by the theme setting.
- `settings`: theme in DataStore Preferences; language through AppCompat per-app locales (stored
  by the system on Android 13+, by AppCompat on 12). Finnish is set on the first start.
- `log`: `Logger` (Logcat + capped file on a background thread), `LogLine` (line format),
  `Evt` (event catalogue), `CrashHandler` (writes `app.crash` synchronously and leaves a marker
  for the next start).

## 3D cube view — Implemented

- `ui/cube3d/CubeScene` (plain Kotlin, unit-tested): 26 cubies of size 1, each a dark body and its
  stickers as quads; a move in progress rotates the turned cubies about the move axis; the view is a
  quaternion; perspective projection, back-face culling, painter's order by cubie distance, hit
  test for taps.
- `Cube3D` draws the projected quads on a Compose `Canvas`; drag turns the view (`CubeViewState`),
  tap returns a sticker index, `marked` stickers get a red outline. Colours are fixed real-cube
  colours (`StickerColors`), not themed.
- `CubeAnimator` queues moves and animates them (300 ms quarter, 450 ms half, scaled by the phone's
  animator scale; instant when animations are off).
- Hold orientations per face (`FaceView` in `cube`) drive the manual input's preview and later
  the scanner's guidance.

## Move guide — Implemented

- `ui/guide/StepperState`: index over a move list, `done` / `back` / `demo` (play, pause 700 ms,
  snap back) and an automatic demo 500 ms after each new step (skipped when animations are off).
- `GuideCube`: `Cube3D` with the turning layer highlighted (other stickers mixed 60 % to grey), the
  direction arrow (`CubeScene.arrow`: an arc on the turning face, sweep = the move's angle, middle
  towards the camera) while the cube is still, and the view from `CubeScene.guideView(move)` —
  the hold never changes, only the camera: default for U/F/R, from the left for L, from behind for
  B, from below for D.
- `MoveWordsText`: the move in words, and its notation when Settings → Show move notation is on.
- Haptics: `Confirm` on done, `SegmentTick` at the end of each demo.

## Camera follow — Implemented

Camera mode of the solution screen (top-bar camera toggle), sharing `StepperState`:

- `cube/follow/FollowTracker`: for the current move m on state S, expects `front(S)` before and
  `front(S·m)` after; ≥ 8/9 cells and better than "before" for 3 frames → done (auto-advance with a
  vibration). A stable unique match to another face turn x → `WrongMove(x, fix = x⁻¹)`. Moves that
  leave the front unchanged (back turns) → `NotVisible`: confirm with the button. Centre not the
  front colour → `HoldFront`.
- `LiveCalibration`: references from the default palette, pulled 30 % towards readings of frames
  that clearly show a known front.
- `FrontArrow.of(move)`: the 2D arrow on the front face (rows left/right, columns up/down, front
  round, "2×" for half turns, none for B/S/rotations); `FollowPanel` draws it over the scan grid
  with the 3D guide cube in the corner.
- `CameraPermissionGate` is shared with the scan.

## Screens — Implemented

| Route | Screen | Notes |
|---|---|---|
| `HomeRoute` | Home | entries: scan, manual input, free cube |
| `ScanRoute` | Scan | camera permission, grid, live dots, auto-capture; result → solve or check |
| `ManualInputRoute(cube?, marked?, fromScan)` | Manual input / check a scan | face-by-face painting with `CubeEditor`, check with `CubeCheck`; valid → solution |
| `FreeCubeRoute(cube?)` | Free cube | face-turn buttons, scramble, undo, reset, solve |
| `SolveRoute(cube)` | Solution | background solve, then the move guide stepper; camera mode follows on the real cube |
| `SettingsRoute`, `LogRoute` | Settings, log | |

## Planned

- Beginner solver (`beginner-solver`), lessons (`lessons`),
  timer and history (`progress`), signed APK (`release`).
