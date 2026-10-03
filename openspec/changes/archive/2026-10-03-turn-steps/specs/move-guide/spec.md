## MODIFIED Requirements

### Requirement: Haptics
Confirming a move SHALL give a short confirming vibration, and the end of a demo a light tick,
using the phone's standard haptic patterns. A demo of a half turn SHALL tick after each of its
two quarter steps.

#### Scenario: Done vibrates
- **WHEN** the user taps done
- **THEN** the phone gives a confirming vibration

#### Scenario: Half turn demo ticks twice
- **WHEN** a half turn is demoed
- **THEN** the phone gives a light tick after the first quarter step and another at the end
