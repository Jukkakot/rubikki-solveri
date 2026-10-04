# Design

## Context

- Modules today: `cube` (Kotlin/JVM, no Android imports, plus the vendored min2phase **Java**
  solver in `cube/src/main/java/cs/min2phase`, 2 470 lines) and `app` (Android, about 5 700 lines
  of Compose UI and platform code, 297 strings in `res/values` (Finnish, the default) and
  `res/values-en`, 3 plurals, 9 vector drawables, 2 fonts, 2 raw licence texts).
- Android coupling in `app` is concentrated in 13 files: `MainActivity`, `RubikkiApp`, `AppLog`,
  `ScanPictures`, `ProgressDatabase` (Room), `Settings` (DataStore, AppCompat locales),
  `CameraPreview` (CameraX), `CameraPermissionGate`, `Theme` (dynamic colour, status bar),
  `CubeAnimator` (animator scale), `TimerScreen` (`SystemClock`), `ManualInputScreen`
  (`android.graphics.Bitmap`), `LogScreen` (share intent). The rest is plain Compose plus a few JVM
  calls (`String.format`, `java.time`, `DateFormat`, `System.currentTimeMillis`).
- `cube` uses only six JVM-only calls outside min2phase: `"%02x".format` (`ColorMath`),
  `java.util.Arrays.sort` and `Math.pow`/`Math.cbrt` (`FrameSampler`), `Math.floorMod`
  (`RotationSearch`), `System.nanoTime` (`TwoPhaseSolver`).

### Spike results (2026-10-04, in the cloud container)

| Checked | Result |
|---|---|
| Compose Multiplatform 1.12.1 + Kotlin 2.4.20, `wasmJs` browser executable | builds; renders in headless Chromium; production output ≈ 10 MB (skiko.wasm 8.2 MB, app wasm 1.7 MB, app.js 0.5 MB) before compression |
| `cube` as KMP (`jvm` + `wasmJs`), min2phase left in `jvmMain/java` | compiles for wasm after the six fixes above; all `cube` JVM tests pass |
| New `shared` module (`com.android.kotlin.multiplatform.library` from AGP 9.4.1 + CMP), compose resources | compiles for Android and wasm; `app` depends on it; `assembleDebug`, `assembleRelease` (R8) and `lintDebug` pass |
| `app` unit tests with `cube` as KMP | 144/145 pass; the one failure was the licence test's path to the moved `LICENSE` |
| Compose resources under Robolectric | `stringResource(Res.string.x)` renders, `values-fi`-style qualifiers follow `@Config(qualifiers)` and `robolectric.properties` |
| Camera preview behind a transparent Compose canvas | **not possible**: `ComposeViewport` has no public transparency option and `WebElementView` draws above the canvas and hides Compose content |
| Video frame into Compose (`Uint32Array` → `ByteArray` → skia `Bitmap` → `ImageBitmap`) | 31 ms per 640×480 frame on the container's CPU (per-element loop); ≈ 4× less at 320×240 |
| Container quirks | `codeload.github.com` is blocked (Kotlin's karma fork): `kotlin.js.yarn=false` (npm) works; the wasm compile needs `kotlin.daemon.jvmargs=-Xmx3g`; headless Chromium needs an explicit locale (`fi-FI`) or Compose throws "Incorrect locale information"; Maven Central sometimes answers 429 — rerun |
| Minified production build with a second `@JsFun` | failed with "X is not a function" while the development build of the same code worked → every `js()` / `@JsFun` interop lives in one hand-written JS module (`web/src/wasmJsMain/resources/platform.mjs`) imported with `@JsModule`, never as inline `@JsFun`; the smoke test runs the **production** build |

Versions to use (latest stable on 2026-10-04): Compose Multiplatform plugin and
`org.jetbrains.compose.{runtime,foundation,ui,components:components-resources}` **1.12.1**,
`org.jetbrains.compose.material3:material3` **1.9.0**,
`org.jetbrains.compose.material:material-icons-core` matching 1.12.x (or keep only the icons used
as vectors in resources — see Decisions), `org.jetbrains.androidx.navigation:navigation-compose`
**2.9.2**, `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` **2.11.0**,
kotlinx-datetime latest 0.7.x, kotlinx-coroutines 1.11.0, kotlinx-serialization 1.11.0.

## Goals / Non-Goals

**Goals:** one code base for both platforms; the browser version complete in one change; the
Android app unchanged for the user; every step verifiable in the container.

**Non-Goals:** syncing data between phone and browser; iOS/desktop native apps; a Web Worker for the
solver (only if the measured speed demands it, see Risks); pixel-identical colours between the
platforms (Material You is Android-only); a browser screenshot gallery.

## Decisions

### 1. Module layout

```
cube    KMP: jvm, wasmJs        cube logic, min2phase port (commonMain), tests in jvmTest
shared  KMP: android, wasmJs    all screens, nav graph, theme, texts, fonts, icons, licences,
                                platform interfaces; androidMain: CameraX preview, permission
                                gate, dynamic colour, animator scale, bitmap/time helpers
app     Android application     MainActivity, RubikkiApp, Room, DataStore, AppCompat language,
                                file log, crash handler, share intent; all existing tests
web     KMP: wasmJs executable  main(), browser storage/log/language/share/haptics, camera
                                (getUserMedia), index.html, PWA files, smoke test
```

Packages stay as they are (`fi.jukkakot.rubikkisolveri.ui.*`, `.progress`, `.settings`, `.log`),
so moved files keep their package and the existing tests in `app` keep their imports. The shared
resource class is `fi.jukkakot.rubikkisolveri.res.Res` (`compose.resources { packageOfResClass;
publicResClass = true }`). Alternative considered: a separate web UI duplicating the screens — rejected,
every future feature would be built twice.

### 2. `cube` to Kotlin Multiplatform, min2phase ported

- `kotlin("multiplatform")` with `jvm()` and `wasmJs { browser() }`; `src/main/kotlin` →
  `src/commonMain/kotlin`, `src/test/{kotlin,resources}` → `src/jvmTest/...` (tests keep reading
  fixtures from the classpath). The six JVM calls: `toString(16).padStart(2, '0')`,
  `IntArray.sort(from, to)`, `kotlin.math.pow`/`cbrt`, `Int.mod(4)`,
  `kotlin.time.TimeSource.Monotonic`.
- min2phase is ported by hand-converting the five Java classes to Kotlin in
  `cube/src/commonMain/kotlin/fi/jukkakot/rubikkisolveri/cube/solve/min2phase/` (same class
  names, same algorithm, statics → `object`s / top-level `val`s initialised in the same order,
  `>>>` → `ushr`, `char`/`byte`/`long` arithmetic kept with explicit conversions). Header comment
  and a `LICENSE` copy say it is a port of min2phase (upstream commit as in the vendored LICENSE),
  MIT. The Java original moves to `cube/src/jvmTest/java/cs/min2phase/` as a **test oracle**:
  `Min2phasePortTest` solves 1 000 random-state cubes (fixed seeds) plus the existing fixtures with
  both and asserts identical solution strings, and `Tools.randomCube` equality for fixed seeds.
  The app's licence text and its test point at the new `LICENSE` location.
  Alternative: a JS solver library in the browser only — rejected (two solvers, different
  solutions per platform).
- `TwoPhaseSolver` stays the facade. `warmUp()` is unchanged in meaning; on the web it is called
  once, 1.5 s after the first frame of the home screen (see Speed).

### 3. Gradle setup

- `gradle/libs.versions.toml`: plugins `kotlin-multiplatform`, `android-kmp-library`
  (`com.android.kotlin.multiplatform.library`, version `agp`), `compose-multiplatform`
  (`org.jetbrains.compose`, 1.12.1); libraries listed in Context.
- `shared/build.gradle.kts`: `kotlin { android { namespace = "fi.jukkakot.rubikkisolveri.shared";
  compileSdk = 37; minSdk = 31; androidResources { enable = true } }; wasmJs { browser() } }`,
  `commonMain`: `:cube`, CMP runtime/foundation/ui/material3/components-resources, JetBrains
  navigation-compose and lifecycle-runtime-compose, kotlinx-serialization-json (routes),
  kotlinx-datetime, coroutines; `androidMain`: CameraX (core, camera2, lifecycle, view),
  activity-compose, core-ktx. Plugin `kotlin-serialization` for type-safe routes.
- `app/build.gradle.kts`: add `implementation(project(":shared"))`; remove the moved resources;
  drop `navigation-compose` (comes through `shared`), keep Room/KSP, DataStore, AppCompat, CameraX
  (for lint), test deps. Android's compose BOM stays; Gradle resolves the androidx Compose
  versions CMP 1.12.1 maps to (spike: 1.12.1), which is fine.
- `web/build.gradle.kts`: `wasmJs { outputModuleName = "rubikki"; browser { commonWebpackConfig {
  outputFileName = "rubikki.js" } }; binaries.executable() }`, depends on `:shared`, `:cube`,
  kotlinx-browser. A `generateBuildInfo` task writes `BuildInfo.kt` (`VERSION_NAME` =
  `1.0.<count>-<sha>` computed as in `app`, `BUILT_AT` epoch millis) into
  `build/generated/buildinfo` added to `wasmJsMain`; configuration-cache safe.
- `gradle.properties`: `kotlin.js.yarn=false` (npm; the yarn path needs `codeload.github.com`),
  `kotlin.daemon.jvmargs=-Xmx3g`. Commit the generated `kotlin-js-store/` lock.
- `settings.gradle.kts`: `include(":cube", ":shared", ":app", ":web")`.

### 4. What moves to `shared` and how platform parts are cut

Everything under `app/src/main/kotlin/.../ui/**`, plus `progress/{ProgressRepository (interface
and data classes only), InMemoryProgressRepository, SolveStats, TimerState}`,
`settings/{ThemeMode, AppLanguage}`, `log/{Evt, LogLine, Level, Logger, AppLog (common part)}`
move to `shared/src/commonMain`. Platform seams:

| Seam | Common side (shared/commonMain) | Android | Browser (web) |
|---|---|---|---|
| Camera preview | `expect @Composable fun CameraPreview(torch, onSamples, onError, modifier, lockExposure, onPicture, onTorchAvailable)` | current CameraX code (shared/androidMain); `onTorchAvailable(hasFlashUnit)` | `getUserMedia` (section 7) |
| Camera permission | `expect @Composable fun CameraPermissionGate(...)` (same signature) | current code | asks through `getUserMedia`; denied → explanation text `camera_denied_web` + manual input |
| Theme colours | `expect @Composable fun platformColorScheme(dark: Boolean, dynamic: Boolean): ColorScheme?` (null → Karkki fallback) | `dynamic*ColorScheme(context)` | always null |
| Status bar in `ForcedDark` | `expect @Composable fun LightStatusBarIcons(off: Boolean)` | current window code | no-op |
| Animation scale | `expect @Composable fun animationScale(): Float` | `ANIMATOR_DURATION_SCALE` | `matchMedia('(prefers-reduced-motion: reduce)')` → 0f else 1f |
| Clock for the timer | `expect fun elapsedMillis(): Long` | `SystemClock.elapsedRealtime()` | `performance.now()` |
| Wall clock | `kotlin.time.Clock.System` (stdlib) everywhere; `System.currentTimeMillis()` defaults in `ProgressRepository` become `Clock.System.now().toEpochMilliseconds()` | | |
| Pictures | `expect fun argbToImageBitmap(argb: IntArray, w: Int, h: Int): ImageBitmap` | `Bitmap.createBitmap(...).asImageBitmap()` | skia `Bitmap.installPixels` → `asComposeImageBitmap()` |
| Dates and times shown | `expect object LocalFormats { fun shortDateTime(epochMillis: Long, lang: String): String; fun timeOrDateTime(epochMillis: Long, lang: String): String }` — `LogTime` and the history rows call it | `java.time`/`DateFormat` (current behaviour; `LogTimeTest` keeps testing it) | `Intl.DateTimeFormat(lang, {dateStyle:'short', timeStyle:'short'})` |
| Number formatting | `SolveStats.format` and `ColorMath` use `padStart`, no `String.format` | | |
| Back in sub-states | `androidx.compose.ui.backhandler.BackHandler` (CMP common) replaces `androidx.activity.compose.BackHandler` | | |
| Haptics | `LocalHapticFeedback` as now | system | `web` provides `LocalHapticFeedback` = `navigator.vibrate(20)` for Confirm, `(8)` for ticks, no-op if absent |
| Opening a link | `LocalUriHandler` (phone-install already uses it) | | `window.open(url, "_blank")` (CMP default) |

App services stay injected through `AppActions` (already the pattern). New fields:
`scanPictures: ScanPictureStore` (interface: `save(face, argb): String`, `list()`, `clear()`;
Android = current `ScanPictures`, web = localStorage), `platform: Platform` (`ANDROID`/`WEB`, used
only to pick the about text and the version suffix wording). `AppLog` common: `object AppLog
{ lateinit var logger: Logger; fun install(...) }` with `Logger(store: LogStore, sink, post:
(() -> Unit) -> Unit, clock)`; Android keeps `LogFile` + executor + Logcat + `flush()` in `app`,
web uses an inline `post` and a localStorage `LogStore`.

`RubikkiNavHost` and `AppActions` move to shared unchanged in shape; `MainActivity` keeps building
them. `collectAsStateWithLifecycle` comes from JetBrains lifecycle-runtime-compose (common).

### 5. Resources

- `app/src/main/res/values/strings.xml` → `shared/src/commonMain/composeResources/values/strings.xml`
  (Finnish = default), `values-en` → `values-en`. Converted by a committed one-off script
  `scripts/android-strings-to-compose.py` and checked by a test: Compose resources support only
  positional args, so `%d`/`%s` become `%1$d`/`%1$s` (6 places, mainly plurals); `\'` → `'`
  (15 places); `%%` and `\n` checked. `app_name` also stays in `app/src/main/res` (the launcher
  label needs an Android resource), as do `themes.xml`, `colors.xml`, the launcher icons, `xml/`.
- `R.string.x` → `Res.string.x`, `R.plurals.x` → `Res.plurals.x`, `stringResource` /
  `pluralStringResource` from `org.jetbrains.compose.resources`.
- Fonts → `composeResources/font/` (`Font(Res.font.fredoka, …)` with the same variable weights).
  On the web, fonts load asynchronously: `RubikkiTheme` keeps the system font until
  `preloadFont` returns, so text never flashes in a wrong shape on Android (synchronous there).
- Drawables (vector XML) → `composeResources/drawable/` (`painterResource(Res.drawable.x)`); the
  launcher foreground/monochrome stay in `app`.
- Raw licence texts → `composeResources/files/` read with `Res.readBytes("files/…")`
  (`StringsTest`'s identity check against the vendored `LICENSE` updated).
- Strings added: `camera_denied_web`, `about_text_web`, `version_built` ("Koottu %1$s" / "Built
  %1$s"), `browser_too_old` lives in `index.html` (it must show before Kotlin runs).

### 6. Android app after the move

`app` keeps `MainActivity`, `RubikkiApp`, `ProgressDatabase` (Room entities become
`TimedSolveEntity` etc. with the **same table and column names**, mapped to the common data
classes; `app/schemas/.../1.json` must stay byte-identical → no migration), `RoomProgressRepository`,
`SettingsRepository`, `LanguageSetting`, `CrashHandler`, `LogFile`, file `AppLog` setup,
`ScanPictures`, `shareLogIntent`. Behaviour is unchanged; the regression net:

1. All existing `app` unit and Compose tests pass (imports `R.string` → a test helper
   `Strings.fi("key")` / `Strings.en("key")` that reads the compose-resource XML, for the 12
   places tests look up texts).
2. **Screenshot diff:** before the first UI move, run `ScreenshotTest` and keep
   `app/build/screenshots` as the baseline (copy to the scratchpad); after the move, rerun and
   compare every PNG with `scripts/compare-screens.py` (Pillow; `pip install pillow`): no image
   may differ in more than 0.5 % of pixels (font rasterising through CMP may shift anti-aliasing
   slightly); any larger difference is a bug to fix, not to accept.
3. `assembleRelease` with R8 still builds; `proguard-rules.pro` keeps the route classes (now in
   `shared`, same package names) — CMP resources need no extra rules (spike).

### 7. Browser platform (`web`)

**Entry and page.** `main()` installs the web services, then
`ComposeViewport(document.getElementById("app")!!) { WebApp() }`. `WebApp` wraps the shared app
in a `Box` that centres a column of `widthIn(max = 480.dp)` × full height when the window is wider
than 600 dp (background = theme `surfaceContainer`), provides `LocalHapticFeedback`, and calls
`window.bindToNavigation(navController)` (navigation-compose web history; routes appear in the
URL fragment so a reload never asks GitHub Pages for a missing path). If restoring a route fails,
it navigates to `HomeRoute`. `index.html`: viewport meta (`viewport-fit=cover`,
`interactive-widget=resizes-content`), theme colour, manifest link, a loading note (cube emoji +
"Ladataan… / Loading…") removed by Kotlin after the first frame, the WasmGC feature check (the
`gc` probe from wasm-feature-detect, inlined) that shows the bilingual "too old" note instead of
loading `rubikki.js`, and the language override (below). All URLs relative (Pages serves under
`/rubikki-solveri/`).

**JS interop.** One module `web/src/wasmJsMain/resources/platform.mjs` (served next to the app)
holds all browser code: storage get/set/remove, `navigator.storage.persist`, vibrate, share/
download, `Intl` formatting, reduced-motion query, `performance.now`, and the camera. Kotlin sees
it through `@JsModule("./platform.mjs") external object Platform { … }` with only `String`,
`Int`, `Double`, `Boolean`, `JsAny`-typed members. No inline `@JsFun`/`js()` (spike: minified
build broke).

**Storage.** `localStorage` with JSON (kotlinx-serialization):
`rubikki.settings.v1` (`{theme, notation}`), `rubikki.progress.v1` (`{nextId, timed[], guided[],
practice[]}`; `LocalProgressRepository` holds `MutableStateFlow`s, writes the whole object after
each change — a few hundred solves are a few tens of kB), `rubikki.log.v1` (lines, capped at
512 kB by dropping the oldest, as on the phone), `rubikki.scanpics.v1` (newest 12 `{name,
pngBase64}`; PNG via skia `Image.encodeToData(PNG)`), `rubikki.language` and `rubikki.crashed`.
All access wrapped so a blocked storage (private mode) degrades to in-memory with one warn line.
`navigator.storage.persist()` requested once at start.

**Language.** `index.html` runs before the app: `lang = localStorage['rubikki.language'] || 'fi'`;
`Object.defineProperty(navigator, 'languages', {get: () => [lang]})` and `navigator.language`
likewise, so Compose resources and `Intl` use it. Changing the language in settings writes the key
and calls `location.reload()`. (Android keeps AppCompat per-app locales.)

**Theme.** `platformColorScheme` → null, so the Karkki `LightFallback`/`DarkFallback` schemes the
Android tests already render are used. `isSystemInDarkTheme()` follows `prefers-color-scheme`
(CMP web). The `<meta name="theme-color">` is updated from Kotlin on theme change.

**Camera (`CameraPreview` actual).** `platform.mjs` starts
`getUserMedia({video: {facingMode: {ideal: 'environment'}, width: {ideal: 1280}, height:
{ideal: 720}}, audio: false})` into an off-screen `<video playsinline muted>` (1×1 px, not
`display:none`, which stops frames in Safari). Per video frame (`requestVideoFrameCallback`, else
`requestAnimationFrame`), throttled to 15 fps, it computes the visible part as `object-fit: cover`
for the Compose box's aspect ratio (passed from Kotlin on size change), and draws:
(a) the preview: the visible part scaled to 360 px on the long side into canvas A;
(b) the analysis frame: the centred square of side `shorter visible side` scaled to 264 × 264
into canvas B (so `FrameSampler`'s 72 % grid is 190 px, each cell ≈ 63 px, like the phone's
640×480 analysis).
Kotlin pulls both with `getImageData` → `Int8Array` → `ByteArray` (kotlinx-browser
`toByteArray`), builds `RgbaFrame(264, 264, rowStride = 1056, bytes, rotation = 0, crop = whole)`
for `FrameSampler` (unchanged), `onSamples`, `onPicture(FrameSampler.picture(frame))`, and shows the
preview as an `ImageBitmap` with `ContentScale.Crop`. Torch: `track.getCapabilities().torch` →
`onTorchAvailable(true)`; on toggle `applyConstraints({advanced: [{torch}]})`. Exposure lock: if
capabilities list `exposureMode` and `whiteBalanceMode` with `'manual'`, apply
`{exposureMode: 'manual', whiteBalanceMode: 'manual'}` (current values are kept by Chrome),
log `scan.lock lock=true`; otherwise log `scan.lock lock=unsupported`. Stall logging as on Android
(`scan.stall where=camera`). The stream stops when the composable leaves (`DisposableEffect`).
Errors (`NotAllowedError` → permission denied state; `NotFoundError` → "no camera" via `onError`).
Alternative considered and rejected by the spike: an HTML `<video>` under a transparent canvas.

**Log sharing.** `navigator.canShare({files})` → `navigator.share({files: [log.txt, …pngs]})`;
else download `rubikki-log-<time>.txt` through a blob link. Crash capture: `window.onerror` and
`unhandledrejection` hooks in `platform.mjs` call back into Kotlin's logger (`app.crash`), set
`rubikki.crashed`, and the next start shows the existing crash notice.

**Timer keyboard.** `TimerScreen` gets `Modifier.onKeyEvent` for `Key.Spacebar` mapped to the same
press/release handlers as touch (common code, so the phone with a keyboard gets it too); the
screen requests focus when shown.

**About.** `Platform.WEB` → `about_text_web` ("…Kaikki toimii selaimessa: mitään ei lähetetä
minnekään.") and the version line `1.0.<count>-<sha> · Koottu <date>`; the download button from
`phone-install` stays (leads to the APK).

### 8. Install and offline (PWA)

- `manifest.webmanifest`: name "Rubikki Solveri", short name "Rubikki", `start_url: "./"`,
  `scope: "./"`, `display: "standalone"`, `orientation: "portrait"`, background/theme colour
  `#1E2A3A` (the launcher background), icons 192/512 and a maskable 512.
- Icons: rendered once from the launcher foreground vector by a committed script
  (`scripts/web-icons.py`: draws the same cube on `#1E2A3A` with Pillow) into
  `web/src/wasmJsMain/resources/icons/`; committed PNGs, the script for redoing them.
- `sw.js`: cache name `rubikki-<VERSION_NAME>` (written into `sw.js` by the Gradle
  `generateBuildInfo` step through a placeholder). Install: precache the files listed in
  `precache.json` (written after the webpack step by a Gradle task listing the distribution:
  html, js, mjs, wasm, composeResources, icons, manifest). Fetch: navigation and `*.js`/`*.mjs` →
  network first, cache fallback; everything else → cache first. Activate: delete other
  `rubikki-*` caches. A new build is fetched in the background on the next start and used on the
  start after (spec: "at the latest on the second start").

### 9. Publishing and CI

- `ci.yml` `check` job: add `:web:wasmJsBrowserDistribution` and the smoke test
  (`node web/smoke/smoke.mjs`, with `npx playwright install --with-deps chromium` on the runner;
  in the container use `/opt/pw-browsers`): serves the production distribution with a tiny static
  server, opens it at 412×915 with `locale: 'fi-FI'`, fails on any page error, waits for the
  console line `RUBIKKI ready`, opens `?selftest` and expects `SELFTEST ok warmup=… solve=…
  beginner=… rotation=…` with warm-up < 4 000 ms and solve < 1 500 ms (headless, CI CPU), and
  saves `home.png` (fails if all pixels are one colour). Uploads the screenshots on failure.
- `?selftest` (in `web` main, not in the UI): runs `TwoPhaseSolver.warmUp`, solves three fixed
  scrambles, the beginner solver on one, and `RotationSearch` on one scan fixture, then logs the
  timings to the console and the app log.
- `.github/workflows/pages.yml`: on push to `main` (and manual), builds
  `:web:wasmJsBrowserDistribution`, runs the smoke test, `actions/upload-pages-artifact` with
  `web/build/dist/wasmJs/productionExecutable`, `actions/deploy-pages`
  (`permissions: pages: write, id-token: write`, environment `github-pages`, concurrency
  `pages`). Separate from `ci.yml` so the Android checks stay green before Pages is enabled.
- User step once: GitHub → repo → Settings → Pages → Build and deployment → Source: **GitHub
  Actions**.

### 10. Speed

- The home screen does not touch the solver. `warmUp()` runs 1.5 s after the first frame inside
  `LaunchedEffect` with `withContext(Dispatchers.Default)` (single-threaded in the browser: it
  runs on the main thread between frames; the home cube may stutter once). Solution screens
  already show a "computing" state while waiting.
- Before each heavy call (solve, `RotationSearch`, `MisreadSearch`) the screen yields one frame
  (`withFrameNanos`) so the spinner is drawn first.
- Measured with `?selftest` (production build, headless Chromium on the author's Windows desktop,
  2026-10-04): `warmup=190 solve=226 beginner=16 rotation=126` ms (solve = slowest of three).
  The phone's browser is still to be measured by the user (About → the version line is the build).

## Risks / Trade-offs

- [min2phase port differs subtly (overflow, signed bytes)] → the oracle test against the Java
  original on 1 000+ cubes; any difference fails the build.
- [The UI move breaks something on Android] → existing tests + per-screenshot pixel diff before
  any web work; the move is mechanical (same packages), done in small verified steps.
- [Solver too slow in the phone's browser (single thread)] → measured by `?selftest`; if the
  warm-up exceeds 4 s on the phone or a solve 2 s, a follow-up change moves the solver into a Web
  Worker running a second instance of the wasm module (out of scope here; recorded so the
  decision is not lost).
- [Camera preview frame rate on the phone] → 360 px preview at ≤ 15 fps (≈ 4× less work than the
  spike's 640×480); the analysis frame is 264×264. If the phone shows < 10 fps, lower the preview
  to 270 px (constant `PREVIEW_LONG_SIDE`).
- [Exposure lock unsupported on some browsers] → scan still works (the classifier learns the cube's
  colours); the log says `unsupported`.
- [First load ≈ 10 MB] → compressed by Pages (gzip), cached by the service worker; the loading
  note shows meanwhile.
- [Compose resources' async font loading on the web] → system font until loaded (a brief
  difference only on the first visit).
- [CMP and androidx Compose versions drift apart later] → CMP 1.12.1 maps to androidx Compose
  1.12.x; future upgrades bump both together (noted in docs/development.md).
- [Experimental APIs: `bindToNavigation`, `ComposeViewport`] → opt-ins confined to `web`; if
  `bindToNavigation` misbehaves, fall back to handling `popstate` in `platform.mjs` →
  `navController.popBackStack()` (back only, reload → home).
- [The container blocks yarn's GitHub download] → `kotlin.js.yarn=false`; CI uses the same.

## Migration Plan

Order: phone-install first (smaller, also touches CI). Then this change in its task order; the
Android app is releasable after every task group (the rolling APK from phone-install keeps
publishing). Rollback: revert the commits; the browser address keeps the last deployment until
Pages is disabled.

## Open Questions

- Real-phone feel of the browser camera (frame rate, exposure) — checked by the user after apply;
  constants are named for tuning.
