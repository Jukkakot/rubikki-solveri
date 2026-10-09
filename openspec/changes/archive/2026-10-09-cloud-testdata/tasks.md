# Tasks

## 1. Stills in git

- [x] 1.1 `.gitignore`: keep ignoring `testdata/video/**` but not `testdata/video/*/stills/**` (videos, frames, sheets, shared logs stay local). Verify: `git status` lists only still JPEGs as new.
- [x] 1.2 Commit the untracked stills (2026-10-07 four 20:20 recordings, 2026-10-08 `web_084657`, 2026-10-08c `web_121505`). Verify: `git ls-files` count equals the files on disk for every stills folder.

## 2. Making stills anywhere

- [x] 2.1 `tools/make-stills.sh <video> <out dir> [crop]`: ffmpeg at 10 fps, 360 px wide, JPEG `f%04d.jpg`; without `ffmpeg` on the path, `pip install imageio-ffmpeg` and use its binary. Verify: run on a local video, frame count and size match the committed stills of that video.

## 3. Docs and roadmap

- [x] 3.1 `docs/development.md`: new-recording flow (local and cloud): Quick Share → `testdata/video/<date>/` → `make-stills.sh` → `VideoScanHarness.writeFixtures` → commit stills and fixture. `docs/architecture.md`: drop "the stills of the four 2026-10-07 20:20 recordings stay local".
- [x] 3.2 Roadmap: `cloud-setup` backlog item's test-material part done.
