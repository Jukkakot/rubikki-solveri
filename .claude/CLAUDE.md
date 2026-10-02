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
- **Autopilot:** off. When the user turns it on: apply → verify → commit → archive → commit →
  push → next specced change, stopping only for money, anything irreversible outside the repo, a
  decision that forces rework, or failing checks that cannot be fixed.

## Working agreements

- The user is new to Android development: when a step needs them (Android Studio, phone settings,
  signing), give exact click-by-click instructions.
- Commit and push to `main` yourself once the GitHub repo exists.
- End every summary that changed something runnable with a short "How to check".

## Handover

At most two lines:

```
Jatka: /opsx:apply <change> (seuraava <task no.>)      ← or /opsx:propose <roadmap item>
Huom: <only what is not in the repo>
```
