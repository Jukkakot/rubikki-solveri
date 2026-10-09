# Proposal

## Why

The user works from cloud sessions for a week. The scan harnesses (`VIDEO_HARNESS`, `ACCEPTANCE`,
`TIMELINE`, the size harness) read stills from `testdata/video/<date>/stills/`, and about 87 MB of
them exist only on the desktop (four 2026-10-07 20:20 recordings, `web_084657`, `web_121505`).
New phone recordings must also turn into stills and fixtures inside a cloud session.

## What Changes

- Commit every still under `testdata/video/*/stills/` (repo grows from about 85 to about 170 MB).
  Videos, `frames/`, contact sheets and shared logs stay local (user, 2026-10-09: no videos in git).
- `.gitignore`: still folders are no longer ignored, so new stills need no `git add -f`.
- `tools/make-stills.sh <video> <out dir> [crop]`: the existing recipe (ffmpeg, 10 fps, 360 px
  wide, JPEG) as one command; when `ffmpeg` is missing (cloud), it uses the static binary from
  `pip install imageio-ffmpeg`.
- Docs (`docs/development.md`): the new-recording flow, also in a cloud session: Quick Share link
  → `testdata/video/<date>/` → `make-stills.sh` → fixture with `VideoScanHarness.writeFixtures`
  → commit stills and fixture; the video itself stays in the session.
- Decisions: stills in plain git, not Release zips or Drive (user: simplest; the cloud gets them
  with the clone); ffmpeg on demand, not in the session-start hook (most sessions never need it).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None (tooling and test data only; `skip_specs`).

## Impact

No module code changes (`cube`, `app` untouched); `.gitignore`, `tools/`, `docs/`, test data.
