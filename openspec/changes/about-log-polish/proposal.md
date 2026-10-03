# Proposal

## Why

Gallery feedback 3 and 4: the About screen is a long page of licence text when all the user wants
is the version and what the app is; the log shows raw UTC timestamps and every line looks alike, so
errors are hard to spot and the times do not match the phone's clock.

## What Changes

- About shows the app name, the version and the one-line description. The open-source parts and
  their licences move behind an "Open-source licences" button on the same screen (the MIT licence
  of min2phase still has to ship with the app, so it stays reachable, just not in the way).
- The log screen shows each line's time in the phone's time zone and the app language's date/time
  format (only the time for today's lines), and colours the line by level: errors red, warnings
  amber, debug muted, info plain.
- The log file itself is unchanged (UTC ISO times), so a shared log stays the same.

Modules: **app** only (About screen, log screen, a small line parser in the log package).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `about`: the about screen shows only name, version and description; licences behind a button.
- `diagnostics`: the log viewer shows local times and colours lines by level.

## Impact

- app: `AboutScreen`, `LogScreen`, new `LogLine.parse` (or similar) with unit tests, strings fi/en,
  `ShellTest.aboutShowsTheLicences`, screenshots `about`, `about-licences` (new) and `log`; gallery
  refreshed.
- No new dependencies.
