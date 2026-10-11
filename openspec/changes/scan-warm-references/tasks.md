# Tasks

## 1. Diagnosis

- [x] 1.1 Replay `web_20261011_100403` and `101618` with the scan's red/orange references logged over time (source tracks, cast fallback, distance to the true-centre references; design 1); write the outcome into design.md Findings; fix a wrong reference source if found, with a JVM test for it; verify `./gradlew :cube:jvmTest` passes.

## 2. Warm references (cube, commonMain)

- [x] 2.1 Add per-face warm groups and the split (design 2): warm positions averaged over the track, the largest-gap split with `WARM_HUE_STEP`, a warm centre anchoring its side; JVM tests: a shaded face's red and orange split right, a one-kind face gives no split, a misread lone sticker under the gap gives none; verify `./gradlew :cube:jvmTest --tests "*FaceTracks*"` passes.
- [x] 2.2 Name readings with face references for red/orange and whole-cube references from the faces' groups (design 3); the `warmRefs` mode switch (design 5); renaming only the tracks whose references moved (design 4); JVM tests: no face with both kinds → names as today, a shaded face's oranges named orange; verify `./gradlew :cube:jvmTest` passes, including the scan budget test.

## 3. Comparison

- [x] 3.1 Extend `ColourSpikeHarness` (`COLOUR_SPIKE=1`) with the three modes on every source plus `scanMs`; run it; write the table and chosen mode into design.md Findings; set the default mode by the go rules (design 5–6), or stop and report if no mode passes; verify the table covers every source and the go rules are stated as met or not.
- [x] 3.2 Make the stuck recordings fixtures if the chosen mode finishes them (finish with the true cube, as other recording fixtures); verify `./gradlew :cube:jvmTest` passes.

## 4. Docs

- [x] 4.1 Update `docs/` where the scan's colour naming is described, the roadmap item 82 to done (backlog note on the stuck recordings updated), and list the phone check for the user (a scan under the 2026-10-11 room light); verify the roadmap row and backlog text match the outcome.

*Built as NEAREST (design Findings): the faces' own red and orange as further references where the centres name them wrong; FACE, CUBE and FACE_ONLY removed. 3.2: 10:16 pinned in `ScanWarmReferencesTest` (10:04 was already). Budget test limits measured again (user 2026-10-11).*

## Workflow follow-up

- Archive with `/opsx:archive scan-warm-references`, commit and push to `main`.
