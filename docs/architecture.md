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

## Screens — Implemented

| Route | Screen | Notes |
|---|---|---|
| `HomeRoute` | Home | entries: scan (coming), manual input, free cube |
| `ManualInputRoute` | Manual input | face-by-face painting with `CubeEditor`, check with `CubeCheck`; valid → solution |
| `FreeCubeRoute(cube?)` | Free cube | face-turn buttons, scramble, undo, reset, solve |
| `SolveRoute(cube)` | Solution | background solve, then a stepper: show / done / previous, moves in words |
| `SettingsRoute`, `LogRoute` | Settings, log | |

## Planned

- Camera scan (`camera-scan`), move guide
  (`move-guide`), camera follow-along (`camera-follow`), beginner solver (`beginner-solver`),
  lessons (`lessons`), timer and history (`progress`), signed APK (`release`).
