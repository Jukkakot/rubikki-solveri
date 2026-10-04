# Design

## Context

See proposal.md. The scan reads a 120×120 grid picture per frame (`FrameSampler.picture`) and the
nine cell readings (`sample`). `looksLikeCube(picture)` decides today from gap contrast only.
Measured on the user's ten shared pictures (2026-10-04): real sticker cells have a middle spread
(median ΔE from the middle's median Lab) of at most 2.3; coloured stickers have chroma ≥ 41
(dark reds included, L 22–30); white stickers have L ≥ 64 and chroma ≤ 13 in daylight, and in the
earlier evening logs read as low as L ≈ 58 (`838e91`). Every rejected picture has at least one cell
that is uneven (spread 6–35), too dark (L < 20), or grey-beige (chroma 14–25, or near-neutral and
not light).

## Decisions

### 1. Per-cell sticker check (`FrameSampler.stickerCells(picture): List<Boolean>`)

For each cell's middle (the same 40 % middle as `gapContrast`): median Lab and spread. A cell is a
sticker when:
- spread ≤ 4.0 (`MAX_STICKER_SPREAD`), and
- either chroma ≥ 30 and L ≥ 15 (a coloured sticker), or chroma ≤ 18 and L ≥ 50 (white).

`looksLikeCube` = all nine cells are stickers and the gap check passes. Thresholds are constants in
`FrameSampler`, chosen with margin from the numbers above, tuned after the user's next try.
Gap 18–30 chroma with low L (beige, brown, olive) fails both branches on purpose. Performance: the
middle pixels are already read for `gapContrast`; one pass computes both (primitive arrays, the
existing lightness table plus a Lab conversion per middle pixel; target under 2 ms per 120×120
picture on the JVM, checked in a test).

### 2. Feedback

`ScanScreen` keeps the latest `stickerCells` with the picture (replacing the `Boolean`) and draws
each grid cell's outline green when it is a sticker and white otherwise (today's look). The green
is a fixed colour (`0xFF4CAF50`), not a theme colour, because it is drawn on the camera image in
both themes. Status text while any cell fails: `scan_status_no_cube`
reworded to "Tuo kuutio ruudukkoon: jokainen ruutu yhdelle tarralle." / "Bring the cube into the
grid: one sticker per square." No picture yet → not a cube (the hold progress waits).

### 3. Camera follow

`CameraPreview` already delivers pictures through `onPicture` (used by the scan). `DefaultFollowPanel`
passes them on; `FollowPanel` gets a `Flow<Pair<List<Rgb>, Boolean>>` (samples and whether the grid
shows a face). A frame without a face: `FollowTracker` is not called, no learning, no advance; the
message is the new `follow_bring_cube` ("Tuo kuutio ruudukkoon" / "Bring the cube into the grid").
The live dots still show. Tests pass `true`.

## Risks / Trade-offs

- [Glare on one sticker makes it uneven, so the auto capture waits] → the capture button still
  works; spread limit tuned from the next logs.
- [White in very dim light falls under L 50] → the evening logs had whites at L ≈ 58; the button is
  the fallback. Logged readings (`scan.capture rgb=…`) let us tune it.
- [Thresholds from one cube and one phone] → constants in one place, the next shared log checks them.
