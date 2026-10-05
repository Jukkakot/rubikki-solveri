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
