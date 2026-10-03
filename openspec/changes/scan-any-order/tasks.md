## 1. Cube module

- [ ] 1.1 Face rotation helpers (net positions turned k quarter turns) and `RotationSearch` (4096 combinations, distinct valid cubes, piece score, opposite-pair renames, ambiguous faces); tests: a scrambled cube with faces turned is restored, the phone regression scan with turned faces gives the same cube, a misread sticker still gives the right rotations, a red/orange label swap is undone, the solved cube is not reported ambiguous (`./gradlew :cube:test`)
- [ ] 1.2 `ScanSession` without fixed order: recognition among faces not done, `accept(face)`, already-scanned check in any rotation, redo of the last accepted, no suggested face, one-face mode kept; `outcome()` uses `RotationSearch`, readings turned to net order, `rotations`; existing session tests updated (`:cube:test`)
- [ ] 1.3 `ScanCheck.replaceFace` tries the four rotations and returns the one used; test: a quarter-turned rescan comes back right (`:cube:test`)

## 2. App

- [ ] 2.1 Scan screen: title "Kuvattu n/6", "any face, any way round" hint (no suggestion, no hold hint), live "looks like" by face, review with the recognised face and the centre-colour picker (44 dp), progress dots by face, already-scanned status; picture turning into `LastScan`; one-face rescan turns its picture; strings fi/en; Compose tests: another face first, changing the recognised face, already-scanned face (`:app:testDebugUnitTest`, lint)
- [ ] 2.2 Screenshots `scan` and `scan-review` updated (review shows the picker); gallery refreshed after the push

## 3. Docs

- [ ] 3.1 `docs/architecture.md` (scan pipeline: recognition, rotation search), `docs/operations.md` if log fields change, `product.md` later ideas (video), roadmap row for `scan-any-order` done; verify by reading
