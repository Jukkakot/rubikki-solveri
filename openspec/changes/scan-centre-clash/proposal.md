# Proposal

## Why

In a web scan on 2026-10-07 (screen recording and log, Samsung Internet) the white face never
scanned and the scan never finished. The cube sat on the table with the white face on top and the
blue face on the right, in shadow. Clean frames from the start of the recording, run through the
finder, show that every grid was found right. But the blue face's dark centre was named **white**,
so the blue face's readings (`YYY/YBB/YBO`) went into the white face's pile, next to the white
face's own right readings, in every picture. The wrong ones became the face's anchor (the reading
the others are compared with). The white face showed `YWR/YWB/YYY`, and the blue face got only its
centre for over half a minute.

It never recovered. A face's readings vote only when they agree with the anchor on at least 7 of 9
stickers, so the right readings counted for nothing. When a face holds more than 40 readings, the
ones that do not agree are dropped first, so the right readings were also thrown away first. All
the other faces were read exactly right, but with the white face wrong no possible cube fitted.

(A first guess, a grid fitted across the cube's edge, was wrong: the grids in those frames are fine.)

## What Changes

- **A. Two faces in one picture never share a centre colour.** When two faces found in the same
  picture name the same centre colour, the one that fits the colour better keeps it and the other
  takes its next-best colour not used in that picture. This catches a centre misnamed by light or
  shadow whenever the face it is mistaken for is also in view, as here.
- **B. Later clear readings win over earlier wrong ones.** A face's readings are kept as a window
  of the most recent ones (oldest dropped first), so wrong readings age out. The anchor is chosen by
  weighted support, so a group of readings that agree with each other wins once it outweighs the
  rest, even after the face was already known.
- **Readings from a face seen straight on weigh more than from a steep angle** (user,
  2026-10-07): each reading's votes and its support for the anchor count by how square its grid is.
- The 2026-10-07 frames become a test fixture: the finder's readings of 14 clean frames from the
  recording's start (before the paint appears), with the blue centre made pale as the camera saw it.

Decisions (`design.md` holds the weights and the window):
- Keep the single-frame protection: one wrong frame still changes nothing; only a sustained group
  of agreeing readings can replace known stickers.
- No new UI. The scan just recovers.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: "Recognised by agreement" (one centre colour per face in a picture; later clear
  readings can replace known stickers; straight-on readings weigh more).

## Impact

- Modules: `cube` only (`VideoScan`: centre naming per picture, readings window, weighted anchor
  and votes). No change in `shared`, `app` or `web`.
- Test data: new fixture `cube/src/jvmTest/resources/video/20261007_web.txt` (already extracted
  while planning); the clean frames go under `testdata/video/2026-10-07/` like the earlier videos.
