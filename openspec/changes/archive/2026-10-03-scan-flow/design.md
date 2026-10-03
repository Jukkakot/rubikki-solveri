## Context

See proposal.md for why. Today `ScanSession` classifies each frame live against references (default
palette + accepted centres + user corrections), refuses a face whose centre does not match
(`WrongFace`), counts a face as steady when the classified colours stay identical, and lets the user
cycle sticker colours in the review. The final `ColorClassifier.classify` (balanced assignment
seeded by the six centres) is already the reliable step; the live guesses are what failed on the
phone. A partial implementation of this design is in the working tree (session and its tests
green; the scan screen lost its `Progress` and `PermissionScaffold` functions and must get them back
from `HEAD`).

## Goals / Non-Goals

**Goals:** never block a correct face because of a colour guess; keep one cheap guard against
capturing the same face twice; show the user what the camera actually sees.

**Non-Goals:** better red/orange separation in the final classification (it already works from the
whole cube; tune later if phone tests show otherwise); changes to the manual editor or the result
flow; the exposure/white-balance lock (unchanged).

## Decisions

- **Order is trusted, centre is a hint.** The face asked for is the face captured. A centre that
  live-reads as another colour is reported in `Holding.centreLooksLike` and in the review, but does
  not stop the capture. Alternative: keep the stop with a looser threshold — rejected, any
  threshold fails on some cube/light pair, and the user holding a face for 1.5 s is a strong signal.
- **"Previous face still in view" is the only stop.** Compared in Lab against the last accepted
  face's captured samples: every cell within `STEADY_DISTANCE` → `PreviousFace`. This catches the
  common slip (accepting, then not turning) without any colour naming. Only the last accepted face
  is compared; older faces coming back is rare and would be caught as invalid at the end.
- **Steadiness by colour distance.** Every cell within `STEADY_DISTANCE` (ΔE 12) of the streak's
  first frame, instead of identical classified colours. Classified colours flicker at red/orange
  boundaries even when the cube is still; distance does not. 12 is generous for camera noise and
  well below the distance between two different stickers. Phone check may tune it.
- **Raw colours in the UI.** Live dots and review tiles draw the sampled RGB. Named colours would
  repeat the guess we no longer trust; raw colours let the user judge glare or shadow.
- **Tap-to-fix removed, not moved.** Corrections taught the live reading wrong lessons and were
  hard to aim. Doubtful stickers are already marked in the manual editor after the scan, which is
  the place to fix them with the whole cube in view. `classify` keeps its `fixed` parameter (used
  by nothing now) only if removing it costs more than it saves; it may go.
- **Accepted centres still teach the centre hint.** References = accepted centres + default
  palette, so the hint gets better as the scan goes (warm red reads as red after the right face).
- **Diagnostics:** the per-face log line drops `live`/`fixed` and adds `centreLooksLike`, so phone
  logs show when the hint fired.
- **Wording:** "Good, next" / "Hyvä, seuraava" and "Scan again" / "Kuvaa uudelleen" (not
  "skannaa": plainer Finnish).

## Risks / Trade-offs

- [User shows the wrong face and ignores the hint] → the final check finds the cube invalid and the
  manual editor opens with problems marked; redo is one tap away during the scan.
- [Two faces that look alike (e.g. solved cube turned 180° on a near-uniform face) trigger
  `PreviousFace`] → only possible with the same centre colour, which a correct next face never has.
- [`STEADY_DISTANCE` too tight in poor light] → progress keeps restarting; capture button remains,
  and the constant is one place to tune.
