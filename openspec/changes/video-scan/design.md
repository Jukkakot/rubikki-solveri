# Design

## Context

The guided scan reads a fixed grid, captures a face after a hold, names the six centres together
at the end and finds the faces' rotations by search (`cube/scan`). The video scan replaces the
grid and the captures; it reuses the colour naming at the end, the rotation search as a fallback,
the colour check and the solution flow. Feasibility is measured first in `video-scan-spike`.

## Goals / Non-Goals

**Goals:** scanning without captures; a progress cube that fills sticker by sticker; visual turning
hints; the same reliability as the guided scan on the user's test videos.

**Non-Goals:** reading a cube while it is being twisted (layers turned) during the scan; other cube
sizes; removing the guided scan.

## Decisions

- **Pipeline (pure Kotlin in `cube`, one code base for Android and the browser).**
  frame (≈360 px, ≈10 fps) → `FaceFinder` (3×3 lattices, see the spike) → face readings (nine colours,
  corner positions, the lattice's rotation on screen) → `VideoScan` state: sticker votes, centre
  naming, pose → progress and hints for the screen.
- **Sticker-level model from the start, face-level input first.** The state keeps votes per sticker
  (54 slots). The first version feeds only complete lattices (all nine stickers), which is the
  reliable part; partial lattices (a finger over one sticker) can be added later without changing
  the model or the screen. This is how the user's "sticker by sticker" is reached naturally.
- **Which face is which.** Centres never move, so a reading's centre colour names the face. Colours
  are named live with the brightness-independent naming already used, and re-checked jointly once
  all six centres are seen (opposite colour pairs as a constraint).
- **Orientation of a face (where its sticker 1 is).** From corner views: two or three lattices in one
  frame share edges, which fixes how those faces sit against each other. Faces only ever seen alone
  get their rotation from the existing rotation search at the end (the real cube is solvable).
  Votes are kept per face in "reading" coordinates until the rotation is known, then mapped.
- **Pose for the progress cube and the hints.** The face towards the camera and the lattice's
  rotation on screen give the cube's pose (one of 24) once its neighbours are known; the progress
  cube turns to the same pose (smoothly), so what the user sees matches the real cube.
- **Turning hints.** From the pose and the unrecognised stickers, pick the quarter tilt (up, down,
  left, right) that brings the most unrecognised stickers into view; show it as an arrow on the
  progress cube plus one short line ("Kallista kuutiota ylöspäin"). No hint while nothing is found.
- **Recognised = agreement.** A sticker counts once its leading colour has enough votes and a clear
  margin; contradictions (a face whose colours do not fit the rest) mark those stickers instead.
- **Finishing.** All 54 recognised + a possible cube for ≈0.5 s → solution, the check behind it
  (as `scan-quick-flow`). A "Valmis / tarkista" action opens the check earlier with what is known.
- **Screen layout (user, 2026-10-05, mockups https://claude.ai/artifact/FYZ54jo3RKhDawW9XafY9r):**
  the camera picture is large; the progress cube is small in its top corner on a dark rounded
  backing (1A); the turning hint's arrow is drawn on the progress cube, not over the camera picture,
  with the short line below the camera (2A).
- **Exposure.** Locked when the first face is found (as the guided scan does).
- **Speed.** Frames are small; if the browser is too slow, the finder runs in a Web Worker
  (measured in the spike).

## Implementation decisions (autopilot)

- **Core in `cube/scan/VideoScan.kt`.** Readings are grouped by centre colour; the group's anchor is
  the reading most others agree with (≥ 7 of 9 in some rotation); only agreeing readings vote.
  Recognised: ≥ 3 votes and twice the next colour; a recognised sticker keeps its colour while it
  still leads. Disputed (≥ 3 votes, no clear lead) = a contradiction on the progress cube.
- **Rotations before every sticker is known:** a search over the unsettled faces' turns that maximises
  the real pieces among fully known pieces; a face counts as settled when every best answer agrees.
  Unsettled faces are drawn with the best guess (they may turn once more). Corner views need 2
  agreeing observations; if they make the whole cube impossible, the free search is used instead.
- **Pose** from the largest face in view whose rotation is settled (so at first it can be the top
  face of a corner view). **Hint score:** the new front's missing stickers plus a quarter of those on
  its four neighbours; no arrow when staying put scores as well.
- **Fixtures:** the finder's output for both test videos is saved in
  `cube/src/jvmTest/resources/video/` (`VideoScanHarness.writeFixtures`), so the tests run without
  the git-ignored frames. Speed: ≈ 2 ms per frame on the desktop JVM on top of the finder.

## Risks / Trade-offs

- Steep angles and glare → only full lattices count at first; many frames give many chances.
- A face shown only once, alone → its rotation comes from the search; the hint steers the user to
  corner views, which also show neighbours.
- Pose ambiguity before two faces are known → the progress cube stays still and no arrow is shown
  until the pose is known.
- Phone performance → measured in the spike on the user's videos (JVM time per frame as a proxy),
  then on the phone.

## Open Questions

- The thresholds (votes, margin): from the spike's numbers.
