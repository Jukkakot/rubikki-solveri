# Rubikki Solveri

Android app that scans your Rubik's Cube with the camera and teaches you to solve it, also as a
browser version. Kotlin + Compose Multiplatform, fully offline. Spec-driven with [OpenSpec](openspec/); the wiki starts
at [docs/README.md](docs/README.md).

## Lataa / Download

**[rubikki-solveri.apk](https://github.com/Jukkakot/rubikki-solveri/releases/latest/download/rubikki-solveri.apk)**
— always the newest version (Android 12 or newer).

<img src="docs/img/download-qr.png" alt="QR code for the download" width="180">

**Suomeksi:** avaa linkki (tai skannaa QR-koodi) puhelimella ja avaa ladattu tiedosto. Ensimmäisellä
kerralla Android pyytää sallimaan asennukset selaimesta: **Asetukset → Salli tästä lähteestä** →
takaisin → **Asenna**. Jos Play Protect varoittaa: **Lisätietoja → Asenna silti**. Päivitykset
samalla linkillä tai sovelluksesta: Asetukset → Tietoja → **Lataa uusin versio**.

**In English:** open the link (or scan the QR code) on the phone and open the downloaded file. The
first time Android asks to allow installs from the browser: **Settings → Allow from this source** →
back → **Install**. If Play Protect warns: **More details → Install anyway**. Updates: the same link,
or in the app Settings → About → **Download the latest version**.

**Automaattiset päivitykset / automatic updates:** install
[Obtainium](https://github.com/ImranR98/Obtainium/releases) (`app-arm64-v8a-release.apk`), then
**Add app** → `https://github.com/Jukkakot/rubikki-solveri` → **Add** → **Install**. Obtainium
tells you when a new version is out.

**Selaimessa / in the browser:** [jukkakot.github.io/rubikki-solveri](https://jukkakot.github.io/rubikki-solveri/)
— same app without installing; can be added to the home screen and works offline.

Other ways to share the app: [docs/distribution.md](docs/distribution.md).

## Build

```
./gradlew test lint assembleDebug
```
