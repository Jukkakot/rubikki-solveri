# Rubikki Solveri – wiki

Rubikki Solveri is an Android app (Kotlin, Jetpack Compose, Material 3) that scans a real 3×3
Rubik's Cube and helps solve it: first the shortest solution move by move, later a human method
taught stage by stage. It runs fully offline on the author's phone. Everything you need to know
about the solution starts here.

## Where to find what

| Question | Go to |
|---|---|
| How is it built? How do the parts fit together? | [architecture.md](architecture.md) |
| How do I install it on the phone, read its log, debug it? | [operations.md](operations.md) |
| How do others get the app? Which free distribution routes exist? | [distribution.md](distribution.md) |
| How do I build, test and debug it on a computer? Conventions? | [development.md](development.md) |
| What exactly does the app do (requirements)? | [`openspec/specs/`](../openspec/specs/): one folder per capability |
| What has been decided but not built yet? | [`openspec/context/`](../openspec/context/): [product](../openspec/context/product.md), [nfr](../openspec/context/nfr.md), [roadmap](../openspec/context/roadmap.md) |
| What is being worked on now? | `openspec/changes/` (active changes) |
| Why was something done this way? | `openspec/changes/archive/`: proposal and design of every finished change |

## Sources of truth

1. **Code** is the truth for details.
2. **Specs** (`openspec/specs/`) are the truth for behaviour; a mismatch with code is a bug.
3. **This wiki** describes the solution as built, plus clearly marked planned parts. Every change
   keeps it current as part of its own task list, and archiving checks this.
4. **Context** (`openspec/context/`) holds decisions not yet specified; once a spec exists, the
   spec wins.

Write here what changes rarely (structure, flows, conventions, procedures), not what changes
constantly (function lists, UI texts) — that stays in code and specs. Sections describing
something not built yet are marked **Planned** with the roadmap item that delivers it.
