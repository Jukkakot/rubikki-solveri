# Design

## Context

The solve screen (`shared/.../ui/solve/SolveScreen.kt`) has two stages today: the start pane (moves,
target, method, a 64 dp hold picture, "Aloita", "Handsfree" with its speed row) and the guide (`Stepper`,
a media player ⏮ ▶/⏸ ⏭). Handsfree never starts by itself, but the start screen offers it as a second big
button, so a solve could begin in the automatic mode. The user asked (2026-10-14) for a visual hold step
with hands and "Olen valmis", and for the manual mode to be the default with the automatic mode switched
on separately.

## Goals / Non-Goals

**Goals:** a hold step that shows the hold at a glance; the guide always opens manual; the automatic mode
and its speed live in the guide.

**Non-Goals:** a hold step when the hold changes in the middle of a learn solve (the words in the guide stay
for that); hands drawn in the guide itself; changing how the automatic mode advances or stops.

## Decisions

1. **A separate hold step, not a bigger picture on the start screen.** The start screen keeps its choices
   (target, method); the hold step is one idea with one button, as the user described ("ja sit vasta kun
   käyttäjä valitsee 'olen valmis'"). Costs one more tap per solve. Shown for every method that has a
   start screen (the hold matters for all). A third value of the screen's saved stage (start, hold, guide).
2. **Hands drawn in a Canvas over the 3D cube, not a bitmap.** Two cartoon hands in Compose paths: the
   palm and fingers on the left and right sides of the cube, a thumb reaching onto the front face's lower
   corner. Sized from the cube's box so they fit any size. A fixed warm skin tone with a darker outline, so
   they read on both light and dark backgrounds; the hands stay in front of the cube (thumbs) and the
   fingers are drawn behind its silhouette edge. Positions tuned on the screenshot test.
3. **The cube is the guide's holding view** (`CubeScene.DEFAULT_VIEW`, the cube's real colours, not
   draggable), so what the user sees here is exactly what the guide shows next.
4. **"Automaattinen" instead of "Handsfree"** in Finnish (English "Autoplay"), the user's own word. Only the
   label changes; code names stay `handsfree`.
5. **Speed in the ⋮ menu** as three items (Hidas, Normaali, Nopea) under a small "Automaattinen tahti"
   label, the current one with a tick. Picking one saves it (the existing remembered setting) and closes the
   menu; it does not start the automatic mode.

## Risks / Trade-offs

- [Drawn hands may look odd] → kept simple and cartoon-like; checked on the screenshot in light and dark;
  the user judges on the phone.
- [One more tap before every solve] → asked for by the user; the hold step is quick to pass.
