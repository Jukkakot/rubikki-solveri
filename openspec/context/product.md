# Product

## Idea

**Rubikki Solveri** is an Android app that scans your real Rubik's Cube with the camera and then
helps you solve it: first by showing the shortest solution move by move, later by teaching a human
method stage by stage so you learn to solve it yourself.

## Decided (2026-10-02)

- **Platform:** Android only, native: Kotlin + Jetpack Compose, Material 3, modern Android look.
- **Cube:** the standard 3×3.
- **First solver:** a third-party two-phase (Kociemba) solver library, shortest practical solution
  (about 20 moves). A human-method solver of our own comes later.
- **Input:** camera scan of all six faces (the app guides but never blocks; colours decided at the end, doubtful ones fixed in the editor). Manual painting works
  before the camera does.
- **Languages:** Finnish and English.
- **Showing the moves** is a core feature, not polish: every move must be unmistakable for a
  beginner (see Move guide).
- **Budget 0 €.** Everything runs on the phone; no server, no paid API. Google Play's one-time
  developer fee only if and when we publish.

## Move guide (ideas, to be specced)

- A 3D cube on screen that turns the same way as the user is holding the real one, the moving
  layer highlighted and an arrow showing the direction; the turn animates and can be replayed.
- One move per screen, big; notation (R, U', F2) shown small for those who learn it.
- **Camera follow-along (AR-like):** the user keeps the cube in front of the camera, the app finds
  the face, draws the turn arrow on top of the real cube and checks from the colours that the move
  was done, then moves on by itself. A 2D overlay on the visible face is realistic; full 3D tracking
  of the cube is a stretch goal.

- **Audience:** just the author for now (no store listing, no onboarding for strangers).
- **Language:** Finnish by default, English selectable in settings.
- **Look:** the most modern Android look: Material 3 with Material You dynamic colour.
- **Moves are shown with animation and arrows only;** standard notation may come later as an option.
- **After a scan:** if the scan is valid and confident, go straight to the solution; ask for a
  check only when something is uncertain or the cube is invalid.
- **Offline:** no accounts, no server. The only online part may be debug logs to the user's
  existing Axiom dataset `games`.
- **Solver library:** min2phase (two-phase), used under its MIT licence option.
- **Name:** "Rubik's Cube" is a trademark; fine for a personal app, but rethink the name before
  any public store release.

## Decided (2026-10-02, second talk)

- **No Google Play** (it needs a paid developer account). The app is installed straight onto the
  phone: from Android Studio during development, later as a signed APK.
- **The user's cube:** standard colour scheme (white/yellow, green/blue, red/orange), stickerless.
- **Holding the cube:** the app decides and tells the user (white on top, green facing you).
- **3D view:** drawn by the app itself with Compose (own projection), no 3D library; can change later.
- **Debugging:** local logging is enough for now (Logcat plus an on-phone log the user can view and
  share); the Axiom upload comes later.
- **Beginner method:** any human-understandable layer-by-layer method that teaches real cube
  knowledge.
- **Minimum Android:** 12 (API 31), the first with Material You dynamic colour.

## Decided (2026-10-03)

- **No tall pages.** Every screen should fit a phone in portrait without scrolling: primary actions
  always visible (bottom bar), secondary text short or behind a tap. Scrolling only where the
  content is a genuinely long list (history, log, lessons list), and even then the actions stay
  pinned. New and changed screens are checked against this.

## Later ideas

- **Five faces are enough (2026-10-03):** once five faces are scanned, the sixth can largely be
  worked out (each piece is known from its other stickers, the counts fill the rest), so the scan
  could skip it or use it only as a check. Not planned yet.
- **Video scan (2026-10-03):** instead of one still capture per face, the user turns the cube
  slowly in front of the camera and the app tracks it continuously, picking up each face as it
  comes into view. Not planned yet.
- **Turns animated in steps (2026-10-03):** in the solver, a face turn animates in clear steps
  (a quarter turn as one step, a half turn as two quarter steps with a pause between), so it is
  obvious how far to turn. Not planned yet.
- **Fewer whole-cube turns in the hands (2026-10-03):** keep the need to turn the real cube in the
  hands to a minimum, in the scan (done in `scan-any-order`: any order, any rotation) and in the
  solver (e.g. prefer solutions and move views that need no regrip). Not planned yet.
