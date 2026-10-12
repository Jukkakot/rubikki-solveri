---
name: general-fixes-only
description: Fix scan problems generally, never with special cases for one cube pattern (e.g. the striped cube)
metadata:
  type: feedback
---

A problem found with a particular cube or pattern (the striped cube, 2026-10-12) is fixed by a rule that helps every
cube, never by a check that recognises that pattern (user 2026-10-12: "älä tee mitään raitakuutio-spesifistä
korjausta vaan sellainen mikä auttaa yleisesti").

**Why:** pattern-specific code piles up and hides the general weakness; the special cube only exposes it.

**How to apply:** state the general weakness first (e.g. a lattice across a cube edge, a finish while the stickers
that tell two possible cubes apart are unsure) and design the fix for that; use the special cube's recordings as
test material, and check the fix on all other recordings too.
