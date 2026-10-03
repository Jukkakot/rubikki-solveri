## Why

The "→" lines under each screen tell where that one screen leads, but not how the whole app hangs
together. For the coming layout round the user wants to see the navigation at a glance and point
at it by number.

## What Changes

- A navigation diagram at the top of the gallery: one box per screen ("7 Tarkista värit"), arrows
  for the "→" links, boxes grouped by area. It is drawn with Mermaid from the same `GROUPS` table,
  so it never drifts from the captions.
- The solve screen's sub-states (opettele, siirto takana, siirto oikealla, seuraa kameralla,
  siirto käynnissä) are one "Ratkaisu" box that lists their numbers, not five boxes of their own.
- Clicking a box jumps to that screen in the gallery.

No app behaviour changes (`skip_specs`). Only `scripts/screen-gallery.py` and the docs.

## Capabilities

### New Capabilities

(none — developer tooling only)

### Modified Capabilities

(none)

## Impact

- `scripts/screen-gallery.py`: Mermaid source built from `GROUPS`, a table of sub-states folded
  into their parent, the diagram section in the page template.
- `docs/development.md`: the diagram and the sub-state table.
