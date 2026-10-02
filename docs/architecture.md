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

## Planned

- Cube model and notation (`cube-model`), 3D cube view and manual input (`cube-view`), two-phase
  solver and solution stepper (`fast-solve`), camera scan (`camera-scan`), move guide
  (`move-guide`), camera follow-along (`camera-follow`), beginner solver (`beginner-solver`),
  lessons (`lessons`), timer and history (`progress`), signed APK (`release`).
