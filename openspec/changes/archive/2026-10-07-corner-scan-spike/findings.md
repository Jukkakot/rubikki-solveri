# Findings

Run: `CORNER_HARNESS=1 ./gradlew :cube:jvmTest --tests "*CornerScanHarness*"`; full table in
`corner-report.txt` (this folder). Eleven fixtures with known cubes, 2 438 frames. "Robustness"
fades every red centre towards orange and every blue one towards white until the palette names them
so, as the phone's camera did on 2026-10-07.

## 1. The corner reader is right when the lattices are right

Three full faces whose common corner sits about 1.5–2.7 steps out along both axes of each face
(perspective stretches the 1.5) are a corner; the three centres are named as one of 24 handed
hypotheses. On clean video every corner read was right in names and turns: striped cube 35 / 35,
web start 13 / 13, evening videos 26 / 26. The misses (`20261007_132721` 16 of 19,
`20261007_152753` 16 of 50) are frames whose lattices are already wrong (stickers of another face,
blue read as white); the check counts them against the reader, but the reader only trusts the
geometry it is given. The handedness sign is right on every source (task 2.2): no fixture is
mirrored.

The synthetic tests show handedness alone decides red against orange beside white and green.

## 2. Corners alone never finish a free scan

No fixture shows two opposite corners; corners cover three faces (five on the striped cube). A
corner-only assembler never finishes. A guided "show the opposite corner" would fix that, but the
user ruled guidance out (`product.md`, "Scanning as easy as possible"). Corners are a strong rule
inside the free scan, not a scan of their own.

## 3. Today's scan is fragile when two centres look alike

| | as recorded | robustness |
|---|---|---|
| today: finished right | 8 / 11 | 1 / 11 |
| today: wrong cube | 0 | 1 (`20261005_151828`, no corners in it) |
| with rules: finished right | 9 / 11 | 3 / 11 |
| with rules: wrong cube | 0 | the same 1 |

(3 fixtures never finish in either run as recorded: `20261007_132049` and `20261007_web` show only
three faces; the striped screen recording finishes only with rules, at frame 322.)

**Why:** faces are piled by how their centre looks before anything else. When the red centre looks
like the orange one, both faces go to one pile, and no later rule can split them. Adding the rules
only to the naming changed nothing; adding them to the piling (a face a corner names never joins a
pile its corner views name otherwise) is what made the striped cube and the hardened tour finish.

## 4. Verdict: go, as rules inside the free scan

Go bar (design): never a wrong cube, clearly fewer frames in the robustness run. Partly met: the
rules added no wrong cube and turned two never-finishing runs into right ones; the one wrong cube
is today's and comes from a fixture with no corners, which the corner rules cannot reach.

Recommended follow-up, **`scan-rules`** (in the spirit of the user's "every safe inference, all the
time"):

1. **Turn the experiment on** (`VideoScan(rules = true)` as the default): corner views name piles
   (handedness) and keep a face out of a pile its corners name otherwise; faces seen together are
   never named opposite colours.
2. **More rules where the corners do not reach**, each a safe fact about any cube:
   - a sticker's colour can only be one a real piece allows next to its known neighbours (white
     never beside yellow on one piece, a corner's colours in its clockwise order), so a sticker read
     as an impossible colour counts for its possible ones only;
   - two faces seen side by side are neighbours: their colours are adjacent, and which edge touches
     which fixes their turns (an edge view gives adjacency, a corner view also handedness);
   - each colour on nine stickers, each piece once (already in the best-cube fit);
   - the cube does not change during the scan.
3. **Pile by these rules, not by centre look first.** Today's order (pile by look → name → turn →
   fit) lets a look-alike centre decide before the rules can act. The rework: keep each reading's
   face as a short list of possible faces and turns, narrowed by every rule above, and let the
   best-cube fit choose among what is left. This is the larger part and needs its own design.
4. **The wrong cube in the robustness run** (`20261005_151828`, no corners) is the first case for
   step 2.

Kept from the spike: `CornerReader` (+ tests), the `rules` flag in `VideoScan` (off), the striped
fixture `web_181940`, and `CornerScanHarness` for measuring the follow-up.
