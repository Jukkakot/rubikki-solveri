## 1. Log screen

- [ ] 1.1 `LogLine.parse` and `LogTime.format` (zone, locale, today); unit tests: a Finnish UTC+3 line from today shows 11.09.51, an earlier day shows date and time, an unparsable line comes back whole (`:app:testDebugUnitTest`)
- [ ] 1.2 `LogScreen` shows local time + rest, coloured by level (level word for non-INFO); Compose test: an error line and an info line render, the error carries the level word; screenshot `log` with an error and a warning line

## 2. About screen

- [ ] 2.1 `AboutScreen`: name, version, description, "Open-source licences" button that unfolds the licences; strings fi/en; `ShellTest.aboutShowsTheLicences` taps the button first; screenshots `about` (folded) and `about-licences` (open), gallery table updated (`:app:testDebugUnitTest`, lint)

## 3. Docs

- [ ] 3.1 `docs/architecture.md` (about, log viewer), `docs/operations.md` (reading the log on the phone), roadmap row 22 done; verify by reading
