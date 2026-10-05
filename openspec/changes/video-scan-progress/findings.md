# Findings: clear threshold (task 1.3)

Simulation `cube/src/jvmTest/.../ScanSimulation.kt` (`SIMULATION=<runs>`; table written to
`testdata/video/simulation.txt`, git-ignored). Model, from `video-scan-spike`: random cube; faces in a
random order, held 8–25 frames, 2–6 frames of two faces at an angle while turning; in 20 % of runs
one face is only shown in the second round (every face again, 1.5 s each, until clear or 45 s).
Per reading: 5 % of stickers missing; a wrong colour starting in 1 % of readings (3 % at an angle,
doubled in dim light) and lasting 1–5 frames, red↔orange in 70 % of those; red↔orange mixed up in
5 % of readings (15 % dim); 40 % of runs dim, and in a dim run each face with 30 % chance reads 60 %
of its orange as red all through (the phone test of 2026-10-05). Finish = clearness over T for 5
frames (half a second).

1000 runs (design said 10 000: the 1000 took 12 minutes at 1.7 ms per frame; the wrong finishes
stop well below the chosen T, so more runs would not move it):

| T | wrong | not finished | frames to finish (median, 90 %) | not finished: dim with orange-as-red faces, other dim, good |
|---|---|---|---|---|
| 0.5 | 24 | 3 | 92, 123 | 3, 0, 0 |
| 1.0 | 3 | 32 | 94, 141 | 32, 0, 0 |
| 1.5 | 0 | 76 | 97, 146 | 76, 0, 0 |
| 2.0 | 0 | 128 | 99, 153 | 128, 0, 0 |
| 3.0 | 0 | 191 | 105, 178 | 191, 0, 0 |
| 4.0 | 0 | 268 | 110, 189 | 265, 3, 0 |
| 6.0 | 0 | 381 | 170, 260 | 329, 50, 2 |
| 8.0 | 0 | 709 | 354, 427 | 329, 50, 330 |

**Chosen: T = 3.0** (`VideoScan.CLEAR_MARGIN`): smallest T with no wrong finish is 1.5, doubled.
Every run in good or merely dim light finishes, a median of 10.5 s (90 %: 18 s). The runs that do
not finish are those where whole faces read orange as red: no possible cube fits clearly, which is
what the stall "nothing fits / more light" is for. Margins are in nats (−ln shares).

Other numbers:
- Best cube with margins: 1.5–1.7 ms per frame on the JVM (desktop). Each margin search stops as soon
  as nothing cheaper can follow and is capped at 15; without that a frame took tens of ms.
- Red/orange lending (up to 2 votes) keeps red/orange stickers weaker: 5 agreeing readings give a
  red/orange difference of only 0.69, 20 readings 1.95, so their pieces become clear mostly through
  the structure and more frames.
- Five whole sides do not always fix the sixth: when several of the unseen side's edges show its own
  colour outwards, they can trade places. 38 of 60 random cubes were clear with one side unseen and
  5 readings per sticker (more with more readings); the margin, never a wrong guess, covers the rest.
