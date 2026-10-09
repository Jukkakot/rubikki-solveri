# Tasks

## 1. Next view in cube

- [ ] 1.1 `NextView.choose(state, previous)`: corners and sides scored (design 1), steadiness (design 2), none while sides unread or once clear
- [ ] 1.2 Tests: the missing white–red–blue corner is chosen; a single side wins when it holds the doubt; the choice holds under small score changes; none when clear; on the committed recordings the chosen view always contains an unknown or doubtful sticker
- [ ] 1.3 State carries the next view (also through `ScanStateCodec` for the browser); `next=` in log snapshots

## 2. Screen

- [ ] 2.1 Ring: segment filled by known stickers / 9, full when confirmed; chosen sides pulse
- [ ] 2.2 Status line: "Näytä kulma" / "Näytä sivu" with pulsing colour dots once every side is read (fi, en)
- [ ] 2.3 `TurnDemo`: known stickers in colour, unknown grey, needed ones blink, turning the chosen view to the camera; unchanged before every side is read
- [ ] 2.4 Tests: ring fill per side; status line shows dots for the chosen corner; demo gets the needed stickers

## 3. Check, docs

- [ ] 3.1 Build both apps, run all unit tests
- [ ] 3.2 `docs/architecture.md` scan overlay map; roadmap row 74 `scan-next-view` done
