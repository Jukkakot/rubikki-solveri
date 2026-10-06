# Tasks

## 1. Handsfree timing logic

- [x] 1.1 Add the speed setting (slow / normal / fast) and the wait per move (quarter turn 5 / 3 / 1.5 s, half turn ×1.6, warning tick ~0.5 s before the end) as plain functions in `shared` (`ui/guide`); unit tests for the times of a quarter and a half turn at each speed
- [x] 1.2 Give `StepperState` (`ui/guide/StepperState.kt`) a handsfree run: after the demo ends wait the move's time, expose the fraction left for the bar, tick before the end, then `done()`; no demo repeat while it runs; with an instant animator the time starts at once; `stop()` returns to the normal repeat on the same move; ends on finish. Unit tests with a test clock: advances after the time, stop keeps the index, half turn waits longer, no advance past the end

## 2. Remembered speed and screen on

- [x] 2.1 Store the speed beside the notation switch: Android settings (`app/.../settings/Settings.kt`), browser stores (`shared/.../store/BrowserStores.kt`), passed through the nav actions like `showNotation`; default normal. Check: build of `app` and `web`
- [x] 2.2 Keep the screen on while handsfree runs: a platform seam (`ui/PlatformSeams.kt`), Android keeps the view's screen on, the browser takes a screen wake lock and releases it (fails silently where unsupported). Check: build of both

## 3. Solve screen

- [x] 3.1 Tap on the guide cube = "Tein sen" (vibration + `done()`) in the shortest-solution guide only (`plan.steps == null`, not camera follow); drag keeps turning the view; a hint chip on the cube ("Napauta = tein sen · raahaa = käännä") until the first move is confirmed. Compose test: a tap on the cube advances the step
- [x] 3.2 "Handsfree" round icon button beside back in the shortest-solution guide's action row (not in follow, not when finished) opening the ready prompt: text to take the cube in hand, speed choice, big "Valmis". While running: a big filling bar instead of the action buttons and a line "Kosketa näyttöä pysäyttääksesi"; a touch anywhere on the screen (including the top bar and the cube) stops it and is consumed. Texts in Finnish and English. Compose tests: "Valmis" starts it and the step advances after the time; a touch stops it on the same step
- [x] 3.3 Run the full checks (`./gradlew` unit tests, build of `app` and `web`) and fix what fails

## 4. Docs

- [x] 4.1 Update the `docs/` page on the solve guide with tap-to-confirm and handsfree (where the code lives, the timing decision), and mark `handsfree-guide` done in `openspec/context/roadmap.md`
- [x] 4.2 For the user to try on the phone: whether the tap is easy to hit with a knuckle while holding the cube, whether the speeds feel right, whether the touch-to-stop triggers by accident
