# Tasks

## 1. Timings (shared, android)

- [ ] 1.1 `ScanImage` gets `prepMs`; `CameraPreview.android.kt` measures copy + upright + bitmap per frame on the camera thread; verify: shared/app compile
- [ ] 1.2 Finder loop in `VideoScanScreen`: `idleMs` from a small pure helper (previous answer → next picture taken), both into `FoundFaces`; verify: unit test of the helper and a `VideoScanLogTest` case with the fields, `./gradlew :app:testDebugUnitTest` green

## 2. Drop unused work (shared, android)

- [ ] 2.1 `onSamples` optional on `CameraPreview` (null = no grid sampling); the video scan passes null, guided scan and solve screen unchanged; wasm actual accepts it; verify: `./gradlew :app:assembleDebug :web:wasmJsBrowserDistribution` and the project's tests green

## 3. Docs and roadmap

- [ ] 3.1 `docs/architecture.md` (app pipeline already sends ahead; `prepMs`/`idleMs` on both); roadmap item 80 done with the phone check pending (app fps, `prepMs`, `idleMs`); item 79's phone check noted as done (2026-10-11 16:33)
