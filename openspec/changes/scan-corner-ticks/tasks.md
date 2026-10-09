# Tasks

## 1. Honest corners

- [ ] 1.1 `readCorners`: a corner is read when its three corner stickers and its three edges' stickers are clear (part of the clear cube, `clearAt`), and never all eight before `complete`; when only the complete flag is missing, the corner touching the open doubt (unsettled face, unclear sticker) stays unread
- [ ] 1.2 Tests: three corner stickers known but an edge not clear → not read; on every committed recording and fixture replay, all eight read never happens before `complete`, and every corner is read at finish
- [ ] 1.3 Status line: "Vielä N kulmaa" from the honest count; remove the zero-corners special case
- [ ] 1.4 Run all unit tests, build both apps; roadmap row 75 done; archive
