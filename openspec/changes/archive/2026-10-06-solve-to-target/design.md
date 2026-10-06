# Design

## Context

`SolveScreen(cube, planner, initialMethod …)` plans with `plan(cube, method)`: two-phase for FAST,
`BeginnerSolver` for LEARN; the stepper celebrates at the end of any plan
(`StepperState.celebrations`), so a target needs no new finish logic. The ported min2phase keeps
`CubieCube` with multiply and inverse. Cubes are colour arrays with fixed centres (scan and hand
input normalise them).

## Goals / Non-Goals

**Goals:** solve S → T exactly and about as short as a normal solve; one picker with four ways;
reuse the hand-input screen, `Cube3D`/`GoalCube`, `StageGoalPicture`.

**Non-Goals:** typed or pasted move sequences, scanning a target from another cube, partial "any
colour here" targets (backlog ideas 6, 8, 11 from 2026-10-06); saving painted targets.

## Decisions

1. **S → T by the group.** Convert S and T to cubie cubes, X = T⁻¹·S, solve X with the two-phase
   search; the moves m then take S to T (S·m = T) because a cube's moves act the same relative to
   any reference. A test checks S.apply(m) == T for random S and every pattern. *Alternative:*
   solve(S) + inverse(solve(T)): always right but about twice as long.

2. **Target model.** `SolveTarget` = `Solved` | `Pattern(id)` | `Stage(stage)` | `Painted(colors)`;
   carried in `SolveRoute(cube, target)` as a short string (pattern id, stage ordinal, or colour
   string), so back navigation and process death keep it.

3. **Patterns** (`cube/Patterns`): id, Finnish/English name (resources), move sequence from solved.
   Starting list (each verified by a test that its picture matches its idea, and by eye in the
   gallery): checkerboard `R2 L2 U2 D2 F2 B2`, six spots `U D' R L' F B' U D'`, cube in a cube
   `F L F U' R U F2 L2 U' L' B D' B' L2 U`, cube in a cube in a cube
   `U' L' U' F' R2 B' R F U B2 U B' L U' F U R F'`, superflip
   `U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2`, anaconda `L U B' U' R L' B R' F B' D R D' F'`,
   python `F2 R' B' U R' L F' L F' B D' R B L2`, tetris `L R F B U' D' L' R'`, twister
   `F R' U L F' L' F U' R U L' U' L F'`, cross `U F B' L2 U2 L2 F' B U2 L2 U`, vertical stripes
   `F U F R L2 B D' R D2 L D' B R2 L F U F`. Any sequence that does not look like its name is
   dropped rather than guessed further. Names get the app's playful voice (e.g. "Shakkilauta",
   "Kuutio kuutiossa", "Superflippi", "Anakonda").

4. **Picker screen (`TargetRoute`).** One scrolling grid: a "Yllätä minut" button, the gallery
   (three per row), the stages list, "Maalaa oma". Gallery thumbnails are small still `GoalCube`s,
   as in the lessons list (decided while implementing: consistency over a new 2D drawing; the
   lessons list already shows eight of them without trouble). Tap → an `AlertDialog` with a large
   turnable `GoalCube` and "Valitse", like the stage goal dialog on the solution screen. A choice
   navigates to `SolveRoute(start, target)`: from a solution screen it replaces that screen (and the
   picker), from home it stays on top of the picker so back returns to the gallery.

5. **Stage target.** LEARN plan cut after the last step of the chosen stage; the method choice is
   hidden (as in practice). Stage pictures: `StageGoalPicture`.

6. **Solution screen.** Under the method choice a target row: small isometric picture + name +
   "Vaihda" button. Pattern/painted targets hide the method choice (shortest only); solved keeps
   both methods. Finished text: "Kohde valmis!" for non-solved targets. Already-at-target message
   offers "Valitse kuvio".

7. **Hand input in target mode.** `ManualInputScreen` with title "Maalaa kohde", prefilled with the
   current target, no scan helpers; on valid it returns the colours as a `Painted` target.

8. **Home tile.** Fifth secondary entry "Kuviot" (icon: a small checkerboard). The tiles stay in
   rows of two (`chunked(2)`); the fifth tile spans the last row's width. *Alternative:* three
   columns; rejected, labels get cramped on a narrow phone.

## Risks / Trade-offs

- [A pattern sequence remembered wrong] → test + visual check per pattern; drop instead of fix.
- [Fixed centres assumed] → `Cube` from scan and hand input already has standard centres; a test
  covers six spots (centres look moved but are not) from a scanned start.
- [Five tiles look uneven] → checked in the screenshot test of the home screen.
