# Design

## Context

The overlay (`ScanOverlayBar`) shows the six-side ring at the top, and the status line and the turn
demo (`TurnDemo`, after 2 s without progress) at the bottom. The scan state already carries the known
stickers (net, null = unknown), the confirmed sides, the read sides, how the cube is held
(`orientation`) and `undecided`. In the browser the state crosses from the worker as text
(`ScanStateCodec`).

The mockups are the artifacts "Skannausvinkit" (https://claude.ai/artifact/DUZUGQpvQXqVFvbndQLK8e),
"Kääntönuolet" (https://claude.ai/artifact/EN7iP1U4ECgB64o4Ca9JZd) and "Kulmarivi"
(https://claude.ai/artifact/UmWycfKzUvLKKv8o6sHKKE). The user chose E1 (all eight corners, read
ones dimmed with a tick), the ring removed, the small cube kept, and no arrows.

## Goals / Non-Goals

**Goals:**
- From the first picture, always show which corners are still to show and which are done.
- Point to the most useful next corner, steadily.

**Non-Goals:**
- Arrows on the real cube.
- A fixed routine.
- "Hitaammin/Suoremmin" hints (backlog).
- Changing how the scan reads or decides.

## Decisions

1. **Corner identity by centres.** A corner position is named by the three centre colours that meet
   there (standard scheme), so all eight can be drawn before anything is read.
   - Row order: the four white-side corners, then the four yellow-side ones, each going round in
     the same direction, so the row reads the same every scan.
   - A corner is read when its three stickers are known in the scan state.
   - At finish every corner counts as read, because the finish fills the cube.
2. **Corner picture.** An isometric corner: three rhombi (top, left, right) in the corner's colours
   with a dark outline, about 28 dp, drawn on a translucent dark pill behind the row.
   - A read corner keeps its place, at 25 % opacity, scaled to 0.8, with a white tick.
   - The next corner pulses (scale 1.0 → 1.22 over 1.1 s). There is no pulse under reduced
     motion.
3. **Next corner score.** The score is the number of stickers in the corner's view (its three
   sides, 27) that are unknown or in doubt (net null, or on a side not confirmed), plus 3 for each
   of the three sides whose turn is still open. Only unread corners are candidates. The previous
   choice stays unless another scores at least 1.5× more. It lives in `NextCorner.choose(state,
   previous)` in `cube`, as a pure function.
4. **Status line.** Before every side is read: as today ("show the grey parts"). After that, until
   clear: "Vielä N kulmaa" / "N corners left". The "turn the cube" text for two look-alike faces goes
   away; the pulsing corner takes its role.
5. **Small cube.** `TurnDemo` receives the sticker colours (known, or grey) and the needed set. The
   target is the shortest whole-cube rotation from the held orientation that brings the next
   corner's three normals towards the camera.
6. **Browser.** The read corners (an 8-bit mask) and the next corner (index) are added to the
   worker's state text.
7. **Log.** Snapshots get `corners=<read count>/8 next=<corner colours>`, so recordings show
   whether the pick helped.

## Risks / Trade-offs

- **"Corner read" may lag behind what the user sees**, for example when a sticker is in doubt even
  though the view showed it. Then the corner stays undimmed and keeps pulsing, which is the right
  prompt.
- **Eight pictures take room.** About 260 dp wide on a dark pill, which fits a 360 dp screen. The
  ring's space at the top is freed.

## Implementation notes

- **Open turns** in the score are the sides seen but not settled plus the faces an open track could
  be (`VideoScanState.openTurns`, empty once complete). `FaceTracks.turnsClear`'s close turns are not
  used: it runs only at the edge of finishing, so its last answer is often stale.
- **Ties** go to the earlier corner in the row, so the very first pick is the white–red–green corner.
  The choice starts with the first picture; before that (empty state) nothing pulses.
- **The tick stays full white** (with a dark outline) on a read corner, while the corner's colours
  are at 25 %: at 25 % the mockup's tick was hard to see on the camera picture.
- **Reduced motion:** the next corner is shown steadily at the pulse's full size (1.22) instead of
  not at all, so it stays marked; the demo's needed stickers keep a steady outline.
- **Blinking** on the small cube is the app's marked-sticker outline (red), on and off every 450 ms,
  on the unknown non-centre stickers of the corner's three sides. Unknown centres show their scheme
  colour, as before.
- **Every corner read but not clear** (only edges or doubts left): the status line says "show the
  grey parts" instead of "0 corners left".
- **The row also stays above the stall notice**, and its description (TalkBack) says "Kulmia luettu
  N/8. Seuraavaksi <colours> kulma".
- The ring's strings (`video_progress*`) and `video_status_turn` are removed; `RING_ORDER` lives on
  as `SIDE_ORDER` for the side demo.
