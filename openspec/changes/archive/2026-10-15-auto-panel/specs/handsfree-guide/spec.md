## MODIFIED Requirements

### Requirement: Start handsfree
Handsfree (shown to the user as "Automaattinen", English "Autoplay") SHALL be offered in the
shortest-solution guide (also to a target), not in camera follow, and not once the solve is finished. The
guide SHALL always open in the manual mode. A small "Auto" button beside the move words SHALL open a panel
from the bottom with what the mode does, the speed as a slider of five steps shown in seconds per move, and
"Käynnistä"; only "Käynnistä" SHALL start handsfree, at once and from the current move. Closing the panel
otherwise SHALL change nothing but the speed. The chosen speed SHALL be remembered between solves;
handsfree itself SHALL NOT be.

#### Scenario: Manual by default
- **WHEN** the user taps "Olen valmis" in the hold step
- **THEN** the guide opens at move 1 and waits for the user; nothing advances by itself

#### Scenario: Ready prompt waits
- **WHEN** the user taps "Auto"
- **THEN** the panel opens and nothing advances until the user taps "Käynnistä"

#### Scenario: Start from the guide
- **WHEN** the user taps "Käynnistä" in the panel
- **THEN** the panel closes and handsfree runs at once from the current move at the speed set in the panel

#### Scenario: Speed remembered
- **WHEN** the user sets the slider to 1,5 s per move and later opens the panel in another solve
- **THEN** the slider is at 1,5 s per move

### Requirement: Any touch stops handsfree
While handsfree runs, a band above the move words SHALL say that it runs and at what speed, and a touch
anywhere on the screen SHALL stop it and return to the normal guide on the same move; that touch SHALL do
nothing else (it does not confirm, go back or turn the view). The normal guide's demo repeat SHALL resume,
the band SHALL go and the wide button SHALL read "Seuraava" again. Leaving the screen SHALL end handsfree.
Handsfree SHALL start again through "Auto".

#### Scenario: Touch stops on the same move
- **WHEN** handsfree runs on move 5 and the user touches the screen
- **THEN** the guide is in the normal mode on move 5, the button reads "Seuraava", and it does not advance by itself

#### Scenario: Restart
- **WHEN** the user has stopped handsfree, taps "Auto" and then "Käynnistä"
- **THEN** handsfree continues from the current move at once
