# Findings: reading in different light

## 1.1 Red and orange in the test videos (raw)

Harness `cube/src/jvmTest/.../ReadingMeasure.kt` (`MEASURE=1`). Every full face in a fixture is matched
to the true cube (face and turn agreeing on at least 8 of 9 stickers, red and orange counted alike);
the references are the mean Lab of each true colour's centres in that video, as the scan's. Distances
are `Lab.distance` (lightness at half weight). Spread: rms distance of every non-centre reading to its
own colour's reference.

| video | step | red–orange refs apart | true red: to red / to orange, nearer orange | true orange: to red / to orange, nearer red | spread |
|---|---|---|---|---|---|
| 151828 (angled) | raw | 28.9 | 6.4 / 30.1, 0 % | 29.7 / 4.4, 0 % | 11.6 |
| 151903 (straight) | raw | 30.3 | 3.8 / 32.9, 0 % | 27.7 / 3.6, 0 % | 5.2 |
| 213729 (window) | raw | 16.0 | 4.0 / 16.6, 1 % | 13.9 / 3.6, 3 % | 4.9 |
| 213817 (dark room) | raw | 15.5 | 7.7 / 16.7, 0 % | 17.5 / 6.3, 2 % | 8.7 |
| 213850 (dim ceiling) | raw | 13.0 | 7.4 / 9.1, 48 % | 14.0 / 2.3, 0 % | 5.8 |
| 213929 (another room) | raw | 21.1 | 12.3 / 21.9, 17 % | 19.1 / 6.6, 16 % | 11.0 |

In the evening the red and orange references lie half as far apart as in daylight. On the two dim
videos the true reds are about as near orange as red (213850: half of them nearer orange; 213929: a
sixth each way), while the other colours stay apart. Readings spread 5–12 around their own colour.

## 1.2 Light correction per frame

Steps in the same harness: "brightest grey" takes each frame's brightest near-grey readings as white
(what the scan could do before anything is known); "true white" uses the truly white stickers (the
best the correction can do).

| video | step | refs apart | true red | true orange | spread |
|---|---|---|---|---|---|
| 151828 | brightest grey | 34.6 | 6.8 / 36.2, 2 % | 32.5 / 4.7, 0 % | 14.7 |
| 151903 | brightest grey | 28.9 | 5.1 / 32.0, 0 % | 27.5 / 5.6, 3 % | 8.0 |
| 213729 | brightest grey | 15.9 | 3.9 / 16.8, 1 % | 13.0 / 4.8, 0 % | 6.8 |
| 213817 | brightest grey | 15.7 | 8.2 / 16.4, 1 % | 20.2 / 11.0, 4 % | 12.4 |
| 213850 | brightest grey | 12.8 | 7.1 / 9.3, 47 % | 13.5 / 2.6, 0 % | 5.8 |
| 213929 | brightest grey | 22.1 | 12.8 / 21.5, 24 % | 21.2 / 11.4, 32 % | 16.6 |
| 151828 | true white | 32.6 | 8.1 / 32.5, 6 % | 31.1 / 7.5, 0 % | 12.2 |
| 151903 | true white | 32.4 | 3.5 / 34.2, 0 % | 29.0 / 4.4, 0 % | 5.0 |
| 213729 | true white | 15.9 | 3.9 / 16.8, 1 % | 13.3 / 4.6, 0 % | 4.5 |
| 213817 | true white | 15.5 | 8.0 / 16.2, 0 % | 17.4 / 7.1, 4 % | 8.6 |
| 213850 | true white | 12.7 | 7.1 / 9.3, 47 % | 13.5 / 2.6, 0 % | 5.8 |
| 213929 | true white | 22.5 | 12.2 / 22.0, 18 % | 19.9 / 7.8, 17 % | 11.4 |

Neither separates red from orange better on the dim videos; the brightest-grey guess spreads the
good videos' readings more, true white changes next to nothing. The camera's white balance already makes white grey (213850's white reads a2977d, warm only
slightly). **Not used by the scan**; the correction stays in the harness (`LightCorrection`, tested).

Why red reads orange-ish in 213850 (per face, G/R of the readings; blue is 0 in every red and
orange reading): red stickers on the white side read G/R 0.27 (165 readings, brighter: R 135),
those elsewhere 0.10–0.22 (R ≈ 105), orange 0.32–0.33 everywhere. The side turned to the lamp is
brighter, and the camera's tone curve lifts green out of black faster than red: a brighter red
looks more orange. This depends on each side's brightness, not on the light's colour, so a
per-frame colour correction cannot remove it. On average a red reading still lies nearer red
(7.1 vs 9.3): votes that keep how sure each reading is (1.4) are the lever.

## 1.3 Glare

A sticker's colour now leaves out pixels much brighter (× 1.1) and much greyer (under 0.6 of the
saturation) than the darker half of its blob; white stickers keep all theirs. Fixtures regenerated:
47 of 1128 lines changed. A strong reflection rarely joins a sticker's blob at all (the finder grows
blobs only within 60 of their mean colour, so it leaves a hole), so only mild sheen was affected.
The red/orange table is unchanged to one decimal; frames to clear 210, 140, 117, 120 (213817: 119
before), the dim two still never.

## 1.4 Soft votes

Each reading gives every colour a share (Gaussian of its Lab distance to the colour's reference,
shares under 5 % dropped so a clear reading is one whole vote); readings with a channel at 250 or
more count 0.2. Frames to clear and the best cube's largest clearness by the width:

| width | 151828 | 151903 | 213729 | 213817 | 213850 | 213929 |
|---|---|---|---|---|---|---|
| 0.5 (≈ hard votes) | 210 (8.0) | 140 (7.4) | 117 (6.4) | 120 (5.9) | never (2.4) | never (2.9) |
| 3 | 210 (8.0) | 140 (7.4) | 117 (6.2) | 119 (5.9) | never (2.6) | never (2.9) |
| 4 (chosen) | 210 (7.9) | 140 (7.4) | 117 (6.1) | 119 (5.8) | never (2.5) | never (2.8) |
| 5 | 210 (7.9) | 140 (7.4) | 117 (6.0) | 119 (5.5) | never (2.4) | never (2.9) |
| 6 | 210 (7.8) | 140 (7.4) | never (0.9) | 119 (4.7) | never (2.4) | never (3.0) |
| 8 | 227 (7.6) | 140 (7.4) | never (0.8) | 123 (3.1) | never (1.6) | never (2.9) |

(Dropping shares under 2 % or 10 % instead of 5 % changes nothing.)

On both dim videos the best cube at the end **is the true cube**, with hard votes as well as soft:
the rest of the cube already outvotes the three reds on the white side that read orange-ish. What
keeps them from finishing is the margin: 2.4–3.0 against the threshold 3.0. Soft votes do not
raise it, because those reds really read nearer orange on that side (1.2): sharing does not turn
them into red votes. Widths over 5 blur good readings (the window video stops clearing at 6).

## 1.6 Threshold with soft votes

`ScanSimulation` now votes softly: a misread gives its wrong colour 0.6–1 of a vote (the rest to the
true colour), a red/orange mix-up 0.5–0.8, an orange-as-red face 0.5–0.9, and in dim light a right
red or orange reading still leans up to 0.4 towards the other. 1000 runs, 1.95 ms per frame:

| T | wrong | not finished | frames to finish (median, 90 %) | not finished: biased dim, other dim, good |
|---|---|---|---|---|
| 0.5 | 2 | 0 | 92, 121 | 0, 0, 0 |
| 1.0 | 0 | 18 | 94, 149 | 18, 0, 0 |
| 1.5 | 0 | 108 | 97, 162 | 108, 0, 0 |
| 2.0 | 0 | 185 | 101, 185 | 185, 0, 0 |
| 2.5 | 0 | 270 | 102, 186 | 268, 2, 0 |
| 3.0 | 0 | 331 | 104, 189 | 314, 17, 0 |
| 4.0 | 0 | 407 | 106, 138 | 357, 50, 0 |
| 6.0 | 0 | 407 | 141, 189 | 357, 50, 0 |
| 8.0 | 0 | 456 | 289, 386 | 357, 50, 49 |

The sweep moved: the smallest T with no wrong finish is now 1.0 (was 1.5); by the same rule
(doubled) **T = 2.0** (`VideoScan.CLEAR_MARGIN`, was 3.0). Soft votes give fractional margins: a
borderline reading no longer adds a whole vote's weight to either side, so wrong cubes stay below
1 where whole wrong votes reached 1.5.

Test videos with T = 2.0 (frames to clear; before this change): 151828 209 (210), 151903 114 (140),
213729 117 (117), 213817 118 (119), **213850 176 (never), 213929 200 (never)**; every clear frame of
every video is the true cube.
