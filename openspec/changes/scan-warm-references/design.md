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

**Decision 1 (replays with the references in the timeline, `ScanTimeline` prints `warmRefLog`):**

- `100403`: the orange reference came from a wrong face. From about -7.4 s on it was `c19d6f` (beige), the
  centre of a 3-reading track settled as L that is really the yellow D face. Against it every orange
  sticker lay nearer red. Fixed: a red or orange reference only from a centre that looks red or orange
  itself (`FaceTracks.mayBeReference`). The replay now finishes with the true cube (test
  `ScanWarmReferencesTest`), the orange reference `dd4211` from three real orange tracks.
- `101618`: the sources are right (after two short wrong red tracks early on), but the red reference
  is a very dark `8e0104` and the orange a bright `ff9b55`: the two faces were seen in different light.
  This is the case for decision 2.
- The scan budget test (`aLongScanStaysWithinItsBudgetPerPicture`) fails on this cloud machine without the change
  too (a window at 5.0–5.2 ms) and passes or fails from run to run with it (5.0–6.7 ms); CI triples the budget.

**Decision 2 as built:** first split by Lab hue (as `WARM_HUE_STEP`): in the 10:16 room red and orange lie only
3–4° apart there, and washed-out or greyish readings (a dim green, white) joined the warm ones and took the
largest gap. Now: HSV hue (red −13..4°, orange 6..18° on both stuck recordings), washed-out readings and
chroma under `MIN_COLOUR_CHROMA` left out, hues −30..45°, gap `FACE_WARM_GAP` 6°, orange side's mean hue
above 0°. With that every split in the two replays matched the true colours.

**Comparison** (`ColourSpikeHarness`, finish time in s; W = wrong cube; CENTRES = centres with the decision 1 fix):

| Source | before | CENTRES | CUBE | FACE | FACE_ONLY |
|---|---|---|---|---|---|
| 213729 (evening) | 12.2 | 12.2 | 9.1 | 9.1 | 12.2 |
| 202058 (striped, dim) | never | never | never | never | 26.4 W |
| 202156 (striped, table) | 18.0 W | 18.0 W | 20.5 | 20.5 | 20.5 W |
| 202403 (striped U2, dim) | never | never | 13.3 | 14.1 | 12.3 |
| web_121505:66 | never | never | 19.9 W | never | never |
| rec 100814 | 6.2 | 6.2 | 5.2 | 5.2 | 5.2 |
| rec 100824 | 9.2 | 9.2 | 20.0 | 13.3 | 13.3 |
| rec 102548 | 7.8 | 7.8 | 17.1 | 17.1 | 17.1 |
| rec 100403 (stuck) | never | 15.3 | 11.1 | never | never |
| rec 101618 (stuck) | never | never | 12.4 | 12.4 | never |

Every other source finishes the same in all modes. FACE_ONLY: face references without the whole-cube ones.
Scan time a picture stays within about ±1 ms of CENTRES in every mode.

**Outcome: no split mode passes the go rules.** CUBE finishes both stuck recordings and two striped videos but
makes `web_121505:66` finish wrong and two good recordings about twice as slow; FACE and FACE_ONLY keep the
slowdowns and lose `100403`. The default stays CENTRES (with the decision 1 fix: `100403` finishes, nothing
worse). Reported to the user (design 5) before going on.
