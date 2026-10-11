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

4. **Maven Central answers 429 in cloud sessions (2026-10-11) — ACCEPTED (user 2026-10-11: "anna tuon
   olla").** Don't repeat it. Installing the mirror init script from a session is blocked by the auto-mode
   classifier; plain retries of the Gradle build got through within a few tries (downloads stay cached). Gradle builds and
   Robolectric's android-all download failed until a `~/.gradle/init.d/central-mirror.gradle` swapped
   Maven Central for Google's mirror (`maven-central.storage-download.googleapis.com/maven2/`) and the
   Robolectric jar was fetched into `~/.m2` by hand. Proposed: `install.sh` in `claude-config` writes
   that init script (and Robolectric's `robolectric.dependency.repo.url`) in cloud sessions.

Also learned: GitHub Pages can be enabled without the user (`gh api -X POST
repos/<owner>/<repo>/pages -f build_type=workflow`), so don't list it as a manual step.

**Why:** the user wants recurring friction raised and tracked until they decide.
**How to apply:** repeat OPEN items at the end of summaries/handovers until the user reacts.
