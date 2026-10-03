## 1. Gallery

- [ ] 1.1 `NAV` table from `RubikkiNavHost`; Mermaid flow diagram at the top; "→ / ←" link chips per screen; verify by building the page and checking the anchors
- [ ] 1.2 Note field per screen backed by `db` (`notes/<screen>`), debounced writes, handled replies, open-note count and "only with notes" filter, disabled state without `db`; verify by publishing with `capabilities: {db: {}, user: {}}` and one `ArtifactData list notes`

## 2. Docs

- [ ] 2.1 `docs/development.md` and `.claude/CLAUDE.md`: reading and answering notes, adding a route to `NAV`; roadmap row 19; verify by reading
