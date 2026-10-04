## ADDED Requirements

### Requirement: Few whole-cube turns
When choosing how to place the next piece, the beginner solver SHALL prefer placements that need
no whole-cube turn, turning the top or bottom layer instead, and SHALL use a whole-cube turn only
when it saves many moves. Across random cubes, solutions SHALL need at least 40 % fewer
quarter whole-cube turns than before this requirement, apart from the fixed turns of the method
(white on top at the start, turning over for the middle layer).

#### Scenario: Piece already in reach
- **WHEN** a white corner can be placed in the front-right slot after turning the bottom layer
- **THEN** that placement is chosen rather than one that turns the whole cube

#### Scenario: Fewer turns overall
- **WHEN** 200 random cubes are solved with the beginner method
- **THEN** the average number of quarter whole-cube turns per solution (without the method's fixed turns) is at most 60 % of the earlier average, and every solution still solves its cube
