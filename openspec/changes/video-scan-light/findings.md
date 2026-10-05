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
