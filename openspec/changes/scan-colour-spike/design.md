# Design

## Context

See proposal.md – Why. The finder looks for faces in a picture 240 px on its long side
(`FrameSampler.FINDER_LONG_SIDE`). A sticker's colour is the colour of its blob in that same
picture. The scan names each reading against references, which are the cube's own centres once a
face is settled (`FaceTracks.balancedRefs`). Recordings (`ScanRecording`) keep each face's lattice
(centre and steps in the finder picture) and its nine RGB values, but no pixels. The committed stills
are 360×640 JPEGs, and `VideoFixtures` lists the true cube for each source.

## Goals / Non-Goals

**Goals:** numbers that decide whether A, B or both are worth building, and what they would cost in
time per picture.

**Non-goals:** changing `VideoScan`, `FaceTracks` or the finder; tuning thresholds; the phone app.

## Decisions

1. **Idea A re-reads at the found lattice, not with a second finder.** For each face found at 240 px,
   each sticker is read again from the full still. The read is a square around
   `centre + i·u + j·v`, scaled to the still, half a step wide, trimmed mean per channel as
   `FrameSampler.sample` does. The reading keeps its lattice and only its colours change, so
   today's scan runs on it unchanged. *Alternative:* run the finder at full size. That costs four
   times the finder time and changes which faces are found, which mixes two effects.

2. **Idea B is a reading transform in front of the scan.** It holds the warm readings of the scan
   so far: chroma above the colour threshold, hue between −30° and 45°. Their hues are split in two
   with 1-D 2-means, red being the lower group. Each warm reading, centres included, is pulled to
   its group's mean colour, keeping its own lightness. The scan's references then come out of the
   same split, so `VideoScan` stays untouched. The split starts only after at least 9 warm readings
   in each group with group means 4° or more apart. Before that, readings pass through unchanged.
   *Alternative:* change `FaceTracks.balancedRefs`. That is the real fix's place if B wins, but a
   spike that edits the scan cannot be compared with today's on the same code.

3. **One comparison table per source and variant.** Each row reports:
   - stickers right: each tracked full face is matched to the true cube as in `ReadingMeasure.trueColors`
   - red ↔ orange mistakes, as a share of the warm stickers
   - finished (yes/no) and the picture or ms at which it finished
   - wrong cube (yes/no)
   - added time per picture for A

   Sources:
   - stills: every `FinderSizeHarness` source plus the evening videos (today, A, B, A+B)
   - recordings: every recording in `recordings/` whose cube is known (today, B)
   
   The true cubes for the 2026-10-11 recordings are the ones the user's next scans finished with.
   They are listed in the backlog, and `100403` is `WBBRWGWWYOR…`, the same cube as `101618`. Runs
   only with `COLOUR_SPIKE=1`.

4. **Go / no-go.** An idea is a go when all of these hold:
   - it finishes at least one of the two stuck recordings (B), or raises stickers right by 5 points
     or more on the evening stills (A)
   - it makes no source finish with a wrong cube
   - it makes no source finish more than 10 % later

   Both a go: build B first (no new picture work), then A.

## Risks / Trade-offs

- [Stills are 640 px at most and JPEG-compressed; the live camera gives 1280 px] → A's gain is a
  lower bound; say so in the findings, and measure the real cost in the browser only in the
  follow-up.
- [Screen-recorded stills carry the app's own paint over the cube] → report those sources apart;
  the camera videos decide.
- [B can lock in a wrong split early (only reds seen, split inside red)] → the start condition in
  decision 2; the findings list any source where B makes a wrong cube.
- [Two stuck recordings are one cube in one room] → also every other recording and the stills, so B
  is not tuned to one case.
