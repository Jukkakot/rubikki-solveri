## Why

The user gives UI feedback by voice and in chat. Screen names like `scan-check` are awkward to
say, and the gallery does not show how one screen leads to another.

## What Changes

- Every screen in the gallery gets a fixed number and a Finnish name ("7 Tarkista värit"), so
  feedback can say "7: nappi liian pieni".
- Under every screen a line "→ 3 Skannaus · 5 Käsin syöttö …" lists the screens it leads to; the
  numbers link to those screens.

No app behaviour changes (`skip_specs`). Only `scripts/screen-gallery.py` and the docs.

## Capabilities

### New Capabilities

(none — developer tooling only)

### Modified Capabilities

(none)

## Impact

- `scripts/screen-gallery.py`: screen table with number, name and targets.
- `docs/development.md`: a new screen or route gets a line in that table.
