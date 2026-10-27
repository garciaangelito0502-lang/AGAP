# AGAP

AGAP (Advancing Greater Access to Communication for Persons with Hearing Impairments) is an offline Android
research prototype with two features, Live Captions and Sound Alerts, plus a Wear OS companion app. It is built
for the student researchers of Pasig City Science High School.

AGAP is a research prototype and a supplementary tool. It is not a medical device and not the only way to
detect emergencies.

## Status

In development. See [PROGRESS.md](PROGRESS.md) for the phase dashboard and [docs/](docs/) for decisions,
known issues and the contract checklist.

## Project layout

- `app/`: the phone app (Java 17, XML layouts, Android 10 or newer).
- `wear/`: the Wear OS companion app (Wear OS 3 or newer). Shell only until Phase 7.
- `gradle/libs.versions.toml`: library versions. `gradle/signing.gradle`: release signing for both apps.
- `docs/`: progress notes, decisions, checklists.

## Building

Needs JDK 17 and the Android SDK (platform 36). In Android Studio (Quail 4, 2026.1.4, or newer): open this
folder and run the `app` configuration. From a command line:

```
gradlew assembleDebug          (Windows)
./gradlew assembleDebug        (macOS, Linux)
gradlew testDebugUnitTest      unit tests
```

The phone APK is written to `app/build/outputs/apk/debug/`, the watch APK to `wear/build/outputs/apk/debug/`.

Release builds are signed with the AGAP release key, which is **not** in this repository. The build looks for
`../keys/keystore.properties` next to this folder (or the file given with `-Pagap.keystore.properties=<file>`).
Without it, release APKs are built unsigned. The phone and watch apps must be signed with the same key and
keep the same applicationId (`ph.agap.app`), or the watch will not connect to the phone.

The full install guide comes with the turnover (Phase 9).
