# Design

## Decisions

- **References = mean of what this cube has shown.** For each colour: the readings of accepted
  centres and of user corrections; colours not seen yet fall back to the default palette. Accepted
  but uncorrected stickers are not used, so a wrong reading the user missed does not train the
  reference. The final classification is unchanged in spirit (all six centres, balanced 9 per
  colour), with corrections seeding the references and pinned like centres.
- **Tap cycles by closeness.** A tap moves the sticker to the next colour in the order of distance
  to the references (for a misread red that is almost always orange → red in one tap). Cycling
  back to the reading removes the correction. The centre tile is fixed. No colour picker: one tap
  fixes the common case, and the manual editor at the end remains for anything else.
- **Corrections are pinned in the result** (`classify(fixed = …)`). The user saw the sticker and
  chose; the balanced assignment works around them.
- **Lenient centre check.** The expected centre also passes when it is second closest after a
  colour with no reference from this cube yet. Before the red centre has been seen, a warm-lit red
  may read as the default orange; requiring an exact match made such cubes unscannable. The cost:
  while orange is unknown, showing the orange face when red is asked passes too; the review shows
  the colours and the final validity check catches a swapped face.
- **AE/AWB lock after the first accepted face**, released if the user redoes back to the first
  face. Camera2 interop on the controller's `CameraControl`; a failure is logged and the scan goes
  on unlocked. Locking at once (before face 1) was rejected: the first frames are often still
  adjusting when the scan opens.
- **Lightness weighting is already 0.5** in `Lab.distance`; it is not changed without real data.
  The next tuning step uses the logged readings (`scan.face rgb=… live=… fixed=…`).

## Risks

- The exposure lock depends on the phone's Camera2 support; on the S24 it is standard. Needs a
  phone check: colours should look the same on all faces after the first.
