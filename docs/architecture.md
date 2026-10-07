# Architecture

Status markers: **Implemented** = on `main`; **Planned (`change`)** = agreed, delivered by that
roadmap change.

## Modules — Implemented

| Module | Kind | Holds |
|---|---|---|
| `cube` | Kotlin Multiplatform library (JVM + browser/Wasm), no Android imports | Cube state, moves, validity, colour classification, solvers |
| `shared` | Kotlin Multiplatform library (Android + browser/Wasm), Compose Multiplatform | Every screen, navigation, theme, 3D view; texts, fonts, icons and licence texts as Compose resources; the platform seams (see Platforms) |
| `app` | Android application | `MainActivity`, Room, DataStore settings, per-app language, file log, crash handler, share intent |
| `web` | Kotlin/Wasm browser application | Browser shell: `platform.mjs` (all JavaScript), camera, storage, service worker |
| `webworker` | Kotlin/Wasm worker (`scan-worker.js`) | The video scan's `FaceFinder` off the page's thread; depends only on `cube`, copied into `web`'s distribution |

`app` → `shared` → `cube`, never the other way. Anything that can be computed without a phone goes
into `cube`, so it is tested by plain JVM unit tests. Packages did not change when code moved to
`shared` (`fi.jukkakot.rubikkisolveri.ui.*`, `.progress`, `.log`, `.settings`); the Android tests
stay in `app/src/test` and test the shared screens on Robolectric.

## Platforms — Implemented

Common code calls `expect` declarations; each platform supplies the `actual` (Android in
`shared/src/androidMain`, browser in `shared/src/wasmJsMain`):

| Seam | Where | Android | Browser |
|---|---|---|---|
| `CameraPreview`, `CameraPermissionGate` | `ui/scan/Camera.kt` | CameraX, runtime permission | planned (`web-app`) |
| `platformColorScheme` | `ui/PlatformSeams.kt` | Material You | none → Karkki scheme |
| `LightStatusBarIcons` | same | window insets controller | no-op |
| `animationScale` | same | `ANIMATOR_DURATION_SCALE` | reduced-motion query |
| `elapsedMillis`, `argbToImageBitmap`, `LocalFormats`, `currentLanguage` | same | `SystemClock`, `Bitmap`, `java.time`/`DateFormat` | browser APIs |

Services that differ per platform come in through `AppActions` (`progress`, `scanPictures`,
`readLog`/`shareLog`, `platform`, …): `MainActivity` builds the Android set. `AppLog` is common;
`AppLog.init(context)` (app) installs the phone's `Logger` over a `LogFile`.

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

- `solve/min2phase` (in `cube/src/commonMain`): the two-phase solver min2phase, ported to Kotlin
  under its MIT licence option (`LICENSE` there names the upstream commit) so it also runs in the
  browser. The Java original stays in `cube/src/jvmTest/java` as the oracle: `Min2phasePortTest`
  checks 1 000 random cubes give identical solutions.
- `solve/TwoPhaseSolver`: checks validity, then `Search().solution(facelets, 21, 100 000 probes,
  1000 min probes)`: ≤ 21 moves, about 19 on average, ~25 ms on a desktop. `warmUp()` builds the
  tables (called from `RubikkiApp` on a background thread). `randomStateScramble()` for practice.
- Solving to a target (`solve-to-target`): `TwoPhaseSolver.solve(from, to)` searches the cube
  X = to⁻¹·from (`min2phase/Relative`, cubie multiply), whose solution made on `from` ends in `to`;
  as short as a normal solve. Centres must match (`CubePattern.cube(like)` builds patterns on the
  start's centres). `cube/SolveTarget` (solved, pattern, stage, painted; `encode()` for routes),
  `cube/Patterns` (`CubePattern`: the move sequences from solved, look-checked in `SolveTargetTest`).
  Stage targets cut the learn plan after that stage (`planTarget` in `ui/solve/SolveScreen`).

## Camera scan — Implemented

Pipeline, all but the first step pure Kotlin in `cube/scan`:

1. `CameraPreview` (app): CameraX `LifecycleCameraController` + `PreviewView`, analysis in
   RGBA_8888 at ~640×480, keep-only-latest. The controller aligns analysis with the visible preview,
   so the frame's crop rect is what the user sees.
2. `FrameSampler`: the grid is a centred square, 72 % of the visible area's shorter side, on
   screen and in the frame; each cell's middle 60 % is read (every 2nd pixel, per-channel mean of the 25th–75th percentile),
   mapped through the frame rotation.
3. `ScanSession`: faces in any order, each turned any way. The centre provisionally names the face among
   the faces not yet scanned (`ColorClassifier.rankedCentre`: brightness scaled out, against the
   default palette and the accepted centres); the full scan shows no face name, the names are
   decided in `outcome()` (step 5). Readings are stored as seen. Steady = every cell within ΔE 18
   (`STEADY_DISTANCE`) for 1.5 s and 3 frames → capture the per-cell median, then review (raw
   colours, "Good, next" / "Scan again"; no tap-to-fix). Stops: an accepted face in view in any of
   its four rotations (`AlreadyScanned`), and a grid that is not a cube face (`NoCube`, decided by
   `FrameSampler.check` on the grid picture: at least 8 of 9 cells' middles must be one even cube colour, so one shadowed cell is allowed
   (coloured, or light and nearly grey for white; thresholds in `FrameSampler`, set from the user's
   pictures of 2026-10-04) and enough cells need dark gaps around them; no picture yet = not a cube).
   Each grid cell that looks like a sticker gets a green outline. The capture button ignores the check. Redo takes back the face accepted last; capture button. Live dots show the raw camera colour.
   Exposure/white balance lock at the first capture (`index > 0 || review != null`); capture
   pictures are written on `Dispatchers.IO`; `scan.stall` logs camera gaps ≥ 300 ms and UI frames
   ≥ 150 ms apart.
4. `ColorClassifier.classify`: CIE Lab (lightness weight 0.5), balanced assignment (Hungarian,
   every colour exactly nine times) seeded by the six centres, refined twice; confidence per
   sticker; below 0.12 is uncertain.
5. `outcome()` first names the six centres together (`ColorClassifier.centreNamings`, the best
   of the 720 namings by brightness-scaled distance to the default palette) and renames faces
   accordingly; if that gives no solvable cube, the next namings (up to `MAX_NAMINGS`) are tried
   with rotations only. Then, per naming, `RotationSearch`: the 54 classified colours as seen; the 4⁶ = 4096 face
   rotations are tried (`CubeCheck.realPieceCount`, then `validity` for those with 20 real pieces).
   One distinct valid cube → it; several → the fewest quarter turns, the faces that differ marked
   uncertain; none → the most real pieces, after trying each single opposite pair renamed (a
   mislabelled pair mirrors the cube; any single rename undoes a mirror, so the pair whose readings
   fit the default palette better swapped is taken). ~0.1 s, 0.35 s worst case on a desktop JVM.
   The outcome carries `rotations` and `from` (which capture ended on which face); readings and
   uncertain stickers are turned into net order, and the app turns each picture the same way
   (`rotatePicture`).
6. `ScanOutcome`: always the check (manual input with `fromScan`). Valid and no uncertain sticker
   (`confident`): nothing marked, and the check opens the solution by itself after 5 s
   (`autoContinue`; any touch stops it; "scan the whole cube again" beside "Looks right").
   Otherwise the doubtful/problem stickers are marked and nothing continues by itself. The outcome keeps the
   54 raw readings (`samples`); the app holds them, the pictures and a just-rescanned face in
   `LastScan` (snapshot state, in memory only).
7. The check (`ScanCheck`, pure Kotlin): faces without a mark start checked; "Looks right" goes to
   the next unchecked face; after the last one `verdict()` → `Solvable`, or `Impossible` with the
   faces to look at again. `MisreadSearch.swaps` tries every swap of two non-centre stickers of
   different colours (1128 validity checks) and ranks the ones that make the cube valid by how
   much worse the readings fit; the best swap's two faces are named and its stickers marked. No
   swap → the faces of the stickers the validity names, else the two faces whose readings fit
   worst. A face rescanned on its own (`ScanSession(only = …)`) is classified by
   `ColorClassifier.classifyFace` against the other 45 readings labelled by the current colours,
   after scaling the rescan by the centre's brightness ratio (the new exposure), in each of its
   four rotations: the one giving a solvable cube, else the most real pieces, else the best fit;
   the check's picture of that face is turned to match.

### Two ways to scan

The video scan (`VideoScanRoute`) is the default since `video-primary` (2026-10-06): the home
screen's primary button and the check's "scan again" open it. A finished scan opens the solution
(sure) or the check (unsure) on top of the scan (`afterScan`), so going back always starts a new
scan; the scan's check stays reachable from the solution's menu (`LastScan.check`). Its menu's "Kuva kerrallaan"
replaces it with the guided scan above (`ScanRoute`), whose "Videolla" switches back; a one-face
rescan from the check stays guided.
Video scan pipeline (`video-scan`):

1. `CameraPreview(onImage)`: every picture the camera delivers (`scan-paint`: no limit of the
   app's own; the finder's one-picture buffer drops what it cannot take; in the browser the worker
   gets the newest picture whenever it is idle, the page-thread fallback stays at 66 ms), the visible picture
   upright, short side ≤ 360 px (`FrameSampler.upright` on Android; the browser passes its 360-px
   copy as is). In the browser the picture the user sees is the camera's own `<video>`, placed
   under the app's canvas where the preview box is; the box draws itself transparent
   (`web/WebCamera.kt`, `cameraShow` in `platform.mjs`), so the marks are drawn on top of the
   full-resolution video.
   Camera control (`camera-exposure`): `cube/scan/ExposureControl` decides per frame what the
   camera does (`CameraSettings`: metering point, focus point, steps darker, lock): meter at the
   middle of all faces found, step darker while washed out, then lock exposure and white balance (at
   the latest about a second after the first face, `scan-start`); washed out after the lock or a
   torch change meters again. It only steers the camera: every frame is read meanwhile (`scan-start`:
   holding frames back cost ~5 s at the start). Thresholds and timings are in the class. Android: `FocusMeteringAction` (point converted to the
   sensor frame by `FrameSampler.toFrameShare`), `setExposureCompensationIndex`, the Camera2 AE/AWB
   lock. Browser: `pointsOfInterest`, `focusMode`, `exposureCompensation` (each only if the browser
   lists it; the point converted through the cover crop), the `exposureMode` lock. Camera steps per
   controller step: `ExposureSteps`. When the camera opens a `scan.camera` line says what it can do.
2. `cube/scan/FaceFinder`: full 3×3 lattices (and partial ones with 7–8 stickers) anywhere in the picture (spike `video-scan-spike`,
   findings in its archive); run on `Dispatchers.Default` on Android. In the browser it runs in a
   Web Worker (`webworker` module, `scan-worker.js`): the page makes an `ImageBitmap` of the visible
   part at 360 px (`createImageBitmap`, or canvas A's pixels where that fails), transfers it, the
   worker reads its pixels in an `OffscreenCanvas` and sends the faces back as numbers
   (`FaceCodec`); one picture at a time, only the newest waits. If the worker cannot start or
   throws, the page reads on its own thread as before (`scan.worker` line with the reason;
   snapshots carry `worker=true/false`). `web/smoke/video.mjs` runs the video scan in Chromium
   with a fake camera (`--no-worker` blocks the worker).
3. `cube/scan/VideoScan`: votes per sticker (partial faces vote, never anchor), pose, orientation
   (`Orientation`, weak perspective from one face's steps), stall reasons and `reset()`. The
   projection is held over frames without a settled face for up to `HOLD_MILLIS`, moved (not
   turned) onto the largest face found, with its age in `projectionAge` (`scan-paint`). The votes are evidence for `BestCube` (`video-scan-progress`): the possible cube that
   fits them best, piece by piece, with a margin per piece place. Each reading gives every colour a
   share by its Lab distance to the cube's own centres (`ColorClassifier.shares`, soft votes;
   washed-out readings count little; `video-scan-light`, whose findings explain why no light-colour
   correction is used); a sticker's colour leaves out a lamp's glare (`FaceFinder`). A sticker is known when its
   place's margin clears `CLEAR_MARGIN` (chosen by `ScanSimulation`, findings in the `video-scan-light` archive) or by
   its votes alone, and the scan finishes when the whole cube is clear, unseen stickers included.
   Face rotations come from the same cost. `CubeProjection` puts every sticker into the picture.
   Earlier decisions in the `video-scan` and `video-scan-live` archives. Regression data: the test
   videos' finder output in `cube/src/jvmTest/resources/video/` (`VideoScanTest`, the true cubes in
   `VideoFixtures`; regenerate with `VideoScanHarness.writeFixtures` from the committed JPEG stills
   in `testdata/video/<date>/stills/`; the videos themselves stay local only).
4. `ui/scan/VideoScanScreen`: the camera fills the screen with the progress painted on the real
   cube (`ScanPaint`: a tile per sticker of the found faces and of the projection's sides facing
   the camera, solid when known, grey and dashed while needed, a white outline around a side the
   best cube confirms; `Glide` moves the tiles at the display's rate, the projection's paint fades
   with its age), back / ring of stickers known / torch / ⋮ menu (one picture at a time, by hand,
   "Korjaa värit") on the picture, one status line at the bottom; no turn arrow, no done-sides row
   (`scan-paint`). A notice at the bottom of the picture per stall reason (scanning goes on; tap outside closes it; restart, fix
   colours, torch); log lines from `VideoScanLog`. The result goes through `afterScan` like the guided scan's
   (stickers known only from the rest of the cube are marked in the check; no face pictures).

## Beginner solver — Implemented

`cube/beginner`: a layer-by-layer method for people, in seven stages (`Stage`): white cross, white
corners (white on top, the app's hold), then the cube is turned over (z2) and: middle layer,
yellow cross, yellow edges, yellow corners into place, yellow corners turned.

- Cross: per edge, IDA* over face turns tracking only the white stickers of the target and the
  placed edges, bounded by a per-sticker distance table.
- Other stages: `MacroSearch` — breadth-first over macros (whole-cube turn y^r, setup turn of
  U/D, a classic algorithm repeated n times) to the next sub-goal without breaking earlier stages.
  Algorithms: trigger R' D' R D (white corners and the last stage), U R U' R' U' F' U F and its
  mirror, F R U R' U' F', R U R' U R U2 R' U, U R U' L' U R' U' L.
- Each `Step` has a `StepNote` (data); the app words it (`ui/common/StepTexts`). About 160 moves,
  < 60 ms on a desktop.
- The solution screen offers "shortest" or "learn step by step" (`SolveMethod`); learning shows a
  stage card (stage n/7 with a goal thumbnail that opens large, the step's note). When a stage
  begins, a goal card ("Next: …", `GoalCard` in `SolveScreen`) covers the guide until Continue. Whole-cube turns
  are worded by the resulting front and top centres, and the hold line updates as the cube turns.

## Lessons — Implemented

- `ui/lessons/LessonCatalog`: basics + one lesson per beginner stage, each a list of
  `LessonPage`s (goal, cases, one per algorithm, practice; basics: four picture pages). Texts are
  one-liners; pictures carry the lesson. `LessonScreens`: `HorizontalPager` with dots and
  back/next; every page fits the screen without scrolling.
- Pictures are all the app's 3D cube via `GoalCube` (grey = not in place, red outline = marked).
  Data lives in the cube module: `beginner/StageGoals` (per-stage hold and in-place/added
  stickers), `beginner/StageCases` (case positions built backwards from the goal, so a test proves
  each case's moves reach what the caption promises), `Sequences.movedStickers` (before/after
  outlines on algorithm pages).
- `AlgorithmPage`: 3D demo from the goal hold with the algorithm's inverse applied, the current
  move highlighted in the notation and said in words; it snaps back after 1.2 s.
- Practice: `Practice.exercise(stage, random)` (cube) scrambles, beginner-solves, applies the
  earlier stages; `PracticeRoute(stage, seed)` opens `SolveScreen` in practice mode with only that
  stage's steps; "new position" replaces the route with seed + 1.

## Progress — Implemented

- Room database `progress.db` (`ProgressDatabase` in `app`, KSP; schema in `app/schemas`; the
  `*Entity` rows map to the common `TimedSolve`/`GuidedSolve`/`PracticeSession`): `timed_solve`,
  `guided_solve`, `practice`. `ProgressRepository` (Room implementation; `InMemoryProgressRepository`
  for tests and previews) is created in `MainActivity` and passed through `AppActions`.
- `TimerState` (hold 500 ms → ready → release starts → tap stops) and `SolveStats` (best, aoN
  with competition DNF rules, mean) are plain Kotlin.
- Timer screen: random-state scramble (`TwoPhaseSolver.randomStateScramble`), guided scramble
  through the solution screen (`ScrambleGuideRoute`), +2/DNF/delete, stats. History lists timed
  and guided solves and practice. The solution screen reports `onFinished`; the nav host records
  guided solves and practice; the lesson list shows practice counts.

## App structure — Implemented

- `RubikkiApp` (Application): sets up the log and the crash handler, logs `app.start`.
- `MainActivity` (an `AppCompatActivity` so the per-app language works on Android 12): reads the
  theme setting, builds `AppActions` (everything the screens need from the app) and hosts the
  navigation graph.
- `ui/nav`: type-safe routes (`HomeRoute`, `SettingsRoute`, `LogRoute`) and `RubikkiNavHost`.
  Screens take plain values and callbacks, so tests drive them with fakes.
- `ui/theme`: `RubikkiTheme` — Material You dynamic colour (where the platform has it), light/dark
  by the theme setting, Karkki typography (`fredoka()`/`nunito()` are composable, as Compose
  resources load fonts) and shapes; `ForcedDark` wraps the scan routes. `ui/common/Buttons.kt`: shared
  `RoundIconButton`, `BigButton`, `BackButton`. `ui/common/FitColumn.kt`: every screen with actions
  (solution, free cube, scan, timer, lesson pages) fits without scrolling; the child marked
  `fitSlot()` gets the height left, and the column scrolls only below the 200 dp floor.
- `settings`: theme in DataStore Preferences; language through AppCompat per-app locales (stored
  by the system on Android 13+, by AppCompat on 12). Finnish is set on the first start.
- `log`: `Logger` over a `LogStore` (common; on the phone Logcat + capped `LogFile` on a background
  thread), `LogLine` (line format), `Evt` (event catalogue), `ScanPictureStore`, `CrashHandler` (writes `app.crash` synchronously and leaves a marker
  for the next start).

## 3D cube view — Implemented

- `ui/cube3d/CubeScene` (plain Kotlin, unit-tested): 26 cubies of size 1, each a dark body and its
  stickers as quads; a move in progress rotates the turned cubies about the move axis; the view is a
  quaternion; perspective projection, back-face culling, painter's order by cubie distance, hit
  test for taps.
- `Cube3D` draws the projected quads on a Compose `Canvas`; drag turns the view (`CubeViewState`),
  tap returns a sticker index, `marked` stickers get a red outline. Colours are fixed real-cube
  colours (`StickerColors`), not themed.
- `CubeAnimator` queues moves and animates them (300 ms quarter; a half turn is two quarter steps
  with a pause, `onHalfway` between them; scaled by the phone's animator scale; instant when
  animations are off).
- Hold orientations per face (`FaceView` in `cube`) drive the manual input's preview and later
  the scanner's guidance.

## Move guide — Implemented

- `ui/guide/StepperState`: index over a move list, `done` / `back` / `demo` (play, pause 700 ms,
  snap back) and an automatic demo 500 ms after each new step (skipped when animations are off).
- `GuideCube`: `Cube3D` with the turning layer highlighted (other stickers mixed 60 % to grey), the
  direction arrow (`CubeScene.arrow`: an arc on the turning face, sweep = the move's angle, middle
  towards the camera) while the cube is still. One view everywhere (both methods, timer, camera
  follow): `CubeScene.DEFAULT_VIEW`; nothing turns it but the user's drag. A reset button
  (`ic_reset_view`) shows while `CubeViewState.isAt(DEFAULT_VIEW)` is false. Why: a view swinging
  round to L/B/D looked like the cube being turned and cost the user time after every such move.
- Mirror (`Cube3D(mirror = true)`, the guide only, not camera follow): a framed mirror fixed in
  camera space (`CubeScene.MIRROR_*`, above the cube, a little left, tuned by eye), its normal set
  so the ray to its centre reflects to the cube. The cube's quads are rotated by the view, then
  reflected in that plane (corner order reversed) and drawn clipped to the glass before the cube,
  so highlight, arrow and animation come for free and dragging changes the reflection only.
  `CubeScene.fit` scales the projection to cube + mirror (fixed, so dragging never zooms).
- `MoveWordsText`: the move in words, and its notation when Settings → Show move notation is on.
  One wording everywhere (`ui/common/MoveWords.parts`, also lessons and camera follow): as seen in
  the holding view (top left/right, sides up/down, back by its top row, front clockwise), whole-cube
  turns by centres. Other texts use the same layer/direction words.
- Haptics: `Confirm` on done, `SegmentTick` at the end of each demo.
- Hands-free (shortest solution only, `Stepper(shortest = true)` in `SolveScreen`; not learn,
  practice, scramble or camera follow): a tap on `GuideCube` (`onTap`) is done, with a hint chip
  until the first confirm. ▶ in the player row (or "Handsfree" on the start screen, with its speed
  chips) starts it at once; `StepperState.handsfreeStep` counts `HandsfreeSpeed.waitMs` from when
  the move appears while the demo plays (warning tick before the end) and confirms once the time is
  up and the demo has ended, without the 3 s demo repeat (`ui-polish`, 2026-10-06). A full-screen layer over the `Scaffold` stops it on
  any touch; `KeepScreenOn` (platform seam: view flag / browser wake lock) holds while it runs.
  Speed is a setting (DataStore / `StoredSettings`); handsfree itself is not saved. Why: the user
  wanted to keep both hands on the cube; spoken commands were left out as fragile (2026-10-06).

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
- Frames whose grid picture is not a cube face (the scan's `FrameSampler.looksLikeCube`) skip
  `FollowTracker` entirely (no advance, no learning) and the panel asks to bring the cube into the grid.
- `CameraPermissionGate` is shared with the scan.

## Screens — Implemented

| Route | Screen | Notes |
|---|---|---|
| `HomeRoute` | Home | spinning hero cube that is the scan action (tap = video scan, drag = turn) with a round camera button on its lower edge; a row of five icons with one-word labels (Käsin, Opettele, Ajanotto, Vapaa, Kuviot); the version |
| `LessonsRoute`, `LessonRoute(index)` | Lessons | basics + 7 stages, algorithm demos |
| `PracticeRoute(stage, seed)` | Practice | the solution screen limited to one stage |
| `TimerRoute`, `HistoryRoute`, `ScrambleGuideRoute(moves)` | Timer, history, guided scramble | the timer area explains hold-and-release only until the first timed solve is saved |
| `ScanRoute(face?, target?)` | Scan (with `face`: that face only, back to the check) | camera permission, grid, live dots, auto-capture; full-screen camera with the video scan's top row (`ScanOverlayBar`: back, six face marks, torch, ⋮ Videolla / Syötä käsin; a one-face rescan has no menu), no title and no "n/6"; at the bottom the "any face" request (until the first face), one status line, the hold bar, the round shutter and a ↶ redo icon; result → solve or check |
| `VideoScanRoute(target?)` | Video scan (the default scan) | full-screen camera with the real cube painted (solid tile = known, grey dashed = needed, white outline = side confirmed), a ring of stickers known, back / torch / ⋮ (Kuva kerrallaan, Syötä käsin, Korjaa värit), one status line; a notice at the bottom of the picture when stuck (scanning goes on); result → solve or check, on top of the scan |
| `ManualInputRoute(cube?, marked?, fromScan, confident, target?)` | Manual input / check a scan | one screen (palette, ‹ › icons and a ✓ main button in the bottom bar); face-by-face painting with `CubeEditor`, check with `CubeCheck`; valid → solution. From a scan: the face-by-face check (`ScanCheck`): its one-line instruction until the first paint, "looks right" or "scan again", checked faces ticked in the face map, the face's camera picture beside the grid (`LastScan`), "Kuvaa uudelleen" (one-face scan) and "Näyttää oikealta" in place of ‹ › and check, the verdict line, "scan the whole cube again" in the menu |
| `FreeCubeRoute(cube?)` | Free cube | layer buttons as small cubes (`Cube3D(compact = true)`: that layer lit and its arrow), a ↻/↺ toggle, scramble / undo / back-to-start icons, solve; the drag hint until the first drag |
| `SolveRoute(cube, target?, fromScan)` | Solution | background solve, then a start screen (`SolveScreen(startScreen = true)`: moves, target row "Kohde … Vaihda", method Nopein/Opettele, hold picture, "Aloita", "Handsfree" + pace), then the guide as a media player (⏮ previous, ▶/⏸ handsfree, ⏭ done, ↻ show again beside the words) with a ⋮ menu (camera follow, "Tarkista värit", back to the start screen); back in the guide returns to the start screen. A pattern or painted target hides the method choice (shortest only), a stage target uses the learn method. Practice and the guided scramble open the guide directly |
| `TargetRoute(start, current?, fromSolve)` | Choose a target | surprise, pattern gallery (tap → large preview → "Valitse"), stages, "Maalaa oma" (`ManualInputRoute(targetStart = …)`); a choice replaces the solution screen it came from; from home (tile "Kuviot") it first asks "Skannaa kuutio" (video scan carrying the target; scan, check and hand input pass it on to the solution) or "Kuutio on jo ratkaistu" (solution on top of the picker); a painted target from home comes back to the picker through its `savedStateHandle` to be asked about |
| `SettingsRoute`, `LogRoute` | Settings, log | the log viewer splits each line (`LogLine.parse`), shows its time in the phone's zone and the app language's format (`LogTime.format`, only the time for today) and colours it by level (error/warn/debug/info; the level word shown for non-INFO) |

## Release build — Implemented

R8-shrunk release (`app/proguard-rules.pro`: line numbers, navigation routes), signed from
`keystore.properties` / `RELEASE_*` env / debug key; `versionCode` = commit count. Settings →
About shows the name, version and one-line description; the open-source licences (min2phase's
MIT text from `shared/.../composeResources/files`, kept
identical to the vendored `LICENSE` by a test) unfold behind an "Open-source licences" button. Every green push to `main` replaces the
rolling release `latest-build` (the `publish` job in `ci.yml`), so
`releases/latest/download/rubikki-solveri.apk` is always the newest; `release.yml` builds tagged
releases. Both sign with the user's Android Studio debug key from the `RELEASE_KEYSTORE_BASE64`
secret (passwords default to the debug key's), so downloads update a Run ▶ install in place; no
secret → no publishing. Settings → About opens the download (`AppLinks.LATEST_APK`); the app makes
no network requests. See [operations.md](operations.md#release--implemented) and
[distribution.md](distribution.md).
