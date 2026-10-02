# Design

## Context

`cube-model` gives the state and moves (including rotations x/y/z), `move-guide`/`camera-follow`
present moves. The beginner solver must be deterministic, fast on a phone, and produce steps a
person can understand — not the shortest moves.

## Goals / Non-Goals

**Goals:** always solves; stage invariants hold; every step explainable with a fixed sentence
pattern; ~100–150 moves is fine.

**Non-Goals:** move-count optimisation, advanced methods (CFOP/F2L), parity of bigger cubes.

## Decisions

- **Hold**: stages 1–2 with white on top (the app's hold), then a "turn the cube over" step (z2:
  yellow up, the front stays in front), stages 3–7 with yellow on top. Whole-cube turns are y
  (and z2 once); text describes the resulting front and top centres.
- **Stage 1, cross**: per white edge (green, red, blue, orange), iterative deepening over face turns
  (depth ≤ 8), tracking only the stickers of the target and already-placed edges, so each node is
  a few array lookups. Explanation: "white–X edge next to the X centre, white on top".
- **Macro search** for stages 2–7: a step is a macro = optional whole-cube turn (y^r), optional
  setup turn of a layer (D^k or U^k), and an algorithm repeated n times. Breadth-first over macros
  (depth ≤ 2–3) finds the fewest macros (then fewest moves) reaching the stage's next sub-goal
  without breaking earlier stages. This yields exactly the human recipe ("turn the cube so…, turn
  the bottom until…, repeat the trigger until…").
- **Stage order of the last layer** is the official beginner order: yellow cross, yellow edges
  (R U R' U R U2 R' U), yellow corners placed (U R U' L' U R' U' L), yellow corners turned (the
  trigger, U between corners). Turning corners before placing them does not work: the placing
  algorithm twists corners.
- **Sub-goals**: stage 2 one more white corner; stage 3 one more middle edge; stage 4 the yellow
  cross; stage 5 all yellow edges solved; stage 6 all corners placed; stage 7 each corner up, then
  a last U turn (the bottom looks scrambled in between and comes back — the text says so).
- **Explanations as data** (`StepNote`: kind + colours), turned into sentences by the app (both
  languages) — the cube module stays free of UI text.
- **Learn mode** in `SolveScreen`: segmented choice (shortest / learn), one `StepperState` over all
  moves; the current step is found from cumulative move counts. Stage intros (what and why) are
  string resources, reused by `lessons`.
- **Camera follow with rotations**: if the frame matches the front after the move, it is done
  even when the centre changed (a whole-cube turn changes the front centre).

## Risks / Trade-offs

- [Cross search too slow on the phone] → tracked-sticker state, move pruning (no same face twice,
  ordered opposite faces); measured in tests over 300 cubes; worst case depth 8 rarely needed.

## Decisions made while building

- Stage intros show only on a stage's first step (screenshots: otherwise the buttons fall below
  the fold).
- Measured: 500 random cubes, about 160 moves on average, worst 52 ms on a desktop.
- `SolveScreen` takes a planner (`plan(cube, method)`), so tests plan inline; `initialMethod`
  lets other screens open the step-by-step view directly.
