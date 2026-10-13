## ADDED Requirements

### Requirement: Learn the combinations
The start screen SHALL offer a third way beside the shortest solution and learning step by step: learning the
combinations. The cube SHALL be solved in the beginner method's stages, one step at a time, each step shown as which
piece goes to which place with which combination and how many times. The piece SHALL be outlined on the cube and its
place marked; turns of the top or bottom before the combination SHALL be said and shown as their own part; a change
of how the cube is held SHALL be its own instruction before the combination. Each combination SHALL link to its
lesson.

#### Scenario: White corner as a combination
- **WHEN** a step of the white corners is shown in this mode
- **THEN** the card names the corner by its colours, outlines it on the cube, marks its place and shows R' D' R D with how many times

#### Scenario: Cross edge
- **WHEN** a step of the white cross is shown
- **THEN** the card shows the edge and the few turns that bring it into place, with no combination

### Requirement: Combination animation without turning the cube
In the combinations mode, playing a combination SHALL turn only the cube's layers: the cube SHALL not turn as a
whole and the view SHALL not turn by itself during the combination, also where turning would show the pieces better.
The combination SHALL also be steppable move by move with the move guide.

#### Scenario: Play the trigger
- **WHEN** the user taps "Näytä" on a step with R' D' R D × 2
- **THEN** the eight layer turns play from the cube as it is, the cube does not turn as a whole and the view stays the same

#### Scenario: Hold change before a combination
- **WHEN** a step needs the cube turned so that another side faces the user
- **THEN** that turn is shown as its own instruction before the combination, not inside its animation
