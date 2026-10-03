# Design

## Context

`ScanSession.onFrame` captured after `STABLE_FRAMES = 6` agreeing frames. The analysis rate depends
on the phone, so the hold time varied and was about half a second on the S24: too short while the
cube is still being turned.

## Decisions

- **Hold time 1.5 s, measured from frame timestamps.** `onFrame(samples, nowMillis)`; a face is
  captured when the live colours have stayed the same, with the right centre, for at least
  `holdMillis` and at least `minFrames` (3) frames. Any change of the live colours restarts the
  hold. 1.5 s is long enough to settle the cube and short enough not to feel slow for six faces;
  it is a constant to tune after trying it.
- **`ScanEvent.Holding(progress)`** replaces `Waiting` while the right face is steady, so the UI
  can fill a progress bar (0…1). `Waiting` remains for "nothing yet".
- **Review state in the session.** A capture stores the face as `review` and returns `Captured`;
  frames are ignored while reviewing. `accept()` stores it and moves on (or finishes); `retake()`
  drops it and asks for the same face again. Keeping this in the pure-Kotlin session keeps it
  unit-testable.
- **The review shows the live (default-palette) reading** of the captured median, as the grid dots
  did. The final classification by the cube's own centres can still correct a colour; the manual
  editor at the end stays the place to fix single stickers. No per-sticker editing in the review
  (it would duplicate the editor).
- **Review UI:** the camera box shows the nine read colours as large tiles over a dimmed preview;
  below, "Näyttää oikealta" (primary) and "Skannaa uudelleen" (outlined). Haptic confirm on capture
  as before. While reviewing, the capture and redo-previous buttons are hidden behind the review
  buttons (only two choices are meaningful then).
- **Tests and screenshots** set `holdMillis = 0` on `ScanContent` so frame-driven tests stay quick;
  the session's own tests cover the timing.

## Risks

- 1.5 s may feel slow once the user is practised; tune `HOLD_MILLIS` after phone testing.
