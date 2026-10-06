# Design

## Context

Current code:
- The solution screen (`SolveScreen`) is one composable. A top bar with the camera toggle sits
  above a method row (`MethodChoice`), a target row (`TargetRow`) and the `Stepper`. The `Stepper`
  holds the hold text, the guide, progress, words and the button row (undo, handsfree, "Näytä",
  "Tein sen").
- Handsfree opens `HandsfreeDialog` first.
- After a sure scan, `afterScan` pushes the check and then the solution, popping the scan. That is
  why back leads to the check.
- `HomeScreen` takes a primary entry and a list of entries. Its cube (`Cube3D`) turns by dragging.

Mockups: https://claude.ai/artifact/C1jszuyq4LdFNd4y9nmGoj (home "Kuutio on nappi", solution "3 ·
Aloitusruutu" followed by "2 · Mediasoitin", chosen 2026-10-06).

## Goals / Non-Goals

**Goals:**
- One start screen holding every choice.
- A guide with three player buttons and a menu.
- Back always leads to a new scan.
- Handsfree time counting from when the move appears.
- The home screen from the mockup.

**Non-Goals:**
- Changes to camera follow's own screen.
- Changes to lessons, the timer or the free cube.
- Changes to the colour check itself.

## Decisions

1. **The start screen is a phase of the solution route, not a new route.**
   - `SolveScreen` gets a `started` state (`rememberSaveable`). False shows the start screen, true
     shows the guide.
   - The solution plan, target and method stay in one place, and changing the target keeps
     recomputing as today.
   - Back in the guide (`BackHandler`) sets `started = false`. Back on the start screen leaves the
     route.
   - Practice and the scramble guide pass `startScreen = false`.
   - *Alternative:* a separate `SolveStartRoute`. Rejected, because the plan would have to be handed
     over or computed twice.

2. **Back stack after a scan.**
   - `afterScan` no longer pops the scan:
     - A sure scan navigates to `SolveRoute` on top of the scan.
     - An unsure one navigates to the check on top of the scan.
   - Going back from either re-enters the scan route. Its state lives in `remember`, so the scan
     starts fresh.
   - From the check, a valid cube opens the solution with `popUpTo<ManualInputRoute> { inclusive =
     true }`, so back from the start screen again leads to the scan.
   - "Scan again" in the check pops back to the scan.
   - The guided scan (`ScanRoute`) follows the same rules.
   - Manual input, the free cube and patterns keep their own back stack: back from their start
     screen returns to them.

3. **Colour check from the guide menu.** The menu opens `ManualInputRoute(fromScan = true)` with the
   last scan's colours and pictures (`LastScan`) when the solve came from a scan. Otherwise it opens
   the solve's cube in manual input. A valid result replaces the solution (`popUpTo<SolveRoute>
   inclusive`).

4. **Player controls.**
   - The row holds ⏮ (`ic_undo`), ▶/⏸ (a 64 dp filled round button, only where handsfree is
     offered) and ⏭ (filled when ▶ is absent, so the main action is still clear).
   - ↻ is a small round icon at the cube's lower right corner. It is the existing reset-view
     button's opposite corner, so the two never overlap.
   - The timeline is a row: current move, bar, total.
   - The tap-on-cube confirmation stays as specced.
   - Content descriptions keep the old words ("Edellinen", "Handsfree", "Tein sen", "Näytä") for
     accessibility and tests.

5. **Handsfree timing.**
   - The move's time starts when the move is presented, the same moment the arrow appears.
   - The guide moves on at whichever comes later: the time is up, or the demo has finished. The
     next move therefore never cuts a turn in half. At the fast speed (1 s for a quarter turn) the
     demo, about 0.5 s pause plus the turn, may set the pace.
   - The bar fills over the same span.
   - `HandsfreeDialog` is removed. The speed chips (slow, normal, fast) move to the start screen
     under "Handsfree", and the choice is remembered as today.

6. **Hold text.** It is shown on the first move and on a move whose hold differs from the previous
   move's (whole-cube turns in the learn method). Otherwise it is hidden. The start screen always
   shows the hold as a small cube picture with one line.

7. **Home.**
   - The cube's tap opens the scan. A tap is a press released within the touch slop and 300 ms, so
     drags keep turning it.
   - A 64 dp round primary button with the camera icon sits centred on the cube's lower edge, with
     "Skannaa" under it.
   - The other five entries are a row of icons with one-word labels: Käsin, Opettele, Ajanotto,
     Vapaa, Kuviot.
   - `HomeSummary` and the tagline go. The version line stays.

## Risks / Trade-offs

- [A guide hidden behind a start screen adds one tap before solving.] → "Aloita" is the big
  primary action, and "Handsfree" starts directly.
- [A tap on the home cube may start a scan when the user meant to spin it.] → The tap needs a quick
  release without movement, and the scan's back returns home at once.
- [The back stack now keeps the scan screen alive under the solution.] → Its composable is disposed
  while it is not shown. The camera is released by `DisposableEffect` as today.
