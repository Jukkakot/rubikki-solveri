## Context

Lessons live in `ui/lessons/LessonCatalog.kt` (texts and algorithms per stage) and
`LessonScreens.kt` (list, a scrolling lesson page, `AlgorithmCard` with a 3D demo). `Cube3D` takes
54 sticker colours and a `marked` set (strong outline), so grey stickers and outlines need no new
drawing code. The beginner method holds the cube white on top for stages 1–2 and turns it over
(`z2`, yellow on top) at the start of the middle layer (`BeginnerSolver`). Practice runs on
`SolveScreen` in learn mode, so the goal card there covers both practice and guided solving.
User decisions: grey goal cube, swipe pages, everything in one change.

## Goals / Non-Goals

**Goals:** pictures carry each lesson; no lesson page scrolls; case data is checked by tests.

**Non-Goals:** new algorithms or a different method; the `solve-challenge` idea (backlog);
illustrations drawn by hand (all pictures are the app's own 3D cube).

## Decisions

- **Goal masks in the cube module** (`StageGoals`): for each stage, the stickers in place after it
  and the stickers it adds, on a solved cube in the stage's hold (white up for stages 1–2, yellow
  up from 3). Stage 4 shows only the yellow stickers of the edges (their side colours stay grey);
  stage 6 shows the corners coloured with the line "corners may still be twisted". Pure data,
  JVM-tested (e.g. the cross mask has 5 + 4 + 4 coloured stickers).
- **Cases in the cube module** (`StageCases`): each case is an id, a setup (moves from the stage's
  start that create the situation), the stickers to highlight and the solving moves (an algorithm
  times n, plus any U/D turn). A test applies each case's moves and checks the stage's piece or
  goal is reached, so captions cannot drift from the moves. The app maps ids to captions.
  Cases per stage: 1 edge down / middle / top flipped; 2 white right ×1, white front ×5, white
  down ×3; 3 edge goes right / left / stuck in the middle; 4 dot, L, line; 5 two neighbours right
  / two opposite right; 6 one corner in place / none; 7 yellow right ×2 / yellow front ×4.
- **One picture component, `GoalCube`**: `Cube3D` with colours masked to grey and `marked`
  outlines, a fixed view, draggable only when large. Used for goal pictures, list thumbnails,
  cases and before/after. Grey is the theme's `outline` colour: mid grey in light and dark (`surfaceVariant` was tried first; in the light theme it reads as white stickers). Alternative: 2D
  top-view diagrams for the last layer; rejected to keep one consistent look.
- **Pager:** `HorizontalPager` with page dots and a bottom bar (back / next; on the last page the
  primary button is "Practise"). Buttons, not only swiping, so the pages are discoverable.
- **Cases page:** a 2×2 grid (at most four cases per stage, three for most). Tapping a case opens it
  large on the same page with a play button; back returns to the grid.
- **Algorithm page:** the demo cube large, notation with the current move highlighted, the current
  move in words under it, and before/after thumbnails. The pieces it moves are computed from the
  algorithm (stickers that differ between solved and solved+algorithm) in the cube module.
- **Goal card in `SolveScreen`:** shown when the current step's stage differs from the previous
  step's (and at the first step), covering the guide until "Continue". The intro text
  (`stageIntro`) is removed from `StageCard`; a thumbnail beside the stage name opens the goal
  large in a dialog.
- **Texts:** `lesson_N_how` strings go; summaries and tips are rewritten to one short line; case
  captions are new short strings. Basics paragraphs are cut to two lines each, one per page.

## Risks / Trade-offs

- [Thumbnails in the list are eight 3D canvases] → small, static (no animation), cheap to draw.
- [Case setups look unnatural if built only from inverse algorithms] → setups start from a
  practice-like position (earlier stages solved) so the rest of the cube looks scrambled, not grey;
  only the pictured piece is highlighted.
- [The goal card adds a tap per stage in guided solving] → seven taps per solve; it is the point
  of the feature. Revisit if it feels slow on the phone.
