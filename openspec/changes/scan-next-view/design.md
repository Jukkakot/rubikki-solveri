# Design

## Context

The overlay shows the ring (`ScanOverlayBar`), the status line, and the turn demo (`TurnDemo`,
after 2 s without progress). The scan state already carries the known stickers (net, null =
unknown), the confirmed sides, the sides read (`readSides`), how the cube is held (`orientation`),
and `undecided`. In the browser the state crosses from the worker as text (`ScanStateCodec`).

The user's mockups are the artifacts "Skannausvinkit" (https://claude.ai/artifact/DUZUGQpvQXqVFvbndQLK8e)
and "Kääntönuolet" (https://claude.ai/artifact/EN7iP1U4ECgB64o4Ca9JZd). Chosen: A (small cube with
the target) + C (colour dots), with no arrows.

## Goals / Non-Goals

**Goals:**
- After every side is read, always name the most useful next view, steadily.
- Progress that visibly grows sticker by sticker.

**Non-Goals:**
- Arrows on the real cube (decided against, 2026-10-09).
- Changing how the scan reads or decides.
- A guided fixed routine (such as two opposite corners).

## Decisions

1. **Score of a view.** It is the number of its stickers that are unknown or in doubt (net null, or
   a known sticker on a side not confirmed), plus 3 for each of its sides whose turn is still open.
   The candidates are 8 corners (3 sides, 21 stickers) and 6 sides (9 stickers). A side counts only
   when it alone holds most of the score: it wins when its score is at least 80 % of the best
   corner's. Pure function in `cube` (`NextView.choose(state, previous)`).
2. **Steady.** The previous choice stays unless another view scores at least 1.5× more.
   There is no choice while sides are unread, because the ring and the demo cover that, or once
   the cube is clear.
3. **Status line.** "Näytä kulma" / "Näytä sivu" ("Show corner" / "Show side") followed by
   coloured dots drawn as small circles in the side colours. They are not emoji, so they look the same
   everywhere. The dots pulse, scaling by 1.25 over 1.2 s, except under reduced motion.
4. **Ring.** A segment's arc length is its known stickers / 9, drawn over the faint full
   segment. It is full when confirmed. The pulse is the same scale animation on the chosen sides'
   segments.
5. **Small cube.** `TurnDemo` gets the sticker colours (known or grey) and the needed set. The
   target rotation is the whole-cube rotation that brings the chosen corner's (or side's) normal
   towards the camera from the held orientation. It uses the shortest rotation, so it is usually a
   single turn.
6. **Browser.** The next view (a corner as three side letters, or one side letter) is added to the
   worker's state text.

## Risks / Trade-offs

- **A wrong pick slows the user.** It shows only after every side is read, and only for views that
  hold unknown or doubtful stickers, so the worst case is a view that settles less than another.
  Logged as `next=` in snapshots so it can be checked from recordings.
- **Pulsing could feel restless.** Only the dots and the chosen segments pulse, and reduced motion
  turns it off.
