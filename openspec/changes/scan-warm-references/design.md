# Design

## Context

See proposal.md – Why, and `archive/2026-10-11-scan-colour-spike/findings.md`.

How the scan names stickers today (`FaceTracks`):
- Every sticker reading is named against one set of references for the whole cube (`refs`, from
  `balancedRefs`). A reference is each settled face's centre, averaged over its readings (a `Tone`,
  plain and brightness-free Lab blended).
- When the references move by more than `REF_STEP`, every reading is named again.
- `warmOrderCost` already keeps a clearly more orange centre off the red face.

The spike gave two facts:
- On `100403` the true centres as references leave only 2.2 % of warm stickers wrong, yet the scan
  named F's and B's oranges red. The scan's own references differ from those true-centre ones.
- On `101618` even the true centres leave 15.3 % wrong. There the light differs between faces.

## Goals / Non-Goals

**Goals:** both stuck recordings finish, with the spike's go rules held on every source.

**Non-goals:** reading the colours themselves again (idea A, dropped), the finder, the face
assignment rules, thresholds outside the warm references.

## Decisions

1. **Diagnose first, on the replay.** Replay both stuck recordings and log the scan's red and orange
   references over time:
   - which tracks they come from, and whether a warm reference ever came from a wrong face
   - the `cast` fallback while only one warm centre is known
   - the distance to the true-centre references
   
   The result goes into this design's Findings. If `100403`'s fault turns out to be a wrong source
   (a misassigned track, the cast), fix that first, then measure again before decision 2.
   *Why:* the spike explains `100403` only partly, and a fix aimed at shade could miss it.

2. **Per-face warm references (the main variant).** For each settled face:
   - Its warm positions are those whose readings, averaged over the track, look warm (chroma above
     the colour threshold, hue in the spike's warm range of −30°..45°). The centre counts when it is
     warm.
   - The face splits them in two by the largest hue gap. The split holds when the gap is at least
     `WARM_HUE_STEP` and each side has at least one position. The lower side is red, the higher
     orange, and each side's mean `Tone` becomes that face's red and orange reference.
   - A face whose centre is red or orange anchors its own side: a single group near its centre's hue
     is the centre's colour.
   - A face without a split uses the whole-cube references (decision 3).
   
   Naming a reading then takes its track's face references for red and orange and the whole-cube
   ones for the other four colours. Positions, not single readings, make the groups, so a face
   gives at most 9 values and the split costs next to nothing.
   *Alternative:* shift the cube's red and orange by each face's light, measured on its centre
   against that colour's reference. That needs a coloured centre to measure light from, and white
   and yellow centres carry almost no hue, so it fails on the faces that matter.

3. **Whole-cube red and orange from the faces' groups.** Once at least one face holds a split, the
   cube's red and orange references are the means of all faces' red sides and orange sides, each face
   weighted the same. The warm centres stay part of their face's group. With no split anywhere,
   `balancedRefs` stays as today (scenario "Red and orange alike on every face").
   *Alternative:* idea B's single hue cut, which the spike ruled out.

4. **When references change.** The face references are worked out again only when a face settles or
   re-opens, or a face's warm position means move by more than `REF_STEP`. Renaming touches only the
   tracks whose references moved. This keeps `scanMs` within the 5 ms per 100-picture budget test.

5. **A switch for the comparison.** `FaceTracks` takes an internal `warmRefs` mode: today, per-face
   (decisions 2+3), or whole-cube only (decision 3 without per-face naming). `ColourSpikeHarness`
   runs every source with each mode and prints the spike's tables plus `scanMs`. The default becomes
   the mode that passes the go rules: per-face preferred, whole-cube second. If neither passes, the
   default stays today's, the losing code goes, and the harness and Findings stay as the record.
   That case is a stop to report to the user, not a decision to take alone.

6. **Go rules** (the spike's design 4):
   - both stuck recordings finish (`100403` and `101618`) with the true cube
   - no source finishes with a wrong cube where today's did not
   - no source finishes more than 10 % later
   
   Plus: today's unit tests and fixtures pass unchanged.

## Risks / Trade-offs

- [A face shows only one red and one orange sticker, and the split catches a misread or a sticker
  half in shade] → `WARM_HUE_STEP` gap and the averaging over the track's readings; the cube's rules
  still decide, and the go rules check every source for a wrong cube.
- [Two stuck recordings are one cube in one room] → every other recording and still is in the
  harness too; the phone check under the same light is listed for the user.
- [A per-face reference makes a face's stickers disagree with the same piece's other sticker on a
  neighbouring face] → the piece rules already weigh both; that is the point (each in its own light).
- [Striped-cube sources whose faces fit several turns] → judged on the scan columns only, as in the
  spike.

## Migration Plan

None: scan logic only, nothing stored. Rolling back means setting the mode back to today's.

## Findings

(Filled in during apply: decision 1's diagnosis, the comparison table, the chosen mode.)
