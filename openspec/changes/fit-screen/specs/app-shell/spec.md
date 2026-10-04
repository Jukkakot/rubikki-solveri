## ADDED Requirements

### Requirement: Screens fit without scrolling
On the solution screen (both methods, including camera follow) and the free cube screen, the
screen's main actions and the text that goes with them SHALL be visible without scrolling, in
the phone app and in the phone's browser. The 3D cube (and the camera view in camera follow) SHALL
shrink to the height that is left. Only when that height would make the cube too small to read
SHALL the screen scroll instead.

#### Scenario: Short browser window
- **WHEN** the solution screen is shown in a phone browser with about 560 dp of height
- **THEN** the move in words and the "Done" button are visible without scrolling, and the cube is smaller than the screen width

#### Scenario: Tall phone
- **WHEN** the solution screen is shown on a tall phone where everything fits at full width
- **THEN** the cube is as wide as before

#### Scenario: Very short screen
- **WHEN** the leftover height for the cube is too small to read it (landscape phone)
- **THEN** the screen scrolls and the cube keeps a readable size
