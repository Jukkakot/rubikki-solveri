# Tasks

## 1. Home

- [ ] 1.1 Rebuild `HomeScreen` after "Kuutio on nappi":
  - The cube's tap opens the scan.
  - A round camera button on the cube's lower edge.
  - The five other entries as an icon row with one-word labels.
  - No tagline, no `HomeSummary` line; the version stays.
  - Fits in portrait, in landscape and in a short browser window.

  Update the home Compose tests: a tap on the cube opens the scan, a drag does not, each icon opens
  its screen, and no summary line is shown with timed solves.

## 2. Back stack after a scan

- [ ] 2.1 Change `afterScan` and the scan routes:
  - A sure scan opens the solution on top of the scan.
  - An unsure scan opens the check on top of the scan.
  - The check's valid cube replaces the check.
  - "Scan again" returns to the scan.

  Verify with navigation tests: after a sure scan, back twice gives a fresh video scan; after an
  unsure one, back from the check gives the scan.

## 3. Start screen

- [ ] 3.1 Add the start phase to `SolveScreen`:
  - Number of moves, target (with change), method (fastest/learn), hold picture with one line.
  - "Aloita", and "Handsfree" with its speed chips.
  - The already-solved and invalid messages.
  - Practice and the scramble skip the start phase.

  Verify with Compose tests: after a scan the start screen shows the moves and the solved target;
  "Aloita" opens move 1; changing the target updates the count; practice opens the guide directly.

## 4. Guide as a player

- [ ] 4.1 Replace the guide's controls:
  - ⏮, ▶/⏸ and ⏭.
  - ↻ on the cube.
  - A timeline row.
  - A ⋮ menu with camera follow, the colour check and back to the start screen.
  - The method and target rows and the handsfree button are removed from the guide.
  - Back in the guide returns to the start screen.

  Update the solve Compose tests to find controls by their content descriptions. Verify:
  - ⏭ goes to the next move.
  - ⏮ undoes the previous move.
  - ↻ replays the move.
  - The menu's camera follow starts camera mode.
- [ ] 4.2 Show the hold text only on the first move and when the hold changes. Verify with a test:
  - The text is shown on move 1.
  - It is hidden on move 2 of the fastest method.
  - It is shown again after a whole-cube turn in the learn method.

## 5. Handsfree timing

- [ ] 5.1 In `StepperState` and `Handsfree`, start the move's time when it is presented. Move on when
  the time is up and the demo is done. Remove `HandsfreeDialog`: ▶ starts handsfree with the
  remembered speed, "Handsfree" on the start screen starts it from move 1. Verify with tests:
  - The bar starts filling during the demo.
  - At normal speed a quarter turn moves on after its time.
  - At the fast speed it waits for the demo's end.
  - A touch stops handsfree and shows ▶.

## 6. Finish

- [ ] 6.1 Update the screenshot tests for home, the start screen and the guide. Run
  `./gradlew test lint assembleDebug` and verify that it passes.
- [ ] 6.2 Update the docs pages that describe home, the solution screen or the scan's back
  behaviour (`docs/README.md`, `docs/architecture.md` where they mention them). In
  `openspec/context/product.md`, record the decisions of 2026-10-06:
  - symbols over words, but text where it tells the non-obvious;
  - back after a scan is a new scan.

  Add `ui-polish` to the roadmap as done. Verify by reading the changed pages once.

## Workflow follow-up

- Archive the change after it is pushed, and push again.
- On the phone: check how the home cube's tap and drag feel, and the handsfree pace now that the
  time starts with the demo.
