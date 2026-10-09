#!/usr/bin/env bash
# Cuts a test video into the stills the scan harnesses read: 10 fps, 360 px wide, JPEG f0001.jpg …
# Usage: tools/make-stills.sh <video> <out dir> [crop]
#   crop: an ffmpeg crop filter for screen recordings, e.g. "crop=1080:1800:0:300" (the camera part).
# Without ffmpeg on the path (cloud sessions) it uses the static binary of pip's imageio-ffmpeg.
set -euo pipefail
[ $# -ge 2 ] || { sed -n 2,5p "$0"; exit 1; }
video="$1"; out="$2"; crop="${3:-}"

ffmpeg_bin="$(command -v ffmpeg || true)"
if [ -z "$ffmpeg_bin" ]; then
  py=""  # first Python that really runs (on Windows python3 may be the Microsoft Store stub)
  for candidate in python3 python; do
    if "$candidate" -c "" >/dev/null 2>&1; then py="$candidate"; break; fi
  done
  [ -n "$py" ] || { echo "No ffmpeg and no Python" >&2; exit 1; }
  "$py" -c "import imageio_ffmpeg" 2>/dev/null || "$py" -m pip install --quiet imageio-ffmpeg
  ffmpeg_bin="$("$py" -c "import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())")"
fi

filter="fps=10,scale=360:-2"
[ -n "$crop" ] && filter="$crop,$filter"
mkdir -p "$out"
"$ffmpeg_bin" -hide_banner -loglevel error -i "$video" -vf "$filter" -q:v 2 "$out/f%04d.jpg"
echo "$(ls "$out" | wc -l) stills in $out"
