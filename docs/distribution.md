# Distribution

How the app reaches other people without Google Play (its developer account costs money;
product.md). Decided with `phone-install` (2026-10-04).

## Now — Implemented

- **GitHub release link + QR code.** Every green push to `main` replaces the release
  "latest-build", so
  `https://github.com/Jukkakot/rubikki-solveri/releases/latest/download/rubikki-solveri.apk`
  always serves the newest APK. No GitHub account needed to download. The repo's front page
  (`README.md`) has the link, the QR code (`docs/img/download-qr.png`, made by
  `scripts/download-qr.py`) and the install steps in Finnish and English; each release's notes
  repeat them (`.github/release-notes.md`).
- **Obtainium** (free, open source) installs apps straight from GitHub releases and checks for
  updates. It works with the rolling release as is: one APK asset per release.

## Later options

| Route | Cost / effort | Reaches | What it would need |
|---|---|---|---|
| Browser version | none (GitHub Pages) | everyone: also iPhone and computers, no install prompts | the `web-app` change (roadmap 27) |
| F-Droid main repository | free; review, slow | F-Droid users | an open-source licence for the whole repo, a reproducible build from source, metadata, and a name without the "Rubik's" trademark |
| Own F-Droid repository on GitHub Pages | free; `fdroidserver` setup in CI | F-Droid client users | a repo signing key; mostly redundant with Obtainium |
| Samsung Galaxy Store | free seller account; review | Samsung phones | a dedicated release key (stores reject debug-signed APKs), store listing, privacy policy |
| Google Play | one-time fee | everyone | not planned (budget 0 €) |

## Signing and other people

The APK is signed with the author's Android Studio debug key, so it updates the author's own
Run ▶ install without losing data. Sideloaded installs accept that key. Any store route needs a
dedicated release key instead; switching keys later means every user uninstalls once (losing the
app's local history), so a store decision should weigh that.
