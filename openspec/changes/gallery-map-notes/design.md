## Context

`scripts/screen-gallery.py` writes a static page from the screenshot test's images; Claude
publishes it to https://claude.ai/artifact/FWqmj6ZeSBgK4qiQFXZyRR. Navigation lives in
`ui/nav/RubikkiNavHost.kt` (routes and the calls that open them).

## Goals / Non-Goals

**Goals:** see the app's flow at a glance; one note per screen that Claude reads without the user
copying anything.

**Non-Goals:** deriving the map from the code automatically (a parser of the nav graph costs more
than the table); threads, several notes per screen, other people's notes.

## Decisions

- **Navigation as a small table in the script** (`NAV`: from-screen → [(to-screen, label)]),
  written from `RubikkiNavHost`. Solve sub-states (guide-back, guide-right, follow, mid-turn) are
  listed as states of the solve screen. A new route means one new line; the docs say so.
- **Diagram** with Mermaid (`<pre class="mermaid">`, rendered natively by the artifact viewer,
  no library), `flowchart LR`, nodes labelled with the Finnish screen names. Per screen, chips
  "→ to" and "← from" link to the screen's anchor.
- **Notes in `db`**: collection `notes`, one document per screen id:
  `{text, updatedAt, reply?, handled?}`. The page subscribes once to the collection, renders each
  note into its textarea (unless the field has focus), and writes `set` on a 800 ms pause after
  typing, one write at a time per document. Empty text deletes the document. A handled note shows
  Claude's `reply` under the field; editing the text again clears `handled`. Default rules (owner
  writes, signed-in viewers read) are enough: only the user writes.
- **Without the database** (`claude.use("db")` resolves `null`): the fields stay disabled with a
  one-line note that notes need a signed-in claude.ai view.
- **Claude's side**: `ArtifactData list notes` reads them; after acting on one, Claude `update`s it
  with `handled: true, reply: "<what was done>"`. Republishing the page keeps the notes.

## Risks / Trade-offs

- [The NAV table drifts from the code] → it is next to the screen list the script already keeps;
  changes adding a route update both.
- [Mermaid layout gets crowded with 20 nodes] → solve sub-states are folded into the solve node.
