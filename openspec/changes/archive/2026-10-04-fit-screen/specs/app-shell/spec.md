## ADDED Requirements

### Requirement: Screens fit without scrolling
In portrait, every screen with actions (solution in both methods including camera follow, free
cube, scan, timer, lessons) SHALL show all its content and actions without scrolling, in the phone
app and in the phone's browser. Spacing and secondary text SHALL be compact; the screen's big
element (3D cube, camera view, timer area, lesson picture) SHALL take the height that is left and
SHALL NOT grow larger than its full width allows. During a solution the cube's size SHALL NOT
change from move to move. Only when the big element would become too small to use SHALL the screen
scroll instead. Settings and About MAY scroll; landscape is unchanged. The learn method's
solution screen MAY scroll on short screens (its stage card leaves the cube too little height).

#### Scenario: Short browser window
- **WHEN** the solution screen is shown in a phone browser with about 560 dp of height
- **THEN** the move in words and the "Done" button are visible without scrolling, and the cube is smaller than the screen width

#### Scenario: Tall phone
- **WHEN** the solution screen is shown on a tall phone where everything fits at full width
- **THEN** the cube is as wide as before

#### Scenario: Same size every move
- **WHEN** the user steps through the moves of a solution
- **THEN** the cube keeps the same size

#### Scenario: Very short screen
- **WHEN** the leftover height for the big element would be under 200 dp
- **THEN** the screen scrolls and the element keeps a usable size
