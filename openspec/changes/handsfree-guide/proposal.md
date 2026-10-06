# Proposal

## Why

In the shortest-solution guide every move is confirmed with the "Tein sen" button, so the user has
to put a hand off the cube and aim at a button after each turn. The user wants the guide to feel
hands-free (spoken commands were considered and left out, 2026-10-06).

## What Changes

- **Tap the cube = done:** in the shortest-solution guide a tap anywhere on the 3D cube counts as
  "Tein sen". Dragging still turns the view; back and "Näytä" stay buttons. A hint on the cube
  tells this until the first move of the solve is done.
- **Handsfree mode:** a "Handsfree" button opens a ready prompt: take the cube in hand, pick the
  speed (slow / normal / fast), press the big "Valmis". From then on each move plays its demo and,
  after the move's time (a half turn gets more), the guide moves on by itself. A filling bar shows
  the time left, and a light vibration comes just before the next move.
- **Any touch stops it:** a touch anywhere on the screen during handsfree returns to the normal
  guide on the same move (that touch does nothing else). Handsfree is started again with the button
  and "Valmis".
- Speed is remembered; handsfree itself is not (chosen each time). The screen stays on while
  handsfree runs.
- Only in the shortest-solution guide (also when solving to a target), not in the learn method,
  practice, the timer's scramble or camera follow.

Decisions taken in the proposal (light lane, no design.md):
- In handsfree the demo does not repeat every 3 s; the time to do the move runs instead. After a
  stop, the normal repeat resumes.
- Times after the demo ends: slow 5 s / normal 3 s / fast 1.5 s for a quarter turn, ×1.6 for a
  half turn; tuned on the phone if needed. With animations off the time starts at once.
- No swipe gesture for back: it would collide with dragging the view.
- The "Handsfree" button is a small round icon button beside back in the action row. While
  handsfree runs the action buttons are hidden; a big bar and "touch the screen to stop" take their
  place. A plain touch stops it (the phone is assumed to lie on a table; user, 2026-10-06).

## Capabilities

### New Capabilities
- `handsfree-guide`: tapping the cube to confirm, and the handsfree mode that advances by itself.

### Modified Capabilities
<!-- none: move-guide requirements stay; handsfree adds to them -->

## Impact

- `shared`: solve screen stepper (tap on the cube, handsfree prompt, timer, stop on touch), texts
  in Finnish and English, settings store for the speed.
- `app` / `web`: remembered speed in the platform stores; keep the screen on (Android window flag,
  browser wake lock).
- `cube`: not touched.
