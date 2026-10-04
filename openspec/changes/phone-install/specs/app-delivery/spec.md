## ADDED Requirements

### Requirement: Latest build at a fixed address
Every push to the main branch whose checks pass SHALL publish an installable, signed release APK
of that commit as the repository's latest release, downloadable at the fixed address
`https://github.com/Jukkakot/rubikki-solveri/releases/latest/download/rubikki-solveri.apk`. The
release SHALL be named after the version (`1.0.<count>-<commit>`) and its notes SHALL list the
commit's subject. Only one such rolling release SHALL exist; a newer build replaces it.

#### Scenario: Push publishes
- **WHEN** a commit is pushed to main and the checks pass
- **THEN** the fixed address serves the APK of that commit within the same CI run

#### Scenario: Failing checks
- **WHEN** a pushed commit fails the tests or lint
- **THEN** nothing is published and the address keeps serving the previous build

### Requirement: Updates keep the installed app
The published APK SHALL be signed with the same key as the builds the user installs from Android
Studio, so that installing it over an existing install (and a later Android Studio build over it)
is an in-place update that keeps the app's data. Version codes SHALL keep increasing with every
commit so each newer build updates an older one.

#### Scenario: Download over a Studio install
- **WHEN** the app was installed with Android Studio's Run and the user installs the downloaded APK of a newer commit
- **THEN** Android offers "Update", and the solve history is still there afterwards

### Requirement: No publishing without the key
When the signing key is not configured in the repository, CI SHALL NOT publish the APK, and the run
summary SHALL say that publishing was skipped and which secret is missing. The checks themselves
SHALL still run.

#### Scenario: Secret missing
- **WHEN** a commit is pushed to main and the signing key secret is not set
- **THEN** the tests run, no release is created or changed, and the run summary names the missing secret
