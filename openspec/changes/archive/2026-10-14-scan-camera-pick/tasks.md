# Tasks

## 1. Exposure control (cube)

- [x] 1.1 `ExposureControl`: brightness kept at the lock; lighter step (or plain re-meter) after 1 s clearly darker; darker cap until the torch changes
- [x] 1.2 `ExposureControl`: a darker step judged once the brightness moved 8 % or after 1.2 s
- [x] 1.3 `Frame` carries the event (darker, lighter, lock) with brightness and washed share
- [x] 1.4 Tests: dark after lock → lighter and re-lock, no darker again; not darker → plain re-meter; shadow above 150 keeps the lock; slow camera gets no extra step; torch resets the cap; existing tests adjusted

## 2. Screen log (app)

- [x] 2.1 `VideoScanScreen` logs each exposure event with darker, light and washed

## 3. Browser camera (web)

- [x] 3.1 `openBackCamera`: back cameras ranked by continuous focus and torch, lowest number on ties; new storage key
- [x] 3.2 `frameRate: { ideal: 30 }`; camera line adds fps and the tried cameras' abilities
- [x] 3.3 Web build passes

## 4. Docs

- [x] 4.1 Roadmap item done; docs pages touched where the camera choice or exposure is described
