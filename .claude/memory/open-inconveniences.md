---
name: open-inconveniences
description: Recurring workflow inconveniences raised with the user and their reactions
metadata:
  type: project
---

1. **Stale IDE diagnostics after every edit (2026-10-04) — FIXED.** fwcd.kotlin's language server
   (Kotlin 2.1, no KMP) flooded every .kt edit with false errors; turned off for this repo in
   `.vscode/settings.json`.

2. **Quick Share links needed re-solving every time (2026-10-07) — FIXED.** Global skill
   `quickshare` (in `Jukkakot/claude-config`).

3. **Wireless adb drops between installs (2026-10-08) — ACCEPTED (user 2026-10-09: "ok antaa
   olla").** Don't repeat it. Workaround if it bites: a USB cable, or keep the screen awake with
   Wireless debugging on.

Also learned: GitHub Pages can be enabled without the user (`gh api -X POST
repos/<owner>/<repo>/pages -f build_type=workflow`), so don't list it as a manual step.

**Why:** the user wants recurring friction raised and tracked until they decide.
**How to apply:** repeat OPEN items at the end of summaries/handovers until the user reacts.
