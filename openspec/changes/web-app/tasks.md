# Tasks

Container notes for every task: `gradle.properties` has `kotlin.js.yarn=false` and
`kotlin.daemon.jvmargs=-Xmx3g` (task 1.1); Maven Central may answer 429 — rerun; headless
Chromium needs `locale: 'fi-FI'`; Playwright's browser is at `/opt/pw-browsers` (never
`playwright install` in the container).

## 1. `cube` becomes multiplatform

- [x] 1.1 Gradle plumbing: add `kotlin-multiplatform`, `android-kmp-library`, `compose-multiplatform` plugins and the libraries from design §Context to `gradle/libs.versions.toml`; root `build.gradle.kts` `apply false` entries; `gradle.properties` additions (design §3). Verify: `./gradlew help` configures
- [x] 1.2 Convert `cube/build.gradle.kts` to KMP (`jvm()`, `wasmJs { browser() }`, `jvmToolchain(21)`, junit/kotlin-test in `jvmTest`); `git mv` `src/main/kotlin` → `src/commonMain/kotlin`, `src/main/java` → `src/jvmMain/java` (temporary), `src/test/*` → `src/jvmTest/*`; move `TwoPhaseSolver.kt` to `jvmMain` temporarily; replace the six JVM calls (design §2). Verify: `./gradlew :cube:jvmTest :cube:compileKotlinWasmJs` green, same test count as before (record it first with `./gradlew :cube:test` on the old layout)
- [x] 1.3 Fix the app's licence test path and run the Android build. Verify: `./gradlew :app:testDebugUnitTest assembleDebug` green
- [x] 1.4 Docs: `docs/architecture.md` Modules table (cube = KMP jvm + wasmJs) and `docs/development.md` (how to run `:cube:jvmTest`, container notes). Verify: commands in the docs run as written

## 2. min2phase in Kotlin

- [x] 2.1 Port `CubieCube`, `CoordCube`, `Util`, `Tools`, `Search` to Kotlin in `cube/src/commonMain/kotlin/fi/jukkakot/rubikkisolveri/cube/solve/min2phase/` (design §2: same structure and init order, `ushr`, explicit `toByte()/toChar()/toLong()`), MIT header + `LICENSE` copy naming the upstream commit. Verify: `:cube:compileKotlinWasmJs` and `:cube:compileKotlinJvm` green
- [x] 2.2 Move the Java original to `cube/src/jvmTest/java/cs/min2phase/`; `TwoPhaseSolver` back to `commonMain` using the port (`kotlin.time` for the timing). Verify: `./gradlew :cube:jvmTest` green
- [x] 2.3 `Min2phasePortTest` (jvmTest): 1 000 random-state cubes from fixed seeds + every scan fixture, solved by Java and Kotlin with the app's parameters (21 moves, 100 000 / 1 000 probes) → identical strings; `Tools.randomCube` identical for 100 seeds; table-init time printed. Verify: test green; a deliberately broken port line (local experiment, reverted) makes it fail
- [ ] 2.4 Point the app's min2phase licence text and `StringsTest`'s identity check at the new `LICENSE`. Docs: architecture "Solver" section (ported, oracle test). Verify: `./gradlew :app:testDebugUnitTest` green

## 3. `shared` module and resources

- [ ] 3.1 Screenshot baseline before any UI move: `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'`, copy `app/build/screenshots` to the scratchpad `baseline/`; write `scripts/compare-screens.py` (Pillow; per image the share of pixels whose max channel difference > 8; fail above 0.5 %; prints a table). Verify: comparing the baseline with itself prints all 0 %
- [ ] 3.2 Create `shared/` (design §3: android + wasmJs, compose resources with `packageOfResClass = "fi.jukkakot.rubikkisolveri.res"`, `publicResClass = true`); `include(":shared")`; `app` depends on it. Verify: `./gradlew :shared:compileKotlinWasmJs :app:assembleDebug` green
- [ ] 3.3 `scripts/android-strings-to-compose.py`: converts `values`/`values-en` strings and plurals to `shared/src/commonMain/composeResources/values{,-en}/strings.xml` (positional args, `\'` → `'`, keeps comments); run it; move fonts, the 7 UI vector drawables (not the launcher ones) and the two raw texts (`files/`); keep `app_name` + launcher resources in `app`. Add the new strings (`camera_denied_web`, `about_text_web`, `version_built`) in both languages. Verify: a JVM test in `app` (`ComposeStringsTest`) parses both XML files: same keys in fi/en, no `\'`, every `%` is `%%` or positional, same placeholder set per key in both languages
- [ ] 3.4 Test helper `Strings.fi(key)` / `Strings.en(key)` in `app/src/test` reading the compose XML; replace the 12 `R.string` lookups in tests. Verify: compiles (UI still uses Android resources at this point; tests still green)

## 4. Screens move to `shared` (Android unchanged)

- [ ] 4.1 Common logic first: move `ThemeMode`, `AppLanguage`, `ProgressRepository` (interface + data classes without Room annotations; `Clock.System` defaults), `InMemoryProgressRepository`, `SolveStats` (no `String.format`), `TimerState`, `Evt`, `Level`, `LogLine` (kotlin.time `Instant`), `Logger` (`LogStore` + `post`, design §4), common `AppLog`; in `app` add `TimedSolveEntity`/`GuidedSolveEntity`/`PracticeEntity` with the same table/column names and mappers, `LogFile : LogStore`, Android `AppLog.init`. Verify: `app/schemas/**/1.json` unchanged (`git diff --exit-code app/schemas`), `ProgressDatabaseTest`, `ProgressLogicTest`, `LogTest` green
- [ ] 4.2 Platform seams as `expect`/`actual` in `shared` (design §4 table): `CameraPreview`, `CameraPermissionGate` (move the Android code to `androidMain`, add `onTorchAvailable`), `platformColorScheme`, `LightStatusBarIcons`, `animationScale`, `elapsedMillis`, `argbToImageBitmap`, `LocalFormats`, `ScanPictureStore` interface (Android impl stays `ScanPictures` in `app`); wasmJs actuals as `TODO()` stubs except trivial ones. Verify: `:shared:compileDebugKotlinAndroid` (or the KMP android compile task) and `:shared:compileKotlinWasmJs` green
- [ ] 4.3 Move `ui/**` (all screens, nav graph + `AppActions` with the new `scanPictures` and `platform` fields, theme, common widgets, cube3d, guide, lessons, scan, progress, settings, log screen without the share intent) to `shared/commonMain`; `R.*` → `Res.*`; `androidx.activity.compose.BackHandler` → CMP `BackHandler`; JetBrains navigation/lifecycle imports. `MainActivity` builds `AppActions` as before. Verify: `./gradlew :app:testDebugUnitTest :shared:compileKotlinWasmJs` green — every existing test passes unchanged except imports
- [ ] 4.4 Screenshot diff: rerun `ScreenshotTest`, `python scripts/compare-screens.py <baseline> app/build/screenshots`. Verify: every image ≤ 0.5 % differing pixels (fix causes, never loosen the limit)
- [ ] 4.5 Remove now-unused Android resources and dependencies from `app`; update `proguard-rules.pro` if a kept class moved. Verify: `./gradlew test lint assembleDebug assembleRelease` green, release APK size within ±1 MB of before (record before in 3.1)
- [ ] 4.6 Docs: `docs/architecture.md` Modules, App structure and a new "Platforms" section (seams table); `docs/development.md` where code and texts live now (strings in `shared/.../composeResources`, how to add a string). Verify: links resolve; no doc mentions `app/src/main/res/values/strings.xml` as the text source

## 5. `web` module: the app in the browser

- [ ] 5.1 Create `web/` (design §3) with `BuildInfo` generation, `index.html` (loading note, WasmGC check with the bilingual "too old" note, language override, relative URLs), `platform.mjs` with storage/persist/vibrate/Intl/reduced-motion/performance functions, and `main()` showing the shared app with `WebServices` (design §7 storage, `LocalProgressRepository`, settings store, log store, `ScanPictureStore`, crash hooks). Verify: `./gradlew :web:wasmJsBrowserDistribution` green; Playwright (scratchpad script, production build, `locale: 'fi-FI'`) loads it without page errors and logs `RUBIKKI ready`; screenshot of home looks like the Android home (compare by eye with the baseline)
- [ ] 5.2 Wasm actuals for the seams (except the camera): Karkki fallback colours, `animationScale` from reduced motion, `elapsedMillis`, `argbToImageBitmap`, `LocalFormats` via `Intl`, haptics provider, language switch (write key + reload), theme-colour meta. Verify: unit tests in `app` for the shared pure helpers touched; Playwright: switch language to English in settings → after reload the home screen texts are English (check via the accessibility DOM if present, else screenshot comparison)
- [ ] 5.3 Navigation: `bindToNavigation` with home fallback, wide-screen column (design §7), timer space-bar handling (common, with a Compose test in `ProgressScreensTest` on Android: key down/up starts, key down stops). Verify: Compose test green; Playwright: open settings, `page.goBack()` → home; at 1280×800 the screenshot shows the centred column
- [ ] 5.4 Data: `LocalProgressRepository` and the settings/log/picture stores with in-memory fallback. Verify: Kotlin tests for their JSON encode/decode run in `app` unit tests against a fake key-value map (the logic lives in `shared/commonMain` with the browser binding a thin wrapper); Playwright: record a timed solve through the UI (hold/release space, press space) → reload → history shows it
- [ ] 5.5 `?selftest` mode (design §9). Verify: Playwright prints `SELFTEST ok …` with timings; record them in `design.md` under Speed

## 6. Camera in the browser

- [ ] 6.1 `platform.mjs` camera: start/stop, frame loop with cover-crop for a given aspect, canvases A (360 px preview) and B (264×264 analysis), torch/lock capabilities, error mapping (design §7). Verify: Playwright with Chromium's fake camera (`--use-fake-device-for-media-stream --use-fake-ui-for-media-stream --use-file-for-fake-video-capture=<y4m made from a scan fixture picture>`) opens the scan screen and the preview shows the picture
- [ ] 6.2 Wasm `CameraPreview`/`CameraPermissionGate` actuals: pull frames, `RgbaFrame` for `FrameSampler`, preview `ImageBitmap` with crop, `onTorchAvailable`, lock + `scan.lock lock=unsupported`, stall log, dispose. Pure geometry (`coverCrop(videoW, videoH, boxW, boxH)`) in `shared/commonMain` with unit tests in `app`. Verify: geometry tests green; with the fake camera showing a scanned face the live dots show its colours and an auto-capture happens (console log line `scan.capture`); with `--deny-permission` style launch (no fake-ui flag, permission denied in the context) the denied explanation shows
- [ ] 6.3 Log sharing: Web Share with files, else download (design §7). Verify: Playwright in a desktop context gets a download named `rubikki-log-*.txt` containing `app.start`

## 7. Install and offline

- [ ] 7.1 `scripts/web-icons.py` + committed icons, `manifest.webmanifest`, `sw.js` with the version placeholder, the `precache.json` Gradle task wired after the distribution task, service-worker registration in `index.html`. Verify: Playwright: first visit, then `context.setOffline(true)` and reload → home appears; Chromium's manifest check (`page.evaluate` fetch of the manifest + icons all 200)
- [ ] 7.2 Docs: `docs/operations.md` new "Browser version" section (address, add to home screen on the S24 in Chrome: ⋮ → Add to home screen → Install; offline; data per browser; clearing site data deletes it; logs via the share button). Verify: steps name exact menu labels

## 8. Publishing

- [ ] 8.1 `web/smoke/smoke.mjs` (design §9) and its npm `package.json` (playwright only), runnable both in CI and in the container (`PLAYWRIGHT_BROWSERS_PATH` respected). Verify: `node web/smoke/smoke.mjs web/build/dist/wasmJs/productionExecutable` passes locally
- [ ] 8.2 `ci.yml`: build the web distribution and run the smoke test in `check`; new `.github/workflows/pages.yml` (design §9). Verify: `actionlint` clean on all workflows
- [ ] 8.3 Docs: `docs/operations.md` Builds (Pages workflow, the one-time "Source: GitHub Actions" click), `README.md` link to the browser version. Verify: links resolve

## 9. Integration and roadmap

- [ ] 9.1 Full run: `./gradlew test lint assembleDebug assembleRelease :web:wasmJsBrowserDistribution` and the smoke test. Verify: all green
- [ ] 9.2 `openspec/context/product.md`: platform decision now "Android app and browser version from one Kotlin Multiplatform code base (2026-10-04)"; `openspec/context/nfr.md` if it states Android-only limits; roadmap item 27 `web-app` done, backlog note "solver Web Worker if the phone browser is slow". Verify: `openspec validate web-app` passes
