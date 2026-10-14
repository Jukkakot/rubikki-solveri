# Design

## Context

What happened on the OnePlus phone (Chrome 154, log 2026-10-14):

- Browser camera choice (`platform.mjs` `openBackCamera`): `facingMode: environment` gave `camera 3`. It
  had a torch, so it was kept (`tried=1`) and remembered. Its abilities were `focus=manual` only (no
  continuous or single-shot) and compensation −3..3 in steps of 1/6 EV. On the user's Samsung the choice
  is `camera 0`, with `focus=manual,single-shot,continuous`.
- The scan read 11–12 pictures a second with `finderMs` 7–16, so the camera, not the reading, set the
  pace.
- `ExposureControl` stepped darker every 600 ms while more than a third of the readings were washed out:
  two steps within about 1.5 s, then the lock. After the lock the sticker brightness (`light`) fell from
  233 to about 100. Nothing in the control makes the camera lighter, so the scan went on in the dark.

We have no access to the phone. The change has to help in general, and the log has to show the next
failure plainly.

## Goals / Non-Goals

**Goals:** open the main back camera in the browser; recover from a picture that turns dark after the
lock; no overshoot on a camera that adjusts slowly; log lines that tell which camera was used, what
the alternatives were, and what each exposure step did to the brightness.

**Non-Goals:** the Android app's camera choice (CameraX opens the main camera); a camera picker in
Settings; the scan logic itself (the unread last side is a separate change).

## Decisions

1. **Rank the back cameras by abilities, not by label.** Score: continuous focus (2) + torch (1). Ties go
   to the lowest number in the label (`camera 0` before `camera 3`), then to enumeration order. Labels are
   not standardised across browsers, so the number is only a tie-breaker. The browser's pick is kept at
   once when it scores 3, the most a camera can score. That keeps today's fast path on the user's phone.
   Otherwise every other back camera is opened once (as today for the torch). Its capabilities are read
   (`focusMode`, `torch`), it is closed, and the best is opened again. A front camera (by `facingMode`
   or label) never counts.
2. **Re-choose once on every phone.** The remembered camera moves to a new storage key
   (`camera.device.2`), and the old key is removed. Each phone then chooses once under the new rule.
   The cost is a second or two once. The alternative (re-choose only when the remembered camera lacks
   continuous focus) needs the camera opened first, so it costs the same and is more code.
3. **Ask for 30 pictures a second** (`frameRate: { ideal: 30 }`). It is harmless where ignored, and some
   Android cameras default lower in the browser.
4. **Lighter after a dark lock.** At the lock the control keeps the sticker brightness (the median of
   max(r, g, b) over the readings, as `VideoScan.brightness`). Suppose that while locked the brightness
   stays below 2/3 of the lock value and below 150 for `DARK_MILLIS` (1 s). If the camera is set darker,
   it goes one step lighter; if not, it is just unlocked. Either way it meters again (unlocked, a new
   lock within about a second). The step it was made lighter from becomes the most it may be made
   darker (`cap`) until the torch changes, so washed → darker → dark → lighter cannot cycle. A
   brightness under 150 is not too dark to read, so a cube simply turned into shadow does not unlock
   needlessly. "Clearly darker" needs both conditions.
5. **A darker step waits for the picture to change.** After a step, the control keeps the brightness
   seen at the step. The next judgement waits until the brightness has moved by at least 8 % from that
   value, or `STEP_MAX_MILLIS` (1.2 s) has passed, and never earlier than the 600 ms settle. The metering
   deadline is already held back while a step is under way, so a slow camera locks a little later
   rather than overshooting.
6. **Events from the control for the log.** `Frame` gains an `event` (`darker`, `lighter`, `lock`) with
   the brightness and washed share. The screen logs one `scan.video kind=<event> darker=… light=…
   washed=…` line for each event. That stays well within "a few dozen lines a minute": at most a few
   steps per scan.
7. **Camera line in the browser.** `cam.info` adds `fps=<settings.frameRate>` and, when others were tried,
   `backs=<label>:<c|m>[t], …` (c = continuous focus, m = no continuous focus, t = torch). For example:
   `backs=camera 0:ct,camera 2:m,camera 3:mt`.

## Risks / Trade-offs

- Opening each back camera once costs about 0.3–0.5 s per camera. Up to five on some phones, once per
  phone.
- A main camera with continuous focus but no torch beats a torch-only fixed-focus lens. The torch matters
  less than focus for reading colours, and the torch button is then hidden.
- The 150 / 2/3 thresholds come from one log. If they are too eager, the lock may be undone in a
  normal shadow. It costs at most one extra metering, and the log line shows it.
