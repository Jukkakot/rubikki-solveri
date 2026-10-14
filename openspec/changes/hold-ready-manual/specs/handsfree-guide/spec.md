## MODIFIED Requirements

### Requirement: Start handsfree
Handsfree (shown to the user as "Automaattinen", English "Autoplay") SHALL be offered in the
shortest-solution guide (also to a target), not in camera follow, and not once the solve is finished. The
guide SHALL always open in the manual mode; handsfree SHALL start only from ▶ in the guide, at once and
with no ready prompt. The speed (slow, normal, fast) SHALL be chosen in the guide's ⋮ menu. The chosen speed
SHALL be remembered between solves; handsfree itself SHALL NOT be.

#### Scenario: Manual by default
- **WHEN** the user taps "Olen valmis" in the hold step
- **THEN** the guide opens at move 1 and waits for the user; nothing advances by itself

#### Scenario: Ready prompt waits
- **WHEN** the user taps ▶ in the guide on move 1
- **THEN** no ready prompt is shown: handsfree runs at once at the chosen speed

#### Scenario: Start from the guide
- **WHEN** the user taps ▶ in the guide
- **THEN** handsfree runs at once from the current move at the remembered speed

#### Scenario: Speed remembered
- **WHEN** the user picks "fast" in the guide menu and later opens the guide of another solve
- **THEN** "fast" is marked in the menu and ▶ runs at the fast speed
