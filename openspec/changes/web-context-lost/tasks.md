# Tasks

## 1. Recover from a lost context

- [x] 1.1 `platform.mjs`: export a hook that watches every canvas that gets a WebGL context for `webglcontextlost` (wraps `getContext`; the canvas is in a shadow root), reports it to Kotlin, and reloads the page when visible (now, or on the next `visibilitychange` to visible); at most one automatic reload per 30 s via `sessionStorage`. Verify: web build passes.
- [x] 1.2 Kotlin side (`Main.kt` / `WebServices.kt`): install the hook at start, log the loss as a warning, and clear the crash flag for an automatic reload so no "crashed last time" notice appears. Verify: web build passes.
- [x] 1.3 Smoke test (`web/smoke/smoke.mjs`): open a screen, drop the context with `WEBGL_lose_context.loseContext()`, check the page reloads on the same route with no page error and no crash notice. Verify: `node web/smoke/smoke.mjs <dist>` passes.

## 2. Docs and roadmap

- [x] 2.1 Roadmap: add `web-context-lost` as done and remove the backlog item; note in `docs/` (web page) only if the map changes. Verify: roadmap row present.
