# Rubikki Solveri – working instructions

Android app (Kotlin, Jetpack Compose) that scans a Rubik's Cube and teaches solving it. Product
decisions: `openspec/context/product.md`; plan: `openspec/context/roadmap.md`; wiki: `docs/README.md`.

## Session start

Read `openspec/context/roadmap.md` and run `openspec list`, then tell the user in two or three
lines where the project stands (last finished change, active change and its task progress, the
natural next step).

## Phases

- **Spec phase (now):** write proposal, design, specs and tasks for one change, then stop for the
  user's review before the next. Ask opinion questions freely; the user often answers by voice, so
  plain numbered questions in text work better than pickers.
- **Autopilot:** **ON since 2026-10-02**, also for proposing the roadmap items not yet specced. Run
  the loop without review stops: propose → apply → verify → commit → archive → commit → push →
  next roadmap item. Make UX and technical decisions yourself from `product.md` and the specs;
  record each non-obvious one in the change's `design.md` and list them in the summary. Stop only
  for money, external accounts, anything irreversible outside the repo, a decision that forces
  rework, or failing checks that cannot be fixed. Work that needs the real phone (camera tuning,
  how the 3D cube feels) is built and unit-tested, then listed for the user to check.

## Working agreements

- The user is new to Android development: when a step needs them (Android Studio, phone settings,
  signing), give exact click-by-click instructions.
- Commit and push to `main` yourself once the GitHub repo exists.
- Summaries list the changes made (what the user will notice) and every decision taken on the
  user's behalf, so the user can validate and correct them. No "How to check" section.
- Don't install or launch the app yourself (decided 2026-10-03): the user puts it on the phone
  with **Run ▶** in Android Studio. Install via adb only when the user asks, and only if `adb devices`
  lists the phone. Never uninstall (it deletes the user's history).
- UI changes are described in words in the summary: which screen, what changed (decided
  2026-10-03). The screen gallery (https://claude.ai/artifact/FWqmj6ZeSBgK4qiQFXZyRR; recipe in
  `docs/development.md`) is not refreshed per change; use it, lighter (no navigation map), only for
  a bigger UI overhaul or when asking the user's opinion on screens.

## Handover

At most two lines:

```
Jatka: /opsx:apply <change> (seuraava <task no.>)      ← or /opsx:propose <roadmap item>
Huom: <only what is not in the repo>
```
