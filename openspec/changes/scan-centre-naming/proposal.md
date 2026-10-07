# Proposal

## Why

Web test on 2026-10-07 at 14:55 (screen recording and log in
`testdata/video/2026-10-07/web-145543/`, version 8a55538, after `scan-centre-clash`): four video
scans, two good ones of 8–9 s, one of 23 s and one given up after 31 s.

In the 23-s scan the cube started with the blue face on top. In this light the blue centre reads
close to white, and with no white face in view the scan named it white. The blue face's readings
went into the white face's pile (the log shows the white face as `YYY/YWB/Y..`, the blue face's
stickers) and, frame by frame, partly into the blue face's pile as well. The screen showed many
wrong stickers as known for about 20 s. It all came right at once only when all six centres had
been seen well enough to be named together (`kind=side` for every side at 22 s). The scan given up
(not recorded) looks the same in the log: almost every sticker "known" after 4 s, many of them
wrong, then nothing for half a minute.

`scan-centre-clash` keeps two faces of one picture apart, but only when both are in view. A face
is still named by its centre alone against fixed reference colours, and once it has joined a pile
it stays there until all six are named together.

## What Changes

- **Faces are told apart by their own centres.** A face's readings go to the pile whose centre
  colour, as this cube shows it in this light, is closest; a centre clearly unlike every pile so far
  starts a new pile. Blue and white then make two piles even when both read "nearer white" against
  the reference colours.
- **Piles are named together, from the first.** Each time the piles change, the scan gives them
  colour names together, each colour once, the best fit overall (as it now does only when all six
  are seen). With five piles the sixth colour follows. A pile whose name changes keeps its readings.
- **A doubtful centre waits.** While a centre fits two colours about equally and the piles cannot
  decide it yet, its stickers are not shown as known; they become known once the naming is clear.
- Keeps: two faces of one picture never share a pile (`scan-centre-clash`), the readings window,
  one bad frame changes nothing.

Decisions (details in `design.md`):
- No new UI; the scan just gets it right sooner. The calmer paint is a separate change
  (`scan-paint-calm`, agreed with the user 2026-10-07).
- Test data: the screen recording has the app's paint over the cube from the first seconds, so the
  finder cannot read it. The regression test is built from the camera video `20261007_132721` (the
  same cube and light) with its blue-face frames moved to the start; a fresh camera video starting
  with the blue face on top would make a better fixture (the user will film one; used if it is there).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: "Recognised by agreement" (faces told apart by the cube's own centres; a doubtful
  centre's stickers not shown as known until the naming is clear).

## Impact

- Modules: `cube` only (`VideoScan`: centre piles and their naming; `ColorClassifier`: joint naming
  for fewer than six centres). No change in `shared`, `app` or `web`.
- Tests: `VideoScanTest` (new scenario test, all video fixtures as regression).
