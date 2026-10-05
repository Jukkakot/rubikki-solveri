# Design

## Context

`VideoScan.onFrame` sets `complete` only when all 54 are recognised by vote (3 agreeing readings,
2× margin), no sticker is disputed and the rotation search finds a valid cube. `Validity.markedStickers`
is empty for wrong colour counts, twist, flip and parity, so those cases add nothing to show and
`finished` never comes. Readings are named per frame against the centres (`ColorClassifier.live`);
nothing uses the cube's structure until every sticker is known. `Orientation` already gives the
cube's rotation in the picture from the largest face found; `FaceReading` gives its centre and
sticker steps.

## Decisions

1. **Evidence per sticker.** Per sticker (in the net, after the faces' rotations) keep vote counts
   per colour. Cost of a colour = −log of its smoothed share; a vote for red also lends a part to
   orange and the other way round (they are the usual confusion), so a red/orange sticker needs
   more agreeing frames to be sure. A sticker without readings costs the same for every colour.
2. **Best cube by pieces.** Corners: an 8 × 8 assignment (Hungarian, already in `ColorClassifier`)
   whose cost per slot and piece is the cheapest of the piece's three twists; edges: 12 × 12 with
   two flips. The best assignment may break twist sum, flip sum or parity; then the cheapest fix is
   searched (change one twist/flip, or swap two pieces' places), as a small k-best search. Centres
   come from the colour scheme.
3. **When it is clear (decided by Claude, user's mandate: as few stickers as possible, a wrong cube
   avoided above all).** For each slot, the cheapest possible cube with that slot's piece or twist
   forced to differ; its extra cost is that slot's margin. The cube is clear when every slot's
   margin is over a threshold T. No cap on unseen or structure-corrected stickers: the margin is the
   rule. One extra guard: a slot decided without being read in full counts only when the stickers
   it rests on are themselves sure (votes alone, red/orange needing more). Clear for half a second →
   finish as today. Stickers whose colour came from the structure (unseen or corrected) are marked
   uncertain in the check behind the solution.
   Background (the user asked for the numbers): the cube has ~4.3·10¹⁹ states, so at least ~26
   stickers in theory; ~34 of the 48 non-centre stickers when well chosen (two per corner for six
   corners plus one, both per edge for ten edges plus one); in practice five whole sides fix the
   sixth.
4. **T from a simulation, checked on the videos.** A JVM harness simulates scans: random cubes,
   faces shown in random order and angles, per-sticker misreads at the rates seen in the spike
   (higher for red↔orange, a whole face's red/orange swapped in dim light) and readings arriving
   frame by frame. For each T it measures wrong finishes and stickers read at finish. T is the
   smallest value with zero wrong finishes in 10 000 runs, doubled as a safety factor; then both
   test videos must still give the true cube and never a wrong clear one. The numbers go into
   `findings.md` in this change.
5. **Marks on the real cube.** From the orientation, the main face's centre and step size, every
   sticker of the cube is projected into the picture (weak perspective). Sides whose normal faces
   the camera get marks: a solid dot (known: margin over threshold, or confirmed by votes), an empty
   ring (still needed). A done side gets a tick at its centre. Without a usable orientation, only the
   faces found are marked, as today. The progress cube and its arrow are removed.
6. **Turn arrow on the picture.** The existing tilt hint drawn as a large arrow beside the real cube
   (on the side it should move towards), anchored to the projected cube, fading when no face is seen.
7. **Stall reasons.** Checked each frame, shown as a panel over the picture with a restart button:
   too dark (median brightness of found faces under a floor for a few seconds: lamp icon), no cube
   (nothing found for a while: cube icon), stuck (cube in view but nothing new for 15 s (user), or
   no possible cube fits the evidence: refresh icon). The panel offers restart (primary) and the
   colour check with what is known; the button that opens the check early is renamed "Tarkista nyt" → "Korjaa värit" (user). Restart clears the scan, keeps the camera. Dim light also shows
   at once as a small lamp notice on the picture (user).
8. **Done sides row.** Under the picture, six dots in the side colours, a tick on each side fully
   known (user: progress on the cube itself, but which colours are done also somewhere).
9. **Log lines** (`scan.video` with a `kind`): `snapshot` every 2 s (known per side, best cube as
   54 letters with `?` where not sure, smallest margin, brightness, faces per frame, finder ms),
   `side` when a side becomes known, `clear` when the cube is clear, `stall` with its reason,
   `restart`, `leave` (back pressed, with the snapshot). A video scan of a minute gives some 30
   lines.

## Risks

- The structure can make a wrong cube look clear when a whole piece was misread consistently; the
  threshold is tuned on the test videos, and the check behind the solution stays.
- The projection drifts on sides seen at a steep angle; marks are drawn only for sides facing the
  camera enough, and the found faces' own lattices win where both exist.
