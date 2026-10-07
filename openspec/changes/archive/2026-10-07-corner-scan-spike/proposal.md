# Proposal

## Why

The video scan's weak point, seen again in the web test of 2026-10-07 18:19 (`scan-steady-progress`),
is telling **which face is in view and how it is turned**. Today that rests on the centre's colour,
and colour fails in real light: red reads orange, blue in shadow reads white, and on a patterned
cube one face can look like another turned round. Each fix so far patches one case.

A corner view (three faces meeting at a corner) carries that answer in its geometry. The three
centres must be the colours of one of the cube's eight corners, in that corner's clockwise order.
White–green–red and white–green–orange run opposite ways round, so once two faces are clear the
third is decided by handedness, not by how red the centre looks. The corner also fixes each face's
rotation. Two opposite corners show all six faces.

The user asked (2026-10-07) to start planning this bigger rework. Like `video-scan-spike`, this
spike answers offline, on the recorded videos, whether it is worth building before the app changes.

## What Changes

- **A corner reader** in `cube` (pure Kotlin, usable later on the phone and in the browser): from
  one frame's three full faces, the corner they form, the faces' names by that corner's colours and
  handedness, and each face's rotation.
- **A corner-anchored assembler** for the harness: faces named and turned by corner views (votes
  over frames), stickers voted as today, the cube checked with the best-cube fit.
- **A comparison** on every fixture with a known cube: frames to a finished cube and wrong cubes,
  corner-anchored against today's scan; also how often a fixture shows the two corners needed.
- **A fixture of the striped cube** from the 18:19 web recording (stills extracted locally, the
  finder's output committed as for the other fixtures).
- **`findings.md`** with the numbers, a go / no-go, and the recommended shape if go: corner naming
  hidden inside today's free scan, or a guided "show this corner, now the opposite one" scan.
- **No change to the app's behaviour.**

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None: a spike, no requirement changes (`skip_specs`).

## Impact

New code in `cube` (corner reader, kept for the follow-up if go) and JVM test harness code; stills
from `testdata/video/2026-10-07/web-181940/rec.mp4` into `testdata/video/2026-10-07/stills/`
(committed JPEGs, as before); one new fixture in `cube/src/jvmTest/resources/video/`. `app`,
`shared`, `web` untouched.
