# Design

## Context

Log 18:29: `kind=lock darker=0 light=143 washed=0`. The next snapshot was still at `light=157`, then
`faces=0.1`, `0.0`, `0.0` until the user left. Earlier logs from the same phone showed the brightness
halving after the lock. `camera 3` at 16:12 kept its brightness after the lock, so the effect depends
on the camera. `cameraLockExposure` sets `exposureMode: 'manual'` with no `exposureTime`, which leaves
the value to the browser and camera.

## Decisions

1. **Hold what the camera had.** On lock, read `track.getSettings()`. If `exposureTime` (and `iso`)
   are numbers and the capabilities list them, pass them with the manual mode. This may already fix
   the phone, but browsers may report stale values, so decision 2 stays.
2. **Check the lock by the whole picture, not the faces.** A 16×16 copy of the video gives the mean
   brightness. It is taken just before the lock and 700 ms after. Below 60 % of the before value means
   undo: `exposureMode`/`whiteBalanceMode: 'continuous'`. Faces cannot be used: once dark, none are
   found. The whole picture is coarse, since the user might point the camera elsewhere within 0.7 s.
   That is why the 60 % threshold is strict. A false undo only means scanning on without the lock,
   which the spec already allows.
3. **Remember per camera.** `localStorage` key `camera.lockDarkens.<deviceId>`. The next scans on that
   camera skip the lock (`skipped` in the lock line) instead of darkening and undoing every time. The
   torch's re-metering also locks, so the same skip applies there.
4. **Logging from Kotlin.** The JS check is asynchronous. `cameraLockCheck()` returns the outcome text
   once known (empty before). `WebCamera` waits 1 s after a lock and logs
   `scan.lock check=<kept|undone> before=… after=… time=…`.

## Risks / Trade-offs

- Without the lock, red and orange may drift with auto white balance. The scan already counts
  washed-out readings little and judges colours against the cube's own references. A dark picture is
  the worse failure.
