# Design

## Context

`cube-model` gives the URFDLB facelet string min2phase reads; `cube-view` gives the 3D view and
the animator. User decision: min2phase under its MIT option (product.md).

## Goals / Non-Goals

**Goals:** correct, fast, short solutions; a stepper a beginner can follow.

**Non-Goals:** arrows, layer highlight, haptics and holding-aware view (`move-guide`); notation
display (later option).

## Decisions

- **Vendored as unmodified Java** in `cube/src/main/java/cs/min2phase` with a `LICENSE` naming the
  commit and the MIT election. Unmodified so it can be diffed against upstream; the Kotlin JVM
  plugin compiles it. Android lint ignores that folder.
- **Search settings** measured (desktop, 40 random-state cubes): first solution ≤ 21 then at least
  1000 second-phase probes for a shorter one → 18.9 moves in ~25 ms (0 probes: 20.8 in 1 ms; 5000:
  18.8 in 135 ms; 20 000: 18.45 in 478 ms). 1000 is the knee. A first attempt to iterate
  `maxDepth - 1` searches could run for minutes on a cube already at its optimum, so the search
  relies on min2phase's own probe limits only.
- **Warm-up**: `Search.init()` builds ~1 MB of tables; run on a background thread from
  `RubikkiApp.onCreate`; `solve` calls it too (idempotent, synchronized).
- **Solving on `Dispatchers.Default`** from the screen's `LaunchedEffect`; the screen shows a
  progress indicator meanwhile.
- **Stepper state** = solution + index, kept in `rememberSaveable`; the 3D animator follows it.
  "Show" plays the move then snaps back after 700 ms. Back plays the inverse.
- **Move words**: side (partitive in Finnish: "yläpuolta"), direction, and "seen from" side, from
  string resources; built by a small function so the move guide can reuse it.
- **Routes**: `SolveRoute(cube)`; manual input and free cube navigate there. Manual input stays
  on the back stack so a mistake can be fixed.
- **Logging**: `solve.done` (length, ms) and `solve.failed` (reason).

## Risks / Trade-offs

- [Phone slower than desktop] → expected 5–10× slower, still well under a second; checked on the
  phone ("How to check").

## Decisions made while building

- The 3D cube was drawn too small; the projection scale now uses the worst-case projected radius
  of the cube (0.302 × scale) to fill 85 % of the canvas without ever clipping.
- Lint does not flag the vendored Java, so no lint exclusion was needed.
