# Native Android project

- Open this directory in Android Studio. Select the `devDebug` variant and run `app`.
- Kotlin, Jetpack Compose/Material 3, Navigation Compose, Hilt, Room/KSP, Kotlin serialization, and coroutines are managed through `gradle/libs.versions.toml`. Minimum Android API: 26; compile/target API: 36. Java/Kotlin bytecode target: 17.
- The Gradle wrapper is included. Use Android Studio's bundled JDK (tested with JDK 21) and an installed Android SDK. On this workstation, export `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and `ANDROID_HOME="$HOME/Library/Android/sdk"` before CLI builds. Android Studio can generate its own ignored `local.properties`.
- Build and lint: `./gradlew :app:assembleDevDebug :app:lintDevDebug`.
- Unit tests: `./gradlew :app:testDevDebugUnitTest`.
- Device tests: use a dedicated disposable emulator, then run `./gradlew :app:connectedDevDebugAndroidTest`. The test harness uninstalls the target app afterward, so never run connected tests against an installation containing data you need to retain. Reinstall with `:app:installDevDebug` after tests. Storage tests use an isolated in-memory Room database; UI tests use synthetic form data, and the startup test opens the real activity.
- Domain coverage: `./gradlew :app:domainCoverage`. HTML output is under `app/build/reports/jacoco/domainCoverage/html/`.
- Install: `./gradlew :app:installDevDebug`. Launch: `adb shell am start -n com.example.patienthealth.dev/com.example.patienthealth.MainActivity`.
- `dev`, `qa`, and `prod` flavors have separate application IDs/local stores. Release signing and a production application ID must be supplied by the project owner before distribution. Do not commit signing keys.
- This is a local-only, synthetic-data prototype. There is no internet permission, backend/FHIR integration, authentication, formal consent workflow, audit trail, CSV/PDF export, or background synchronization. Do not use real patient data or claim HIPAA/GDPR compliance.
- Room stores UUIDs and AES-256-GCM encrypted patient payloads only. Android Keystore holds the encryption key, with record IDs authenticated as associated data. Backup and device transfer are disabled; unrecoverable keys/data fail closed, never reset automatically. Key recovery and database upgrades require a future migration strategy. Never add destructive migration fallback.
- The main activity sets `FLAG_SECURE` to protect screenshots and recents. Do not disable it to obtain preview screenshots. Use UI assertions or accessibility hierarchy for verification.
- Unsubmitted forms are held in an activity-scoped ViewModel, not plaintext saved-state bundles. Rotation retains drafts; process termination does not. Never log patient payloads or unexpected exception details.
- Numeric limits and adult reference ranges are illustrative, not clinically approved or age-adjusted. Canonical units are cm, kg, and Celsius. Unit changes clear the corresponding input to avoid silent reinterpretation.
- `requirements.md` and `todo.md` remain read-only specifications; do not update their completion statuses without explicit permission.
