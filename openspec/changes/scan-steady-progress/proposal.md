# Proposal

## Why

The user's web test on 2026-10-07 at 18:19 (`testdata/video/2026-10-07/web-181940/log.txt`, video
kept locally): a striped pattern cube that the scan read in 10 s twice took 61 s once. From 17 s
everything was right except the red face: its centre looked more orange than red in that light,
orange was taken, and the red pile stayed a doubtful "stray that waits", so its stickers were never
known and the scan could not finish. Meanwhile the ring went backwards twice (known stickers lost
when piles were renamed or faces lost their rotation), and "Värit eivät täsmää" came up. On screen
nothing showed what went wrong: since `scan-paint-calm` known stickers are left bare, and the marks
hide whenever the cube moves, which a hand-held cube nearly always does.

The user agreed to all four recommendations (2026-10-07).

## What Changes

- **The sixth colour follows a seen face too.** When the other five faces are named surely, a face
  seen several times whose centre is none of them is named with the colour left, even when its
  centre looks more like a colour already taken. It no longer waits as a stray.
- **Progress does not go backwards on a rename.** A pile renamed keeps what its stickers were read
  as; only stickers whose colour now reads differently change.
- **Read colours come back, small.** Each known sticker gets a small dot in its read colour at its
  centre; needed stickers keep the grey veil; confirmed sides keep the outline and tick. A misread
  sticker is visible again (as a dot in the wrong colour), without the busy full tiles of before.
- **Marks stay while the cube is held.** The marks fade only when the cube moves clearly fast (about
  a side width a second instead of a third), so a hand-held cube keeps its marks.
- **Test case:** the user's striped cube (`WWWWWWWWWBRGBRGBRGOGROGROGRYYYYYYYYYGOBGOBGOBRBORBORBO`)
  with a red centre that reads orange-ish, held yellow up, must finish once every face is seen.

Changes specced behaviour: `video-scan` "Colours on the camera picture" and "Progress on the real
cube" (dots on known stickers, partly reverting `scan-paint-calm`), "Paint follows the cube"
(fades only on clearly fast moves), "Recognised by agreement" (sixth follows a seen face; renames
keep stickers).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: "Recognised by agreement", "Colours on the camera picture", "Progress on the real
  cube", "Paint follows the cube".

## Impact

- Modules: `cube` (`VideoScan` naming and sticker consensus, tests), `shared` (`ScanPaint`, the
  paint layer in `VideoScanScreen`). No change in `app` code apart from the existing
  `ScanPaintTest`.
