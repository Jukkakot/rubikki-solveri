# Spec Delta

## MODIFIED Requirements

### Requirement: Learn mode in the solution screen
The start screen SHALL let the user choose between the shortest solution and learning step by
step. In learning mode the guide SHALL show the stage number and name with a small goal picture of
the stage (tapping it shows it large), and the current step's explanation above the move guide. When
a stage begins, a card with the stage's goal picture ("Next: …") SHALL be shown until the user
continues.

#### Scenario: Choose learning
- **WHEN** the user picks "learn step by step" on the start screen and taps "Aloita"
- **THEN** stage 1/7 "White cross" is shown with its first step

#### Scenario: Next stage
- **WHEN** the last step of the white cross is done
- **THEN** a card shows the goal picture of the white corners until the user continues
