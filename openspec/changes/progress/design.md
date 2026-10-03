# Design

## Context

Everything stays on the phone (nfr). The app has no database yet; DataStore holds settings.

## Goals / Non-Goals

**Goals:** reliable local history, competition-style averages, a timer that feels right.

**Non-Goals:** cloud sync, export (the log share exists), inspection time, stackmat.

## Decisions

- **Room 2.8 with KSP 2.3** (KSP is no longer tied to the Kotlin version). Tables:
  `timed_solve(id, finishedAt, millis, penalty, scramble)`, `guided_solve(id, finishedAt, method,
  moves, durationMillis)`, `practice(id, finishedAt, stage, durationMillis)`. Schema exported to
  `app/schemas` for future migrations; version 1.
- **`ProgressRepository`** interface (Flows + suspend writes) with a Room implementation; screens
  get the interface (tests use an in-memory Room database under Robolectric).
- **Stats** (`SolveStats`, plain Kotlin): effective time = millis (+2000 for +2), DNF = infinite;
  aoN over the last N: drop one best and one worst, mean of the rest; more than one DNF → DNF;
  fewer than N solves → none. Best ignores DNF. Mean over non-DNF.
- **Timer state** (`TimerState`, plain Kotlin, clock injected): IDLE → HOLDING (finger down) →
  READY after 500 ms → RUNNING on release → STOPPED on tap. Release before READY returns to IDLE.
  The display ticks every frame while running.
- **Scramble**: `TwoPhaseSolver.randomStateScramble()` on a background thread. "Scramble with the
  guide" opens the solution screen in a scramble mode (start solved, moves = the scramble, finish
  text "Scrambled – start the timer"), reusing the move guide.
- **Recording**: the solution screen reports `onFinished(method, moves, durationMillis)`
  (duration from the first shown step to the last "done"); practice reports its stage. The nav
  host writes them through the repository on a background coroutine.

## Risks / Trade-offs

- [AGP 9 built-in Kotlin with KSP] → if incompatible, fall back to Room's non-KSP option is not
  available; would then use a small hand-written SQLite helper. Recorded if it happens.

## Decisions made while building

- KSP 2.3 works with AGP 9's built-in Kotlin; no fallback was needed.
- "Moves" in the history row is a plural resource (lint).
- The timer's hold/release/stop behaviour is tested on `TimerState` with an injected clock. A
  Compose gesture test of the running timer ran the test JVM out of memory (the display loop
  requests every frame and the manual test clock never idles), so the screen test covers the
  scramble and statistics only.
