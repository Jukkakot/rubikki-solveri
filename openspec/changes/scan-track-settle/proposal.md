# Proposal: scan-track-settle

## Why

The rules scan keeps changing its mind about some face tracks every picture, even when nothing new is
read: a track goes `=none` → `U?U2` → `=none` … (`20261005_213929` #19, 20 pictures) or `=B3` ↔ `B?B3`.
A replay of every fixture (2026-10-08) finds such flips on 12 of 22 fixtures. In the long recordings
they sometimes show: in `web_121505` (picture range 66–373) 27 stickers change back and forth for seven
pictures, and in `web_084657` 16 stickers do so for twenty. A flipping state is also worked out again
every picture, so the skip from `scan-speed-up-3` cannot apply to it.

The cause: two checks judge a track with different costs and no gap between them. The joint assignment
of the open tracks settles a track when every other way costs `ASSIGN_MARGIN` (2.5) more, counting the
pair costs with the other open tracks. The recheck of a settled track looks at the track alone against
the settled tracks, and re-opens it as soon as another way is cheaper at all (`REOPEN_SLACK` = 0). What
one check sets, the other undoes in the next picture, and so on.

## What Changes

- A track does not go back to the decisions it just left (face, turn, no face) without a new reading of
  its own. Tried first and dropped: making the checks judge alike, and leaving a face-only track's turn to
  the cube (each moved the flips elsewhere and lost finishes; design Findings).
- No change in what the scan aims for: it still finishes on the fixtures it finishes on today, never
  wrongly, and about as fast.
- Gain in speed: a held cube reaches a steady state, so pictures without new readings skip the work
  (`scan-speed-up-3`'s skip) also in the long recordings.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`: "Recognised by agreement" gains that the scan's view of the faces holds still while
  nothing new is read (no blinking between two readings).

## Impact

- Module `cube` only (`scan/FaceTracks.kt`: the deciding steps, `hold`); tests in
  `RulesScanTest`. No change in `app`, `shared` or `web` (they show the same state, only calmer).
- Older scanner (Settings: earlier scanner) untouched.
