# Design

## Context

`scripts/screen-gallery.py` builds a static page from the screenshot test output; `GROUPS` holds
each screen's number, Finnish name and targets. The page is a claude.ai artifact, so external
scripts may only come from the allowed CDNs, and it must work at phone width in light and dark.

## Goals / Non-Goals

**Goals:** one diagram of all routes, generated from `GROUPS`; readable on a laptop and usable
(scrollable) on a phone; follows the page's light/dark choice.

**Non-Goals:** hand-tuned layout, edge labels, showing the sub-states' own transitions.

## Decisions

- **Mermaid from jsDelivr** (`mermaid@11` ESM). The source is written into a
  `<script type="text/plain">` block and rendered on load, so a failed load shows nothing broken;
  the section is hidden and the "→" lines still work. Alternative (pre-rendered SVG with the
  Mermaid CLI) would need Node tooling in the build; not worth it for a review page.
- **Node ids `n<number>`**, label "number name". Screen ids like `end`-style words or hyphens
  cannot break Mermaid syntax that way.
- **Sub-states:** a `PARTS = {"solve": [...]}` table next to `GROUPS`. A part is drawn inside its
  parent's box ("11 Ratkaisu" + a second line "12–16 opettele, siirto takana …"); edges between a
  parent and its parts are dropped, edges from parts to other screens move to the parent. Keeping
  it a separate table (not a rule like "no targets") stops about/log/history from folding too.
- **One subgraph per area**, `flowchart TD`; Mermaid lays it out. All edges, also the back ones
  (scan-review → scan), because that is the real map.
- **Clicks:** `click n7 href "#scan-check"`, which needs `securityLevel: "loose"`; the source is our
  own table, so that is safe.
- **Theme:** Mermaid `base` theme with `themeVariables` read from the page's CSS tokens; it
  re-renders when the system light/dark setting changes (the page has no theme switch of its own; its buttons only pick which screenshots show).
- **Phone:** the diagram sits in a container with its own horizontal scroll; the page never
  scrolls sideways.

## Risks / Trade-offs

- Mermaid's automatic layout with subgraphs can cross arrows → accepted; if it reads badly, a later
  change can switch direction or drop back edges.
- A CDN outage leaves the page without the diagram → the rest of the page is unaffected.
