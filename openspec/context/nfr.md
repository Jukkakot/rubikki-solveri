# Non-functional requirements

Apply to every change. Designs and task lists must show how they are met.

## Budget and platform
- 0 €: no server, no paid API, no store account. Everything runs on the phone (or in the browser,
  served as static files by GitHub Pages), offline.
- Android 12 (API 31) and newer; reference device Samsung Galaxy S24. Phones only, portrait first.
  The browser version: current Chrome, Edge, Firefox, Safari 18.2+; on wide screens a phone-width
  column.
- Only the author uses the app: no onboarding for strangers, no accounts, no analytics.

## Performance (reference device)
- Cold start to the home screen under 1 s; every tap gives feedback at once.
- Animations at 60 fps. Heavy work (solving, image analysis) never runs on the main thread.
- The two-phase solver answers in under 1 s for any valid cube; the camera colour analysis keeps
  up with the preview (at least 10 analysed frames per second).

## Error UX
- Expected problems (invalid cube, unreadable sticker, camera permission denied) get a short
  localized message that says what to do next.
- A crash shows "The app crashed last time – show log" on the next start.
- Technical details go to the log only, never to the UI.

## Logging
- One line per event: `ts level evt key=value … msg="…"`; `evt` from the fixed catalogue in the
  app (`app.start`, `app.crash`, `nav.screen`, `settings.changed`, …). New features add their events
  there; filtering by name must keep working.
- Every line goes to Logcat (tag `Rubikki`) and to a file on the phone capped at 512 kB (oldest
  lines dropped). The log screen in settings shows it and shares it.
- Errors are one line with the stack in a field, never multi-line output.
- `app.start` carries the version (with the short commit), Android version and device.
- Key domain events are logged: scan result and confidence, validity failures with the reason,
  solver time and length. Never images.
- Nothing leaves the phone except when the user shares the log. Remote upload (Axiom dataset
  `games`) is a later option.

## Testing (required in every change)
- Cube logic (state, moves, validity, colour classification, solvers) lives in the pure JVM `cube`
  module with unit tests; every spec scenario maps to at least one test named after it. Property
  style tests (random scrambles, round trips) where an invariant exists.
- Test each behaviour once, at the lowest level that can show it. Screens get a few Compose tests
  (Robolectric, on the JVM) for key interactions; looks are checked on the phone.
- Work that only the phone can show (camera tuning, how the 3D view feels) is built, unit-tested
  as far as possible, and listed under "How to check" in the change summary.
- `./gradlew test lint assembleDebug` passes before every commit; CI runs the same.

## Accessibility
- Basic: contrast from Material 3, tap targets ≥ 48 dp, respect the system's reduced-motion
  setting for non-essential animation. Cube colours may carry meaning, but each face is also named.

## Legal and privacy
- "Rubik's Cube" is a trademark: fine for a personal app; rethink the name before any public
  release. Never use its logo or box look.
- Third-party code keeps its licence text next to it (min2phase: `cube/src/main/java/cs/min2phase/LICENSE`, used under MIT).
- No license for our own code: all rights reserved.

## Development workflow
- Claude commits and pushes to `main` itself; CI guards `main`.
- Conventional commits (`feat:`, `fix:`, `docs:`, `chore:` …).
- No formatter; Android lint with warnings as errors.
- Dependencies are updated manually (no Renovate/Dependabot), in `gradle/libs.versions.toml`.

## Theme
- Material 3 with Material You dynamic colour; light and dark both designed; the phone's setting
  is followed unless the user forces one in settings.
