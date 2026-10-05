# Tasks

Built only after `video-scan-spike` says "go"; thresholds and limits come from its findings.

## 1. Recognition core (`cube`)

- [ ] 1.1 `VideoScan` state: sticker votes (54 slots, per face in reading coordinates until the face's rotation is known), live centre naming, contradictions. Verify: JVM tests on face readings from the test videos (the harness output saved as fixtures): one wrong reading does not change a recognised sticker.
- [ ] 1.2 Face rotations from corner views (shared edges between lattices in one frame), the rotation search as fallback; pose tracking (one of 24). Verify: on both test videos the assembled cube equals the true state; the pose sequence is plausible on a few hand-checked frames.
- [ ] 1.3 Turning hint: the quarter tilt that shows the most unrecognised stickers from the current pose. Verify: unit tests (only the bottom missing → tilt so the bottom shows).

## 2. Video scan screen (`shared`)

- [ ] 2.1 Screen: full camera picture, found faces outlined, the progress cube (grey, filling, following the pose, contradictions marked), hint arrow and line, vibration on new stickers; exposure lock on the first face. Verify: Compose smoke test with a fake frame source fed from saved readings; screenshot for the gallery.
- [ ] 2.2 Finishing: all recognised and possible for 0.5 s → solution with the check behind it; "stop" → the check with the missing stickers marked. Verify: test with fake readings.
- [ ] 2.3 Scan choice: the guided scan default, the video scan offered beside it (switch the default later in a small change). Verify: smoke test that both open.

## 3. Platforms and speed

- [ ] 3.1 Frames at ≈10 fps from the Android camera and the browser camera into the finder; Web Worker in the browser if the spike's timing says so. Verify: web build and smoke test; timing logged.

## 4. Docs and roadmap

- [ ] 4.1 Roadmap and `docs/` (scan section: the two ways, where the finder lives). Verify: roadmap row present.
