# Design

## Context

`VideoScan(engine)` runs either the rules scanner (`rulesFrame`, `FaceTracks`) or the earlier
look scanner (piles, `nameJointly`, `consensus`, about lines 111–833 of `VideoScan.kt`). Both feed
the shared `finish`, `holdProjection`, `stall`, pose and outcome code. The engine choice runs
through Settings (Android DataStore `scan_engine`, browser `StoredSettings.scanEngine`), the nav
host, `VideoScanScreen`, `ScanLogger` (`engine=` on every line) and the browser worker's
`reset:<engine>` / `adopt:<resets>:<engine>` messages. Tests use an open base class with a LOOK
default (`VideoScanTest`, `ScanPaintTest`) and a RULES subclass. `ScanAcceptanceHarness` holds the
rules scanner within 1.2× of the look scanner's finish frame.

`ScanPaint.of` draws veils on every face in `state.found`. `rulesFound` returns a face with no
track, or a track that has only this picture's reading, with all nine stickers unrecognised, so it
is fully veiled. These are the ghost tiles in the 2026-10-09 recording.

## Goals / Non-Goals

**Goals:**
- One video scanner. The look path, its setting, its strings and its tests are removed.
- No veils or marks on a lattice that no earlier picture followed.

**Non-Goals:**
- Changing how the rules scanner reads, decides or finishes. The fixtures' results stay the same.
- Renaming shared constants with "look" in their name (`MARGIN_LOOK`). They are used by the rules
  path and the log, and renaming them gains nothing.
- Hiding the marks on a fast-moving cube in the read-picture mode. The followed rule removes the
  ghosts without hiding real faces.

## Decisions

1. **"Followed" = the face's track has at least two readings.** A face found in this picture is
   followed when it joined a track that an earlier picture already had (track size ≥ 2 after this
   reading). `FoundFace` gets `followed: Boolean`. `ScanPaint.of` skips faces that are not
   followed. Their dim outline was already only drawn once read, which needs `MIN_READINGS`.
   The alternative was a motion gate on the read picture. Rejected because it also hides real,
   well-found faces while the hand moves, which `scan-paint-steady` worked to avoid.
2. **Cost of the rule:** a new face's veils appear one picture later (about 35–60 ms at 17–30
   pictures a second). The eye does not notice that.
3. **Stored choice left alone.** The Android DataStore key and the browser's stored field are no
   longer read. Old browser JSON with `scanEngine` still parses, because unknown keys are ignored
   (checked in `BrowserStoresTest`). No migration step.
4. **Log:** the `engine=` field is dropped from scan log lines and from `SETTINGS_CHANGED`. With
   one scanner it says nothing. Old logs still read fine.
5. **Worker protocol:** `reset` and `adopt:<resets>` without the engine part. Page and worker are
   built and deployed together, so there is no version skew to handle.
6. **Acceptance harness:** the look-vs-rules comparison becomes a stored bar per fixture: each
   fixture's current rules finish frame × 1.2, same slack as before. It still runs only with its
   env flag.
7. **Tests:** `VideoScanTest` and `RulesVideoScanTest` merge into one RULES-only class.
   LOOK-only cases (`MAX_READINGS`, the engine branches) go. `ScanPaintTest` and
   `RulesScanPaintTest` merge the same way. A new paint test: a face whose track has one reading
   gets no tiles or dots; the same face in the next picture does.

## Risks / Trade-offs

- A face that the tracker loses and starts as a new track in every picture would never show marks.
  Tracks follow a face even through a turn, and the fixtures show long tracks. Check the per-picture
  share of not-followed faces on the fixtures while implementing. If it is large, the rule is wrong.
- Removing about 700 lines from `VideoScan.kt` risks deleting something shared. The agent's map
  of what is shared (`finish`, `holdProjection`, `stall`, `Held`, `Tone`, `neighbourAt`,
  `MIN_VOTES`, `MARGIN`, `WASHED_*`) is the checklist. The fixture tests must give the same
  finishes before and after.
