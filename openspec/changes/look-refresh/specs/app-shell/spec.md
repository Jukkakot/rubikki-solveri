## MODIFIED Requirements

### Requirement: Theme
The app SHALL use its own look ("Karkki": warm and playful, rounded shapes and fonts, a blue
primary and an orange accent) with designed light and dark variants. Its colours SHALL be the
same on every phone and SHALL NOT be taken from the phone's wallpaper. It SHALL follow the phone's
dark mode unless the user forces light or dark in settings; the choice SHALL persist across
restarts.

#### Scenario: Follow the phone
- **WHEN** the theme setting is "follow the phone" and the phone is in dark mode
- **THEN** the app is dark

#### Scenario: Forced light
- **WHEN** the user picks light in settings while the phone is in dark mode
- **THEN** the app is light and stays light after a restart

#### Scenario: Same colours on every phone
- **WHEN** the app runs on two phones with different wallpapers, both in light mode
- **THEN** the app's colours are the same on both
