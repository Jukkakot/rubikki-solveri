# Findings

`ColourSharesProbe` (`COLOUR_SHARES=1`), 2026-10-12, the scan as it ships (NEAREST warm references). Per picture,
every counting track's eight outer stickers: the leading colour (plurality of the readings' hard names) against the
shares' leader (the track's votes). "Judged": the track's true face and turn are known (leading colours and shares
fit the same true option with at least 6 of 8), so the true colour is known. "Close": the second share above 60 %
of the first.

| Source | Pictures | Sticker-pictures | Lead ≠ share | of them R/O | Judged | Lead right | Share right | Close | Lead wrong when close | Lead flips /100 pic | Share flips /100 pic |
|---|---|---|---|---|---|---|---|---|---|---|---|
| rec 100814 | 152 | 3992 | 0.1 % | 100 % | 5 | 1 | 4 | 0.8 % | 33 % | 2.0 | 3.3 |
| rec 100824 | 888 | 105608 | 0.0 % | 14 % | 29 | 23 | 5 | 0.9 % | 94 % | 2.4 | 1.9 |
| rec 102548 | 990 | 92904 | 0.3 % | 37 % | 186 | 4 | 175 | 0.5 % | 88 % | 3.5 | 3.9 |
| rec 100403 | 1337 | 125024 | 0.9 % | 22 % | 477 | 459 | 18 | 2.1 % | 23 % | 5.6 | 5.4 |
| rec 101618 | 1580 | 126600 | 1.0 % | 5 % | 233 | 56 | 177 | 1.6 % | 39 % | 2.0 | 1.4 |
| 151828 | 250 | 11976 | 0.0 % | 100 % | 3 | 1 | 2 | 1.7 % | 75 % | 6.0 | 5.2 |
| 151903 | 186 | 6112 | 0.0 % | - | 0 | 0 | 0 | 0.0 % | - | 0.0 | 0.0 |
| web_181940 (striped) | 780 | 103272 | 0.1 % | 45 % | 61 | 0 | 0 | 0.4 % | 51 % | 2.3 | 1.9 |
| web_121505 | 782 | 134856 | 0.4 % | 13 % | 167 | 124 | 43 | 1.2 % | 28 % | 36.8 | 35.8 |
| 213729 | 133 | 3752 | 0.0 % | - | 0 | 0 | 0 | 0.1 % | 50 % | 2.3 | 2.3 |
| 213817 | 143 | 6608 | 0.0 % | - | 0 | 0 | 0 | 0.1 % | 0 % | 2.1 | 2.1 |
| 213850 | 203 | 8376 | 0.6 % | 100 % | 52 | 52 | 0 | 1.5 % | 3 % | 1.0 | 1.5 |
| 213929 | 213 | 14048 | 0.0 % | - | 0 | 0 | 0 | 1.2 % | 81 % | 0.5 | 0.5 |

**What it shows:**

- The two disagree on at most 1 % of sticker-pictures (mostly well under). Where they do, only about a quarter is
  a red/orange case.
- When they disagree, neither is better: the leading colour is right 720 times, the shares' leader 424 times over all
  judged cases. The shares win on `102548` and `101618`, the leading colour on `100403`, `100824`, `213850` and
  `web_121505`; on the striped `web_181940` neither is right (61 cases, both wrong).
- The leading colour flips between pictures about as often as the shares' leader (e.g. `100403` 5.6 against 5.4,
  `web_121505` 36.8 against 35.8): the sway comes from the readings themselves, not from naming each picture hard.
- Stickers near a tie are 0–2 %; there the leading colour is wrong anything from 3 % to 94 % of the time, a sign that
  a near tie is a hard sticker either way, not one the shares would settle.

**Which decisions it would reach:** a face's turn settled, the whole-cube recheck, two tracks agreeing, the finish
check (`FaceTracks.fits`, `settleTurns`, `turnAll`, `turnsClear`, `updateVoting`, `sameReading`) and the warm
reference choice. All of them see a sticker's colour through `Track.leading`; at 0–1 % disagreement they would
decide the same nearly always.

**Outcome: no-go.** Moving those decisions onto the shares would change under 1 % of the stickers they look at, and
in those the shares are not more often right than the leading colour (424 against 720). The scan already keeps every
colour's likelihood where it counts most (votes, the best cube and its margins). Worth another look only with a
recording that stalls on a sticker whose leading colour and shares disagree.
