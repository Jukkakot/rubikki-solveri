# Proposal

## Why

Two browser scans of 2026-10-11 (10:04 and 10:16) never finished: oranges on the green and blue
faces stayed named red. `scan-colour-spike` showed that the readings themselves separate. The
scan's red and orange references are what fail, because they come from the two warm centres, each
under its own face's light. One hue cut for the whole cube (idea B) finished the 10:16 scan, but it
moved every reading. That made one source finish wrong and two good recordings 20–40 % slower. The
fix belongs inside the scan's references.

## What Changes

- **Red and orange named in each face's own light.** Once a face is settled, its own warm stickers
  (both kinds, beside its centre) set the red and orange references for that face's stickers. A face
  in shade is then judged against its shade, not against a centre seen in another light.
- **Better whole-cube references.** The faces' own warm groups also give red and orange
  references for faces that show only one warm kind. Where no face shows both kinds yet, today's
  centre-based references stay.
- **Measured before it ships**, with `ColourSpikeHarness` (`COLOUR_SPIKE=1`) and the spike's go rules:
  - both stuck recordings finish
  - no source finishes with a wrong cube
  - no source finishes more than 10 % later
  
  If no variant passes, the scan stays as it is and the findings say why.
- Shared scan logic, so the phone app and the browser both get it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: *Reading in different light* gains red and orange told apart in each face's own
  light, and a scenario for a face in shade.

## Impact

`cube` only: `FaceTracks` references and naming (commonMain), `ColourSpikeHarness` and unit tests
(jvmTest). `app`, `shared` and `web` use the shared scan unchanged. A phone check under the
2026-10-11 room light is listed for the user.
