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

# Every screen, by area in the order a user meets them: (id, Finnish name, screens it leads to).
# The position gives the screen's number in the gallery ("7: nappi liian pieni"), so add new
# screens at the end of their area and keep the targets in step with ui/nav/RubikkiNavHost.kt.
GROUPS = [
    ("Aloitus", [
        ("home", "Koti", ["scan", "manual-input", "lessons", "timer", "free-cube", "settings", "log"]),
        ("settings", "Asetukset", ["about", "log"]),
        ("about", "Tietoja", []),
        ("log", "Loki", []),
    ]),
    ("Skannaus", [
        ("scan", "Skannaus", ["scan-review", "scan-permission", "manual-input"]),
        ("scan-review", "Puolen tarkistus", ["scan", "solve", "scan-check"]),
        ("scan-permission", "Kameralupa", ["manual-input"]),
        ("scan-check", "Tarkista värit", ["solve", "scan"]),
    ]),
    ("Käsin syöttö", [
        ("manual-input", "Syötä värit käsin", ["solve"]),
    ]),
    ("Ratkaisu", [
        ("solve", "Ratkaisu", ["solve-learn", "guide-back", "guide-right", "follow", "mid-turn", "home"]),
        ("solve-learn", "Ratkaisu: opettele", ["solve"]),
        ("guide-back", "Siirto takana", []),
        ("guide-right", "Siirto oikealla", []),
        ("follow", "Seuraa kameralla", []),
        ("mid-turn", "Siirto käynnissä", []),
    ]),
    ("Oppiminen", [
        ("lessons", "Oppitunnit", ["lesson"]),
        ("lesson", "Oppitunti", ["solve", "free-cube"]),
    ]),
    ("Ajanotto", [
        ("timer", "Ajanotto", ["history", "solve"]),
        ("history", "Historia", []),
    ]),
    ("Vapaa kuutio", [
        ("free-cube", "Vapaa kuutio", ["solve"]),
    ]),
]

# Sub-states drawn inside their parent's box in the navigation diagram, not as boxes of their own.
PARTS = {
    "solve": ["solve-learn", "guide-back", "guide-right", "follow", "mid-turn"],
}


def numbers(ns: list[int]) -> str:
    ns = sorted(ns)
    if len(ns) > 2 and ns[-1] - ns[0] == len(ns) - 1:
        return f"{ns[0]}–{ns[-1]}"
    return ", ".join(map(str, ns))


def diagram(groups, names: list[str], number: dict, title_of: dict) -> str:
    """Mermaid flowchart of every route: a subgraph per area, PARTS folded into their parent."""
    owner = {part: parent for parent, parts in PARTS.items() for part in parts if parent in names}
    lines = ["flowchart TD"]
    for i, (title, group) in enumerate(groups):
        boxes = [s for s in group if s[0] not in owner]
        if not boxes:
            continue
        alone = len(boxes) == 1  # a one-screen area needs no frame around it
        if not alone:
            lines.append(f'  subgraph g{i}["{title}"]')
        for sid, name, _ in boxes:
            label = f"<b>{number[sid]}</b> {name}"
            parts = [p for p in PARTS.get(sid, []) if p in names]
            if parts:
                part_names = ", ".join(title_of[p].split(": ")[-1].lower() for p in parts)
                label += f"<br/><small>{numbers([number[p] for p in parts])}: {part_names}</small>"
            lines.append(f'    n{number[sid]}["{label}"]')
        if not alone:
            lines.append("  end")
    edges = []
    for _, group in groups:
        for sid, _, targets in group:
            src = owner.get(sid, sid)
            for t in targets:
                dst = owner.get(t, t)
                if t in names and dst != src and (src, dst) not in edges:
                    edges.append((src, dst))
    # Back edges (a depth-first walk from the first screen meets them going back up its own path)
    # are dotted, so the forward routes read first.
    back, seen, path = set(), set(), []

    def walk(a: str) -> None:
        seen.add(a)
        path.append(a)
        for x, b in edges:
            if x == a:
                if b in path:
                    back.add((a, b))
                elif b not in seen:
                    walk(b)
        path.pop()

    for start, _ in edges:
        if start not in seen:
            walk(start)
    lines += [f"  n{number[a]} {'-.->' if (a, b) in back else '-->'} n{number[b]}" for a, b in edges]
    lines += [f'  click n{number[sid]} href "#{sid}"' for _, group in groups for sid, _, _ in group if sid not in owner]
    return "\n".join(lines)


def git(*args: str) -> str:
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True).stdout.strip()


def main() -> None:
    names = sorted({p.name.rsplit("-", 1)[0] for p in SHOTS.glob("*-light.png")})
    if not names:
        raise SystemExit(f"No screenshots in {SHOTS}; run the ScreenshotTest first.")
    screens = [screen for _, group in GROUPS for screen in group]
    number = {sid: i + 1 for i, (sid, _, _) in enumerate(screens)}
    title_of = {sid: name for sid, name, _ in screens}
    for n in names:
        if n not in number:
            number[n] = len(number) + 1
            title_of[n] = n
    groups = [(title, [s for s in group if s[0] in names]) for title, group in GROUPS]
    groups.append(("Muut", [(n, n, []) for n in names if n not in {sid for sid, _, _ in screens}]))

    def link(sid: str) -> str:
        return f'<a href="#{sid}"><b>{number[sid]}</b> {html.escape(title_of[sid])}</a>'

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
        <figcaption><span class="num">{number[n]}</span> {html.escape(title)} <code>{n}</code></figcaption>
        <div class="pair">
          <a class="light" href="img/{n}-light.jpg" target="_blank"><img src="img/{n}-light.jpg" alt="{n}, vaalea" loading="lazy"></a>
          <a class="dark" href="img/{n}-dark.jpg" target="_blank"><img src="img/{n}-dark.jpg" alt="{n}, tumma" loading="lazy"></a>
        </div>
        {('<p class="to">→ ' + ' · '.join(link(t) for t in targets if t in names) + '</p>') if any(t in names for t in targets) else ''}
      </figure>"""
            for n, title, targets in group
        )
        sections.append(f"""  <section>
    <h2>{html.escape(title)} <span class="count">{len(group)}</span></h2>
    <div class="grid">
{figures}
    </div>
  </section>""")

    page = (TEMPLATE.replace("{{stamp}}", html.escape(stamp)).replace("{{count}}", str(len(names)))
            .replace("{{diagram}}", diagram(groups, names, number, title_of)).replace("{{sections}}", "\n".join(sections)))
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
  .hint { margin: 6px 0 0; color: var(--muted); max-width: 65ch; }
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
  figcaption { font-weight: 600; display: flex; gap: 8px; align-items: baseline; flex-wrap: wrap; }
  figcaption code { font: 12px var(--mono); color: var(--muted); font-weight: 400; }
  .num { display: inline-grid; place-items: center; min-width: 28px; height: 28px; padding: 0 6px; border-radius: 999px;
    background: var(--accent); color: var(--surface); font: 600 14px var(--sans); font-variant-numeric: tabular-nums; }
  .to { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.7; }
  .to a { color: var(--fg); text-decoration: none; white-space: nowrap; border-bottom: 1px solid var(--line); }
  .to a:hover, .to a:focus-visible { color: var(--accent); border-color: var(--accent); }
  .to b { color: var(--accent); font-variant-numeric: tabular-nums; }
  .screen { scroll-margin-top: 16px; }
  .screen:target .pair a { outline: 3px solid var(--accent); outline-offset: 2px; }
  [hidden] { display: none !important; }
  .map-scroll { overflow-x: auto; background: var(--surface); border: 1px solid var(--line); border-radius: 14px; padding: 16px; }
  #map-svg svg { display: block; margin: 0 auto; min-width: 760px; }
  #map-svg .node { cursor: pointer; }
  #map-svg small { color: var(--muted); }
</style>
<main>
  <header>
    <div>
      <h1>Rubikki Solveri näkymät</h1>
      <div class="meta">{{count}} näkymää · {{stamp}}</div>
      <p class="hint">Viittaa näkymiin numerolla, esim. "7: nappi liian pieni". → kertoo mihin näkymästä pääsee.</p>
    </div>
    <div class="themes" role="group" aria-label="Teema">
      <button type="button" id="t-both" aria-pressed="true">Molemmat</button>
      <button type="button" id="t-light" aria-pressed="false">Vaalea</button>
      <button type="button" id="t-dark" aria-pressed="false">Tumma</button>
    </div>
  </header>
  <section id="map" hidden>
    <h2>Navigaatiokartta</h2>
    <div class="map-scroll"><div id="map-svg"></div></div>
  </section>
{{sections}}
</main>
<script type="text/plain" id="map-src">
{{diagram}}
</script>
<script type="module">
  // The diagram is extra: if Mermaid does not load, the section stays hidden and the "→" lines remain.
  const section = document.getElementById('map'), target = document.getElementById('map-svg');
  const src = document.getElementById('map-src').textContent;
  const css = name => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  let mermaid = null, seq = 0;
  try { mermaid = (await import('https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs')).default; } catch (e) {}
  async function draw() {
    if (!mermaid) return;
    mermaid.initialize({
      startOnLoad: false, securityLevel: 'loose', theme: 'base', fontFamily: css('--sans'),
      themeVariables: {
        background: css('--surface'), primaryColor: css('--surface'), primaryTextColor: css('--fg'),
        primaryBorderColor: css('--accent'), lineColor: css('--muted'), clusterBkg: css('--bg'),
        clusterBorder: css('--line'), titleColor: css('--fg'), fontSize: '14px',
      },
    });
    try {
      const { svg, bindFunctions } = await mermaid.render('map-graph-' + ++seq, src);
      target.innerHTML = svg;
      bindFunctions?.(target);
      section.hidden = false;
    } catch (e) { section.hidden = true; }
  }
  await draw();
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', draw);
</script>
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
