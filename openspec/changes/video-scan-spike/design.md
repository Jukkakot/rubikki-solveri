# Design

## Context

The scan today samples a fixed 3×3 grid in the middle of the camera picture (`FrameSampler`) and
checks for the dark gaps between stickers. The colours are named at the end from all six faces
together (`ScanSession`, `ColorClassifier`), and the faces' rotations are found by search
(`RotationSearch`). Those later steps can stay; what is new is finding faces without the grid.

## Goals / Non-Goals

**Goals:** measure, on the two test videos, how often a face is found, how many stickers are read
right, and whether the whole cube can be assembled; leave reusable finder code.

**Non-Goals:** the live feature, its screens, the progress indicator, speed tuning for phones
beyond keeping frames small (about 360 px wide).

## Decisions

- **Find stickers, then lattices (no OpenCV, no ML model).** Stickers are bright or saturated
  rounded squares separated by dark plastic. Mark sticker-like pixels, take connected blobs of a
  plausible size and squareness, then look for nine blobs whose centres fit a 3×3 lattice: pick a
  blob and two neighbours as the lattice's two step vectors, predict the nine positions, count the
  blobs that land there (RANSAC-style), keep lattices that fit all nine. A small perspective
  correction comes from fitting the lattice to the nine centres. Pure Kotlin keeps one code base
  for Android and the browser and runs in JVM tests. Alternatives: OpenCV (two platform builds,
  large) or a trained model (needs data and a runtime on both platforms); kept as fallbacks if the
  numbers are poor.
- **Several faces per frame.** A corner view shows up to three lattices; each is read on its own.
  Which faces are neighbours is noted for assembly, but the first assembly only needs one good
  reading of each of the six faces.
- **Assembly through the existing end of the scan.** The best reading per centre colour (most
  frames agreeing) goes into the same naming and rotation search as a normal scan, so the result
  is comparable with the true state.
- **Frames from ffmpeg, PNG, read with ImageIO in the JVM test.** The finder itself takes an ARGB
  `IntArray`, like the rest of the scan code.

## Risks / Trade-offs

- Fingers and glare break lattices → only lattices with all nine stickers count; many frames give
  many chances.
- White and yellow stickers look alike at an angle → the joint naming at the end already handles
  this for normal scans.
- Lattices at steep angles distort → measured separately for the angled video; the go / no-go may
  limit the first live version to near-straight views.

## Open Questions

- What hit rate is "good enough" is judged from the numbers with the user (proposal in
  `findings.md`).
