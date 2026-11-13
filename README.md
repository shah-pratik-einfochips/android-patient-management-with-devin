# PatientHealth (Android)

Native Android patient-health prototype built with Kotlin, Jetpack Compose/Material 3, Navigation Compose, Hilt, Room/KSP, Kotlin serialization, and coroutines.

> Local-only, synthetic-data prototype. Do not use real patient data. No backend/FHIR integration, authentication, or HIPAA/GDPR compliance is implemented.

## Requirements

- Android Studio with bundled JDK (tested with JDK 21)
- Android SDK; min API 26, compile/target API 36

## Getting started

Open this directory in Android Studio, select the `devDebug` variant, and run `app`.

For CLI builds, set `JAVA_HOME` (e.g. Android Studio's bundled JBR) and `ANDROID_HOME` first.

## Common commands

```sh
./gradlew :app:assembleDevDebug :app:lintDevDebug   # build and lint
./gradlew :app:testDevDebugUnitTest                 # unit tests
./gradlew :app:connectedDevDebugAndroidTest         # device tests (disposable emulator only)
./gradlew :app:domainCoverage                       # domain coverage report
./gradlew :app:installDevDebug                      # install
```

## Flavors

`dev`, `qa`, and `prod` flavors have separate application IDs and local stores. Release signing must be supplied by the project owner; never commit signing keys.

## Security

Room stores UUIDs and AES-256-GCM encrypted patient payloads only, with the key held in Android Keystore. Backup and device transfer are disabled, and the main activity sets `FLAG_SECURE`.

## Documentation

- `requirements.md` – product requirements
- `todo.md` – task list
- `AGENTS.md` – development notes
