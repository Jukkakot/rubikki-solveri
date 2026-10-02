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
- **Input:** camera scan of all six faces; tap to fix a misread sticker. Manual painting works
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
