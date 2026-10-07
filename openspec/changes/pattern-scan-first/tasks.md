# Tasks

## 1. Target through the scan

- [ ] 1.1 Video scan, guided scan and hand-input/check routes take an optional encoded target and pass it on (the switch between scans, the colour check, "scan again", and `afterScan`'s solution route); verify: a unit test that `afterScan` with a target puts it on the solution route, and the existing nav tests stay green
- [ ] 1.2 Ordinary scans from home (no target) behave exactly as before; verify: existing scan/nav tests green

## 2. Start question on the picker

- [ ] 2.1 From home, choosing a target (pattern, surprise, stage, painted) shows the dialog "Skannaa kuutio" (primary) / "Kuutio on jo ratkaistu", texts fi/en; from the solution screen the picker works as today; verify: a Compose test that home → picker → choose shows the dialog and "already solved" opens the solution with the target
- [ ] 2.2 "Skannaa kuutio" opens the video scan with the target; back from the scan returns to the picker; verify: Compose/nav test that the scan route carries the target

## 3. Docs

- [ ] 3.1 Update the docs/ pages that describe the patterns flow and mark `pattern-scan-first` done in `openspec/context/roadmap.md`
