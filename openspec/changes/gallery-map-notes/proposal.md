## Why

The screen gallery shows each screen alone; the user cannot see how one gets from one screen to
another. Comments go through the page's general comment threads, which are clumsy for "one note
per screen" and awkward to hand over to Claude.

## What Changes

- A navigation map at the top of the gallery (a flow diagram of the screens and what leads where),
  and under every screen its "from here" and "comes from" links that jump to those screens.
- A note field under every screen. Notes are saved in the gallery page's own database (as the user
  types, after a short pause) so they survive reloads and devices, and Claude reads them directly
  ("lue kommentit"). Claude can mark a note handled with a short reply, shown under the note.
- A count of open notes at the top and a filter "only screens with notes".

No app behaviour changes (`skip_specs`). Touches `scripts/screen-gallery.py` and the docs.

## Capabilities

### New Capabilities

(none — developer tooling only)

### Modified Capabilities

(none)

## Impact

- `scripts/screen-gallery.py`: navigation table, diagram, links, note fields and their script.
- The gallery artifact gains the `db` capability (notes collection).
- `docs/development.md`, `.claude/CLAUDE.md`: how notes are read and answered.
