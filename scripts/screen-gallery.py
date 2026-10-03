"""Builds the screen gallery from the screenshot test's output.

Run after `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'`:

    python scripts/screen-gallery.py

Reads app/build/screenshots/<name>-light.png and <name>-dark.png, writes build/gallery/index.html
and build/gallery/img/*.jpg (downscaled with ImageMagick). The page is published as a private
claude.ai artifact; see docs/development.md.
"""
import datetime
import html
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent
SHOTS = ROOT / "app" / "build" / "screenshots"
OUT = ROOT / "build" / "gallery"

# Screens by area, in the order a user meets them. A screen missing here lands under "Muut".
GROUPS = [
    ("Aloitus", ["home", "settings", "about", "log"]),
    ("Skannaus", ["scan", "scan-review", "scan-permission", "scan-check"]),
    ("Käsin syöttö", ["manual-input"]),
    ("Ratkaisu", ["solve", "solve-learn", "guide-back", "guide-right", "follow", "mid-turn"]),
    ("Oppiminen", ["lessons", "lesson"]),
    ("Ajanotto", ["timer", "history"]),
    ("Vapaa kuutio", ["free-cube"]),
]


def git(*args: str) -> str:
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True).stdout.strip()


def main() -> None:
    names = sorted({p.name.rsplit("-", 1)[0] for p in SHOTS.glob("*-light.png")})
    if not names:
        raise SystemExit(f"No screenshots in {SHOTS}; run the ScreenshotTest first.")
    known = {n for _, group in GROUPS for n in group}
    groups = [(title, [n for n in group if n in names]) for title, group in GROUPS]
    groups.append(("Muut", [n for n in names if n not in known]))

    img = OUT / "img"
    img.mkdir(parents=True, exist_ok=True)
    for old in img.glob("*.jpg"):
        old.unlink()
    for name in names:
        for theme in ("light", "dark"):
            src = SHOTS / f"{name}-{theme}.png"
            if src.exists():
                subprocess.run(["magick", str(src), "-resize", "40%", "-quality", "82", str(img / f"{name}-{theme}.jpg")], check=True)

    stamp = f"{git('rev-parse', '--short', 'HEAD')} · {datetime.date.today().isoformat()}"
    sections = []
    for title, group in groups:
        if not group:
            continue
        figures = "\n".join(
            f"""      <figure class="screen" id="{n}">
        <div class="pair">
          <a class="light" href="img/{n}-light.jpg" target="_blank"><img src="img/{n}-light.jpg" alt="{n}, vaalea" loading="lazy"></a>
          <a class="dark" href="img/{n}-dark.jpg" target="_blank"><img src="img/{n}-dark.jpg" alt="{n}, tumma" loading="lazy"></a>
        </div>
        <figcaption>{html.escape(n)}</figcaption>
      </figure>"""
            for n in group
        )
        sections.append(f"""  <section>
    <h2>{html.escape(title)} <span class="count">{len(group)}</span></h2>
    <div class="grid">
{figures}
    </div>
  </section>""")

    page = TEMPLATE.replace("{{stamp}}", html.escape(stamp)).replace("{{count}}", str(len(names))).replace("{{sections}}", "\n".join(sections))
    (OUT / "index.html").write_text(page, encoding="utf-8", newline="\n")
    print(f"{len(names)} screens -> {OUT / 'index.html'}")


TEMPLATE = """<title>Rubikki Solveri näkymät</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;600&family=IBM+Plex+Mono:wght@400&display=swap">
<style>
  /* Layout: a contact sheet - every screen as a light/dark pair, grouped by area of the app. */
  :root {
    --bg: #f3f4f7; --surface: #ffffff; --fg: #1b1f27; --muted: #5d6573; --line: #d9dde5;
    --accent: #1d5fd0; --phone-shadow: rgba(20, 30, 50, .14);
    --sans: "IBM Plex Sans", system-ui, sans-serif; --mono: "IBM Plex Mono", ui-monospace, monospace;
  }
  @media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) {
    --bg: #12151b; --surface: #1b2029; --fg: #e7eaf0; --muted: #9aa3b2; --line: #2c3340;
    --accent: #7fa8ff; --phone-shadow: rgba(0, 0, 0, .5); color-scheme: dark } }
  :root[data-theme="dark"] {
    --bg: #12151b; --surface: #1b2029; --fg: #e7eaf0; --muted: #9aa3b2; --line: #2c3340;
    --accent: #7fa8ff; --phone-shadow: rgba(0, 0, 0, .5); color-scheme: dark }
  body { background: var(--bg); color: var(--fg); font: 15px/1.5 var(--sans); padding: 0 16px; }
  main { max-width: 1200px; margin: 0 auto; padding-block: 24px 48px; display: grid; gap: 36px; }
  header { display: flex; flex-wrap: wrap; gap: 12px 24px; align-items: end; justify-content: space-between; }
  h1 { font-size: 1.6rem; font-weight: 600; margin: 0; text-wrap: balance; }
  .meta { color: var(--muted); font: 13px var(--mono); }
  .themes { display: inline-flex; border: 1px solid var(--line); border-radius: 999px; overflow: hidden; background: var(--surface); }
  .themes button { font: 600 13px var(--sans); color: var(--muted); background: none; border: 0; padding: 8px 14px; cursor: pointer; min-height: 36px; }
  .themes button[aria-pressed="true"] { background: var(--accent); color: var(--surface); }
  .themes button:focus-visible { outline: 2px solid var(--accent); outline-offset: -2px; }
  section { display: grid; gap: 14px; }
  h2 { font-size: 1.05rem; font-weight: 600; margin: 0; letter-spacing: .02em; display: flex; gap: 8px; align-items: baseline; }
  .count { color: var(--muted); font: 12px var(--mono); }
  .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr)); gap: 20px; }
  .screen { margin: 0; display: grid; gap: 8px; }
  .pair { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  body.only-light .pair, body.only-dark .pair { grid-template-columns: minmax(0, 180px); }
  body.only-light .dark, body.only-dark .light { display: none; }
  .pair a { display: block; border-radius: 14px; overflow: hidden; box-shadow: 0 1px 2px var(--phone-shadow), 0 6px 18px var(--phone-shadow); border: 1px solid var(--line); }
  .pair img { display: block; width: 100%; height: auto; max-width: 100%; }
  figcaption { font: 13px var(--mono); color: var(--muted); }
</style>
<main>
  <header>
    <div>
      <h1>Rubikki Solveri näkymät</h1>
      <div class="meta">{{count}} näkymää · {{stamp}}</div>
    </div>
    <div class="themes" role="group" aria-label="Teema">
      <button type="button" id="t-both" aria-pressed="true">Molemmat</button>
      <button type="button" id="t-light" aria-pressed="false">Vaalea</button>
      <button type="button" id="t-dark" aria-pressed="false">Tumma</button>
    </div>
  </header>
{{sections}}
</main>
<script>
  const buttons = { both: 't-both', light: 't-light', dark: 't-dark' };
  function show(mode) {
    document.body.classList.toggle('only-light', mode === 'light');
    document.body.classList.toggle('only-dark', mode === 'dark');
    for (const [m, id] of Object.entries(buttons)) document.getElementById(id).setAttribute('aria-pressed', String(m === mode));
    try { localStorage.setItem('gallery-theme', mode); } catch (e) {}
  }
  for (const [m, id] of Object.entries(buttons)) document.getElementById(id).addEventListener('click', () => show(m));
  try { const saved = localStorage.getItem('gallery-theme'); if (saved in buttons) show(saved); } catch (e) {}
</script>
"""

if __name__ == "__main__":
    main()
