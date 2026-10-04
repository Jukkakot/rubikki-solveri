# web-app Specification

## Purpose
TBD - created by archiving change web-app. Update Purpose after archive.

## Requirements

### Requirement: Browser version at a fixed address
The app SHALL be available as a web page at `https://jukkakot.github.io/rubikki-solveri/`. Every
push to the main branch whose checks pass SHALL publish that commit's browser build there. The
version shown in the browser (home screen and about) SHALL use the same `1.0.<count>-<commit>`
form as the phone app, followed by the build date and time instead of the install time.

#### Scenario: Push updates the page
- **WHEN** a commit is pushed to main and the checks pass
- **THEN** after the publishing run the address serves that commit's build, and its home screen shows the new version

#### Scenario: Failing checks
- **WHEN** a pushed commit fails the checks
- **THEN** the address keeps serving the previous build

### Requirement: Same features as the phone app
The browser version SHALL offer every feature of the phone app with the behaviour its
capabilities specify (app-shell, about, cube-model, cube-view, manual-input, camera-scan,
fast-solve, move-guide, camera-follow, beginner-solver, lessons, progress, diagnostics), except
where this capability states a browser-specific rule. The texts, screens and the 3D cube SHALL look
the same as on the phone, apart from the colours (see Theme in the browser).

#### Scenario: Solve from manual input
- **WHEN** the user enters a valid cube by hand in the browser and asks for the solution
- **THEN** the same shortest solution as on the phone is shown and can be stepped through with the animated 3D guide

#### Scenario: Learn a stage
- **WHEN** the user opens a lesson in the browser and starts its practice
- **THEN** the lesson pages and the practice position behave as on the phone

### Requirement: Supported browsers
The browser version SHALL work in current Chrome (including Chrome on Android), Edge, Firefox and
Safari (18.2 or newer). In a browser that cannot run it, the page SHALL show a short message in
Finnish and English saying the browser is too old, instead of a blank page. While the app loads,
the page SHALL show a loading note.

#### Scenario: Old browser
- **WHEN** the page is opened in a browser without the required WebAssembly support
- **THEN** a message says the browser is too old to run the app

#### Scenario: Loading
- **WHEN** the page is opened for the first time on a slow connection
- **THEN** a loading note is shown until the home screen appears

### Requirement: Install and offline use
The page SHALL be installable as a home-screen app (with the app's name and cube icon, opening
full screen without the browser bar). After one complete visit it SHALL open and work without a
network connection. A new build SHALL be taken into use at the latest on the second start after it
was published.

#### Scenario: Add to home screen
- **WHEN** the user picks "Add to home screen" / "Install app" in the phone's browser
- **THEN** an icon with the app's name appears and opens the app full screen

#### Scenario: Offline
- **WHEN** the user has opened the app once and later opens it without a network connection
- **THEN** the app opens and every feature works, including solving and the camera

### Requirement: Data stays in the browser
Settings, solves, practice, the log and the scan pictures SHALL be stored in the browser, survive
reloads and restarts, and never be sent anywhere. They SHALL be separate from the phone app's data.
The app SHALL ask the browser to keep its storage persistently.

#### Scenario: Reload keeps solves
- **WHEN** the user records a timed solve and reloads the page
- **THEN** the solve is still in the history and in the statistics

#### Scenario: Separate from the phone
- **WHEN** the user has solves in the phone app and opens the browser version for the first time
- **THEN** the browser version's history is empty

### Requirement: Language in the browser
The browser version SHALL start in Finnish regardless of the browser's language. Picking another
language in settings SHALL switch all texts by reloading the app and SHALL persist.

#### Scenario: Switch to English
- **WHEN** the user picks English in the browser version's settings
- **THEN** the app reloads in English and stays English on later visits

### Requirement: Theme in the browser
The browser version SHALL use fixed Karkki colours (the same light and dark colour schemes the phone
app uses when dynamic colours are unavailable), with the same fonts and shapes. Light or dark SHALL
follow the device's setting unless the user forces one in settings; the scan screens SHALL always
be dark.

#### Scenario: Dark device
- **WHEN** the device is in dark mode and the theme setting follows the device
- **THEN** the browser version is dark

### Requirement: Camera in the browser
The scan and camera follow SHALL use the device's back camera when there is one, else any camera.
When the device has several back cameras and the browser's choice has no torch, the other back
cameras SHALL be tried once and the first with a torch used; the camera chosen SHALL be remembered
for later scans and named in the log.
The browser's camera permission prompt SHALL be shown when the user opens the scan; if access is
denied, the screen SHALL explain how to allow the camera in the browser's site settings and offer
manual input instead. The torch button SHALL be shown only when the camera supports a torch. When
the camera supports it, exposure and white balance SHALL be locked at the first capture as on the
phone; when it does not, the scan SHALL work without the lock and the log SHALL say the lock is not
supported.

#### Scenario: Scan in the phone's browser
- **WHEN** the user opens the scan in Chrome on the phone and allows the camera
- **THEN** the back camera's picture fills the scan view with the grid and the live colour dots, and faces are captured and checked as in the phone app

#### Scenario: Back camera with a torch
- **WHEN** the phone has several back cameras and the browser opens one without a torch
- **THEN** the scan switches to a back camera that has one, shows the torch button, and opens that camera directly next time

#### Scenario: Camera denied
- **WHEN** the user blocks the camera for the page
- **THEN** the scan screen explains how to allow it in the browser's site settings and offers manual input

#### Scenario: No torch
- **WHEN** the camera has no torch the browser can control (e.g. a laptop webcam)
- **THEN** no torch button is shown

### Requirement: Back button in the browser
The browser's back action (back button, back gesture, Android back) SHALL do what the app's back
does on that screen. Reloading the page SHALL never show a broken page: it reopens the same screen
or, if that is not possible, the home screen.

#### Scenario: Back from settings
- **WHEN** the user opens settings and presses the browser's back button
- **THEN** the home screen is shown

### Requirement: Vibration in the browser
Where the browser supports vibration, the moments that vibrate on the phone (move done, demo
ticks, capture, follow-along advance) SHALL give a short vibration; elsewhere they SHALL be silent
without any error.

#### Scenario: Done on an iPhone
- **WHEN** the user confirms a move in Safari, which cannot vibrate
- **THEN** the guide advances as usual and nothing fails

### Requirement: Sharing the log in the browser
The log screen's share action SHALL open the device's share sheet with the log file and the newest
nine scan pictures (browsers share at most ten files) when the browser can share files. When the browser cannot share files or refuses the
share (other than the user cancelling it), one zip file with the log and the scan pictures SHALL
be downloaded instead. The outcome and the browser's refusal SHALL be written to the log. The
newest 12 scan pictures SHALL be kept in the browser.

#### Scenario: Share on the phone's browser
- **WHEN** the user taps share on the log screen in Chrome on Android
- **THEN** the share sheet opens with the log file and the newest nine scan pictures

#### Scenario: Share on a computer
- **WHEN** the user taps share on the log screen in a desktop browser that cannot share files
- **THEN** a zip with the log and the scan pictures is downloaded

#### Scenario: Browser refuses the share
- **WHEN** the browser says it can share files but refuses the share
- **THEN** a zip with the log and the scan pictures is downloaded and the refusal is written to the log

#### Scenario: User cancels
- **WHEN** the user closes the share sheet without choosing an app
- **THEN** nothing is downloaded

### Requirement: Keyboard and wide screens
On a screen wider than a phone, the app SHALL be shown as a centred phone-width column that uses
the full height. On the timer screen, holding and releasing the space bar SHALL work like holding
and releasing a finger on the screen, and pressing it while the timer runs SHALL stop it.

#### Scenario: Laptop
- **WHEN** the app is opened in a desktop browser window wider than a phone
- **THEN** the screens appear in a centred column of phone width with the full window height

#### Scenario: Space bar timer
- **WHEN** the user holds the space bar until the timer is ready, releases it, and presses it again
- **THEN** the timer starts on release and stops on the press

### Requirement: Motion and about in the browser
When the device asks for reduced motion, cube moves SHALL appear at once, as on a phone with
animations off. The about screen in the browser SHALL say that everything stays in this browser,
and its download button SHALL lead to the latest Android APK.

#### Scenario: Reduced motion
- **WHEN** the device's reduced-motion setting is on and a move is played
- **THEN** the cube shows the result at once without the turning animation

### Requirement: Speed in the browser
In Chrome on the reference phone, the shortest solution SHALL appear within two seconds of
opening the solution screen, and the beginner solution within one second. The one-time
preparation of the solver after start SHALL NOT delay the home screen's first appearance.

#### Scenario: Solve in the browser
- **WHEN** the user opens the solution of a scanned cube in the phone's browser after the app has been open a few seconds
- **THEN** the first move is shown within two seconds
