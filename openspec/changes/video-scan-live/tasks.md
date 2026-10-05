# Tasks

## 1. Partial faces (cube)

- [ ] 1.1 `FaceReading` with missing stickers (null colours); `VideoScan` takes partial faces: grouped by centre (none → dropped), turn found on the present stickers, votes only when all but at most one agree with the group, never an anchor. Verify: unit tests (a partial face votes for its eight stickers; one with a wrong lattice does not vote; no centre → ignored).
- [ ] 1.2 Harness writes partial faces into the fixtures; replay tests feed them. Verify: both videos still give the true cube, no recognised sticker changes colour, and the replay prints frames-to-complete with and without partials (expected: fewer with).

## 2. Spike: smaller pieces (cube)

- [ ] 2.1 Lower the finder's hit floor for lattices that include the centre (4–6 stickers) behind a setting; measure on both videos with the replay: final cube, wrong recognised stickers at any frame, frames to complete, stickers recognised at the halfway frame. Write `findings.md` in this change.
- [ ] 2.2 Apply the criterion in `design.md`: go → feed the pieces like partial faces, fixtures and replay tests updated; no-go → floor stays at 7, setting removed. Verify: replay tests green either way.

## 3. Orientation and faint stickers (cube)

- [ ] 3.1 Orientation from one face's steps (weak perspective, sign from the face looking at the camera), from the largest found face with a settled rotation; `VideoScanState.orientation`. Verify: unit tests with synthetic projections of known rotations (straight, rolled 30°, tilted 25°) give them back within a few degrees; null without a usable face.
- [ ] 3.2 `VideoScanState`: leading colour per unrecognised sticker; per found face the colour named in this frame and whether each sticker is recognised. Verify: unit tests (one reading → leading colours, none recognised; three agreeing → recognised flags true).

## 4. Screen (shared)

- [ ] 4.1 Progress cube eases towards `orientation` at display rate (shortest way, ~150 ms), stays put without one; faint leading colours (about one third over grey). Verify: Compose test that the progress cube renders with a partly known state; easing function unit-tested (moves towards the target, never overshoots).
- [ ] 4.2 Camera overlay: a round dot per sticker of each found face in its named colour, solid when recognised and faint when not, thin dark rim; thinner, dimmed cyan outline. Verify: the existing screen smoke test still passes with faces carrying colours.

## 5. Docs

- [ ] 5.1 Update the docs page that describes the video scan (`docs/architecture.md`) where the map changes; mark `video-scan-live` done in `openspec/context/roadmap.md` (new row 41).
