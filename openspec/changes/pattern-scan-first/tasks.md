# Tasks

## 1. Painted target fits any holding (cube)

- [ ] 1.1 A painted target's cube for a start cube is the painted cube turned as a whole (one of the 24 orientations, from the existing x/y/z rotations) so its centres match the start's; verify: JVM tests that a painted target is reachable from a start with other centres up/front, and is unchanged when the centres already match

## 2. Target through the scan

- [ ] 2.1 Video scan, guided scan and hand-input/check routes take an optional encoded target and pass it on (the switch between scans, the colour check, "scan again", and `afterScan`'s solution route, which keeps `fromScan` so back still leads to the scan); verify: a unit test that `afterScan` with a target puts it on the solution route, and the existing nav tests stay green
- [ ] 2.2 Ordinary scans from home (no target) behave exactly as before; verify: existing scan/nav tests green

## 3. Start question on the picker

- [ ] 3.1 From home, choosing a target (pattern, surprise, stage, painted) shows the dialog "Skannaa kuutio" (primary) / "Kuutio on jo ratkaistu", texts fi/en, no memory of the last choice; from the solution screen the picker works as today; verify: a Compose test that home → picker → choose shows the dialog and "already solved" opens the solution with the target
- [ ] 3.2 "Skannaa kuutio" opens the video scan with the target; back from the scan returns to the picker; verify: Compose/nav test that the scan route carries the target

## 4. Docs

- [ ] 4.1 Update the docs/ pages that describe the patterns flow and mark `pattern-scan-first` done in `openspec/context/roadmap.md`
