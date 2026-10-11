# Proposal

## Why

Video scans take long or never finish because sticker colours are misread, not because the cube is
turned too little. In the browser recordings of 2026-10-11, only 77–78 % of the stickers of tracked
faces were read right (94 % in a good scan). The scans at 10:04 and 10:16 never finished: oranges on
three faces read red the whole time, while the orange centre of a fourth face read orange. The two
warm colours are still apart in the scan's own readings (hue peaks at −6..3° and 9..18°). They fail
only against the references, which come from the centres under one face's light.

The user agreed (2026-10-11) to measure the two most promising fixes offline before the app changes:

- **A. Colours from a bigger picture.** Today the sticker colours come from the finder's 240 px
  picture, where a sticker is a few pixels across and blends with its gaps and neighbours.
- **B. Red and orange split by the scan's own readings.** The scan would use the 18 warm stickers
  it has seen, nine of each colour, to set the split instead of the centres alone.

## What Changes

- **A harness for each idea** in the `cube` JVM tests, run on demand like `ReadingMeasure`:
  - A re-reads each found sticker from the full still at the finder's lattice.
  - B rewrites the warm readings by a split learnt from the scan so far.
- **A comparison** on every source with a known cube. The stills fixtures get today's reading, A, B,
  and A+B. The phone browser recordings, including both stuck ones, get today's reading and B,
  because the recordings hold colours, not pictures. Each run reports:
  - share of stickers read right, and red/orange mistakes
  - whether and when the scan finishes, and wrong cubes
- **`findings.md`** with the tables, a go / no-go for each idea, and the follow-up change to build
  (browser first, the phone later as with `scan-send-ahead`).
- **No change to the app's behaviour.**

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None: a spike, no requirement changes (`skip_specs`).

## Impact

Test-only code in `cube` (`jvmTest`): the two variants and the comparison harness; `app`, `shared`
and `web` are untouched. The stills cover only 640 px on the long side, while a live camera gives
1280 px. Idea A's gain on the stills is therefore a lower bound.
