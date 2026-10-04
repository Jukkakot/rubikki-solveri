## REMOVED Requirements

### Requirement: View follows the move
**Reason**: Swinging the view looks like the cube being turned; the user then has to work out the
orientation again after every left, back or bottom move.
**Migration**: Replaced by "Steady view" and "Mirror for hidden sides" below.

## ADDED Requirements

### Requirement: Steady view
The guide cube SHALL always be shown in the holding position (white on top, green in front, seen
from the front a little from the right and above), for every move. The view SHALL never turn by
itself; only the user's drag turns it.

#### Scenario: Back move
- **WHEN** a turn of the back is presented
- **THEN** the cube stays in the same view as for the previous move, with the back layer highlighted

#### Scenario: Sequence of moves
- **WHEN** the user steps through R, B, L and D
- **THEN** the cube's view is the same for all four moves

### Requirement: Mirror for hidden sides
While a move of the left, back or bottom side is presented or animates, a small mirror cube SHALL
be shown next to the guide cube, showing that side as a mirror placed on that side would (left
and right as the user sees them), with the same highlight, direction arrow and animation as the
main cube. It SHALL NOT respond to drags. For moves of the top, front and right it SHALL be hidden.

#### Scenario: Back move in the mirror
- **WHEN** a clockwise turn of the back is presented
- **THEN** the mirror shows the back face with its arrow, and the arrow's direction matches the turn as seen in a mirror behind the cube

#### Scenario: No mirror for visible sides
- **WHEN** a turn of the right side is presented
- **THEN** no mirror cube is shown

### Requirement: Reset orientation
After the user has dragged the guide cube to another view, a button SHALL appear on the cube that
turns it back to the holding view with an animation. The button SHALL be hidden while the cube is
in the holding view.

#### Scenario: Drag and reset
- **WHEN** the user drags the cube round and taps the reset button
- **THEN** the cube animates back to the holding view and the button disappears

#### Scenario: Next move keeps the user's view
- **WHEN** the user has dragged the cube and steps to the next move
- **THEN** the view stays where the user left it and the reset button stays visible

### Requirement: Whole-cube turn shown by its centres
When the presented move turns the whole cube, the guide SHALL show the centre colours that end up
in front and on top as large colour dots next to the words.

#### Scenario: Turn to red
- **WHEN** a whole-cube turn that brings red to the front is presented
- **THEN** a red dot marks the front and a white dot the top
