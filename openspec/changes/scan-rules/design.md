# Design

## Context

See proposal.md (Why) and `archive/2026-10-07-corner-scan-spike/findings.md` (numbers).

Today, per frame (`VideoScan.onFrame`): the finder's faces are **piled by centre look**
(`pileFaces`, `mergeClosePiles`), the piles **named** by centre colour (`nameJointly`), each pile's
readings **voted** into nine stickers in the pile's own frame (`consensus`), the piles **turned** into
the net from neighbour views (`updateRotations`), and the votes go into `BestCube`, which finds the
cheapest real cube (8 × 8 corner and 12 × 12 edge assignments with twist, flip and parity) and how
much costlier any other piece would be at each place (`supportedMargin`). The last step already is
the user's "possible cubes, crossed out by observations", in soft form. The first three steps are
where it goes wrong: a look-alike centre decides the pile before any rule is consulted.

Kept as they are: the finder, `FaceReading`, `CornerReader`, `BestCube`, the state the screen reads
(`VideoScanState`: stickers, leading, found faces, projection, pose, confirmed, stall), exposure and
stall logic, the paint.

## Goals / Non-Goals

**Goals:** faces known by rules, never by look alone; never a wrong cube on any fixture, also with
look-alike centres; finishing at least as often and about as fast as today; same screen.

**Non-Goals:** a new finder or better lattices (a slipped lattice stays a bad reading, outvoted);
guidance or colour questions (ruled out by `product.md`); changes in `shared` beyond the one status
text of decision 5.

## Decisions

### 1. The possible cubes are kept per piece place (BestCube stays the engine)

Every cube is a choice of piece and turn at each of the 8 corner and 12 edge places, with each piece
once and the twist, flip and parity of a real cube. "Crossing out" is soft: each (place, piece,
turn) carries the cost of the readings against it (`StickerEvidence`), and an option is out when the
cheapest real cube using it costs more than `CLEAR_MARGIN` above the best. That is exactly
`BestCube.solve` + `supportedMargin`, so it is reused, not rebuilt. A sticker is known when every
cube within the margin agrees on it (as now). Hard crossing-out was rejected: one misread would
remove the true cube for good.

### 2. Readings are followed as tracks, not piled by look

A **track** is one physical face followed from picture to picture: a full reading continues a track
of the last ~300 ms when its centre is within about one sticker step of where the track's last
lattice was (moved with the picture's other tracks), its step vectors are of similar length and
direction (in-plane turn within ±45°, carried as the track's turn), and at least six of its eight
outer stickers agree in that turn. Otherwise it starts a new track. A track is safe grouping (the
cube does not jump); joining tracks seen at different times is left to decision 4. Partial readings
(7–8 stickers) continue a track only. Today's piles by look go away; the centre's look becomes
evidence (decision 3).

### 3. Each track's face and turn is a hypothesis with a cost

Each track has 24 options (face × turn). An option's cost adds:

- **content:** the track's votes (each sticker's colour shares, as today) against the best cube of
  the previous frame at that face and turn (the cost `StickerEvidence` gives); a track whose stickers
  fit no option well stays cheap nowhere and waits;
- **centre look (soft):** the centre's distance to that face's colour, relative to the cube's own
  centres as learned from settled tracks (`centreDistances` with known centres), with a small weight,
  so look alone never decides against the rules;
- **one-picture rules (hard):** for every pair of tracks found in one picture, the two must be
  different faces; when the picture shows them touching (`sideTowards`, or the common corner of
  `CornerReader` for a three-face view, where the top face's neighbours lie diagonally), their faces
  must be neighbours on the net across exactly the touching sides, which fixes both turns and with
  three faces the corner's handedness. Opposite colours side by side are thereby impossible. These
  pair rules are stored per pair of tracks and apply to every later assignment.

### 4. The joint assignment, solved each frame

Tracks are either **settled** (one option beats every other by `ASSIGN_MARGIN` given the rules and
the other settled tracks) or **open**. Settled tracks vote into the net evidence with their face and
turn. Open tracks with at least `MIN_VOTES` readings (at most the eight most-read; the rest wait) are
assigned jointly: a branch-and-bound over their options, most-read first, pruned by the hard pair
rules (also against settled tracks), minimising the summed cost. Several tracks may take the same
face (the same face seen at different times); tracks with a pair rule between them never do. The
best assignment's votes join the evidence; `BestCube` then gives the cube and the per-place margins;
the next frame's content costs use this cube (one alternation per frame, so the cube and the face
hypotheses settle together). A settled track re-opens when its option stops beating the next by the
margin (recent readings count over old ones, as today), so an early wrong settling is undone.

The pile-level refinements of today (sticky colours, waiting piles, clash and stray rules,
`rules` flag) are subsumed and removed with the old path.

### 5. Finish and known stickers

Finish (spec "Finish the video scan"): `BestCube` clearness ≥ `CLEAR_MARGIN` **and** the joint
assignment is clear: the cheapest assignment in which any open-or-settled track with readings takes
another option costs at least `ASSIGN_MARGIN` more; both hold for about half a second. Known
stickers: from the best cube where its margin is clear and the stickers' faces come from settled or
clearly assigned tracks; otherwise the leading vote, as today.

**Turn-the-cube hint** (user, 2026-10-07): while the cube is clear except that two faces could be
told apart either way (the best and the next joint assignment within `ASSIGN_MARGIN`, swapping
faces) for about two seconds, `VideoScanState` says so and the status line reads "Käännä kuutiota" /
"Turn the cube": a description of what helps, not a colour question or a step-by-step guide. It
goes as soon as the assignment is clear; a stall notice still takes its place.

### 6. Built beside, both kept

The track solver lives in its own file (`cube/scan/FaceTracks.kt` or similar) and `VideoScan` gets a
mode (old path / rules path) keeping one state assembly (projection, pose, stall, exposure hooks).
The acceptance harness (grown from `CornerScanHarness`) replays all fixtures in both modes and in the
robustness variant, and keeps doing so. **Both paths stay** (user, 2026-10-07): Settings offers the
scanner choice (stored with the other settings), the rules path becomes the default when it meets
the bar below, and every scan log line carries `engine=rules|look`. The spike's `rules` flag (corner
votes added to the old path) goes, since the rules path supersedes it; the old path otherwise stays
as it is.

**Acceptance bar** (confirmed by the user, 2026-10-07): on every fixture, as recorded and robustness, the
rules path never finishes wrong; as recorded it finishes on at least the 8 fixtures today finishes,
each within 20 % more frames; in the robustness run it finishes on at least 6 of the 11; one frame's
solve averages under 10 ms on the JVM over all fixtures.

### 7. Performance

24 options per open track, pair rules prune most branches, at most eight open tracks; `BestCube`
already runs every frame. Settled tracks are merged into per-face vote tables so the work does not
grow with the scan's length. Measured per frame in the harness (decision 6 bar); the browser runs
the scan in the Web Worker as today.

### 8. Decisions taken while implementing (task 5.2, measured on the fixtures)

Code: `Tracks.kt` (tracks), `PairRules.kt` (rules of one picture), `FaceTracks.kt` (costs, joint
assignment, settling), `VideoScan.rulesFrame`. Tuning tool: `RulesTimeline` (gated JVM test).

- **Turns of faces seen alone come from the best cube**, as in the earlier scanner: a face's turn
  settles by the joint assignment only when a picture binds it (a side or corner rule) or a track of
  the same face already has a picture-settled turn; otherwise all its tracks turn together the way
  that makes the best cube cheapest, and these cube turns are worked out again as the readings change
  (an early turn read in bad light is undone). One-colour faces (white, yellow on the striped cube)
  settle in any turn: turns that read alike are no alternative, and their rules allow every such turn.
- **The finish also checks cube turns in pairs**: two faces whose turns came from the cube, turned
  together, must make the best cube at least `TURN_MARGIN` costlier (the striped cube with front and
  back both half round is another possible cube; it finished wrong once in the robustness run before).
- **Evidence per face comes from the tracks agreeing with its most supported track** (at most two
  stickers otherwise, red for orange not counted; a track in view counts double): a face read wrong at
  first or a stray lattice taken for a face does not spoil it (the earlier scanner's anchor).
  Two tracks taken for one face that disagree on more stickers pay `CLASH_COST` each in the
  assignment; two face-only tracks of one face that disagree in every turn re-open.
- **Red and orange**: content costs lend each other half; references for them come in pairs (with
  one known, the other lies from it as far as in the default palette; an orange centre looking red
  made orange stickers read red otherwise). Moving all unknown colours by the known centres' cast was
  tried and dropped (a washed-out yellow centre pulled the rest astray). References are worked out
  again in the frame a face settles, before stickers are shown.
- **What is shown**: a sticker's own votes are shown on a settled face only where the best cube is
  not clearly of another mind; a short track (under ten readings) that ended unsettled neither votes
  (unless it is a face's only view) nor holds the finish; a short track whose face is settled and that
  reads like the clear cube does not hold the finish either. The drawing follows the largest face found
  by its look when its track is not settled yet (drawing only, no knowledge).
- Constants as tuned: `ASSIGN_MARGIN` 2.5, `LOOK_WEIGHT` 0.3 (cap 40 Lab), `NONE_COST` 6,
  `FACE_KEEP` 2, `CLASH_COST` 4, `MIN_READINGS` 3; a face shown alone shows its stickers after about
  five readings (the earlier scanner: three).

**Bar (design 6), measured 2026-10-07 evening:** on the eleven confirmed fixtures the rules scanner
never finishes wrong, finishes as recorded on all eight the earlier one does within +20 % frames, in
the robustness run on 9 of 11 (earlier: 1), 2–9 ms a frame. Four evening recordings added the same day
(striped cube, dim light): never wrong; it finishes two the earlier scanner never finished
(`202156`, and the web recording of the striped cube) and two it did not: `202058` (dim, the striped
cube's red and orange alike and the orange face never shown, so the right face turns cannot be told)
and `202403` (front and back half round fit nearly as well: the earlier scanner finished on what was
in effect a guess). The harness checks the bar on the confirmed eleven and reports the others.

### Known limits

- A washed-out yellow centre that looks whiter than the white face, shown alone before the white face,
  is taken for the white face until views of neighbours tell them apart (it never finishes so); its
  stickers are shown on the white side meanwhile, as the earlier scanner shows a look-named face.
- Sides of a cube with one-colour top and bottom (the striped cube) seen only one at a time, with red
  and orange alike in the light, do not finish: the scanner waits and asks to turn the cube.

## Risks / Trade-offs

- [The alternation locks into a wrong cube early (wrong settling feeds the content costs)] → settle
  only with a margin given the hard rules, re-open when the margin goes, and the finish also needs
  the assignment margin; the robustness fixtures test exactly this.
- [Tracking breaks on fast moves, many short tracks] → short tracks wait (fewer than `MIN_VOTES`);
  the solver joins tracks by assigning them the same face.
- [Two faces that look the same and are never seen with a common neighbour] → the scan does not
  finish until a view settles it (spec scenario "Two ways to tell the faces"); slower, never wrong.
- [The robustness wrong cube of `20261005_151828` has no corner views] → a task studies it first; if
  no safe rule reaches it, the bar is discussed with the user rather than the cube accepted.
- [Bigger rewrite of `VideoScan` internals] → the old path stays until the bar is met; existing
  `VideoScanTest` scenarios run against both modes.

## Migration Plan

Mode flag, default old → harness → default rules (the phone and the web get it in the same push);
the old path stays choosable in Settings. Rollback: flip the default back.

## Open Questions

None. Answered 2026-10-07: the acceptance bar as in decision 6 (if no safe rule removes the
robustness wrong cube of `20261005_151828`, stop and ask); the turn-the-cube hint (decision 5); new
phone recordings (striped cube, dim light) come later from the user and are added to the fixtures
then; until then the current fixtures and synthetic tests are the measure.
