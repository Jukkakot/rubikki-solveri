# Proposal

## Why

The scan already counts each sticker by its colour shares (votes, best cube), but several side decisions take a
sticker's single leading colour: a face's turn settled, the whole-cube recheck, two tracks agreeing, the finish
check, which centres may be references. The leading colour is a plurality of each picture's hard name
(`Track.updateLeading`), so a sticker 52 % orange and 48 % red in every picture leads orange with every vote. The
user (2026-10-12) wants the scan to keep every colour's likelihood. Before changing the decisions, measure whether
the leading colour decides differently from the shares often enough, and wrongly enough, to be worth it.

## What Changes

- A probe in `cube` jvmTest (`COLOUR_SHARES=1`), run over every recording and video fixture with a known cube:
  per picture and counting track sticker, the leading colour against the shares' leader and the truth; how often
  they differ (and how often red/orange), which is right when they do, how many stickers are near a tie, and how
  often each flips between pictures.
- Findings in `findings.md` and a go/no-go for a change that moves the decisions onto the shares.
- No change to the app's behaviour.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None: a measurement only (`skip_specs`).

## Impact

`cube` jvmTest only (the probe reads the scan's internals by reflection). `app`, `shared` and `web` untouched.
