# Design

## Context

See proposal.md. `AboutScreen` is one scrolling column: name, version, `about_text`, a licences
heading, three licence lines and min2phase's licence (raw resource, monospace). `LogScreen` shows
the file's lines as they are (`2026-10-03T08:09:51Z INFO scan.done …`, built by `LogLine.format`),
newest first, monospace, one colour.

## Goals / Non-Goals

**Goals:** About readable at a glance; log times that match the phone; errors visible at once.

**Non-Goals:** changing the log file format or what is logged; filtering or searching the log; a
separate licences screen (a route for one block of text is not worth it).

## Decisions

- **Licences behind a button, same screen.** The column shows name, version and description; below
  them an `OutlinedButton` "Avoimen lähdekoodin lisenssit" toggles the licence block (heading,
  three lines, min2phase text) open; the column scrolls only when it is open. The MIT licence must
  ship with the app, so it is kept, just folded. Alternative: drop it — rejected (licence terms).
- **Description.** `about_text` stays one sentence; it already says what the app does and that
  nothing leaves the phone. No change in wording.
- **Parsing a line for display.** `LogLine.parse(line): Parsed(time: Instant?, level: Level?, rest)`
  — splits off the first two tokens when they parse as an instant and a level; anything else
  (older or odd lines) is shown as is with no time and the info colour.
- **Time format.** `LogTime.format(instant, today, zone, locale)`: `ofLocalizedTime(MEDIUM)` for
  today's lines, `ofLocalizedDateTime(SHORT, MEDIUM)` otherwise, both `withLocale(locale)` and in
  `zone` (`ZoneId.systemDefault()`, the locale of the current configuration, which follows the
  app's language setting). Pure function with explicit zone/locale/today, so it is JVM-testable.
- **Look of a line.** Time in `labelSmall` muted, then the rest monospace 11 sp. Colour by level:
  ERROR `colorScheme.error`, WARN a fixed amber that reads in both themes (`#B26A00` light,
  `#FFB74D` dark), DEBUG `onSurfaceVariant`, INFO `onSurface`. The level word itself is not shown
  for INFO (it is the default) and shown for the others, so colour is not the only signal.

## Risks / Trade-offs

- Localized formats differ by Android version (e.g. Finnish uses "11.09.51"). → Tests assert with
  the JVM's formats for the given locale, not hard-coded strings beyond the spec's Finnish example.
- Hiding licences one tap away could be missed. → The button is visible without scrolling.
