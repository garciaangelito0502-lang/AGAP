# AGAP

AGAP (Advancing Greater Access to Communication for Persons with Hearing Impairments) is an offline Android
research prototype for deaf and hard-of-hearing users, with a Wear OS companion app. It was built for the student
researchers of Pasig City Science High School.

AGAP is a research prototype and a supplementary tool. It is not a medical device and not the only way to detect
emergencies.

## Features

- **Live Captions:** speech to text in English (Google's offline speech recognition on the phone, or AGAP's own
  Vosk English model where that is not available) or Filipino (Vosk `vosk-model-tl-ph-generic`), one language at a
  time, with no internet.
- **Sound Alerts:** 13 sounds in three groups (Emergency, Traffic, Household/Entry), recognised with Google's
  YAMNet model through the MediaPipe Audio Classifier; a full-screen alert with icon, name, group, confidence and
  time, and one vibration pattern per group.
- **Event History:** every alert with the time the sound was detected and the time the alert was shown, to the
  millisecond; detail view with the response time; CSV export.
- **Settings** (text size, high contrast, vibration, alert threshold), Sound Categories, Vibration Patterns guide,
  and a **Demonstration Mode** with sample captions and alerts that never uses the microphone.
- **Watch app** (Wear OS 3+): shows each alert with its vibration and the phone's live captions over Bluetooth.
- **Offline and private:** no internet permission, no accounts, no cloud; sound is never recorded or saved; AGAP
  listens only while its screen is open.

## Status

Version **1.0** (2026-11-08). Release files, sizes and checksums: [docs/RELEASE.md](docs/RELEASE.md). What it can
and cannot do: [docs/WHAT-1.0-CAN-AND-CANNOT-DO.md](docs/WHAT-1.0-CAN-AND-CANNOT-DO.md). Progress by phase:
[PROGRESS.md](PROGRESS.md).

## Requirements

- **Phone:** Android 10 or newer, ARM processor, about 1.5 GB of free storage (the app is 426 MB; Filipino captions
  unpack about 780 MB once). Microphone. For Google's offline English: Android 13 or newer with Google's speech
  service, and internet once to download the English pack.
- **Watch (optional):** Wear OS 3 or newer (Galaxy Watch4 or newer, Pixel Watch), paired with the phone.

## Install and use

- [docs/INSTALL-GUIDE.md](docs/INSTALL-GUIDE.md): installing the phone and watch apps, step by step.
- [docs/USER-GUIDE.md](docs/USER-GUIDE.md): every screen.

## Building

Needs JDK 17 and the Android SDK (platform 36). In Android Studio (Quail 4, 2026.1.4, or newer): open this folder
and run the `app` configuration (or `wear` for the watch). From a command line:

```
gradlew assembleDebug                          debug builds of both apps (Windows; ./gradlew on macOS and Linux)
gradlew :app:assembleRelease :wear:assembleRelease   release builds (see "Release signing")
gradlew testDebugUnitTest                      unit tests on the computer (104)
gradlew assembleDebugAndroidTest               phone tests (database, screens with accessibility checks, 12):
    install both APKs, then run
    adb shell am instrument -w ph.agap.app.test/androidx.test.runner.AndroidJUnitRunner
```

Debug APKs are written to `app/build/outputs/apk/debug/` and `wear/build/outputs/apk/debug/`.

**The first build needs the internet once.** The two Vosk speech models are not in the repository: the Filipino
model (329 MB) is over GitHub's file limit. The first build (task `fetchVoskModels`) downloads both from Alpha
Cephei into `app/src/main/assets-vosk/`, checks their SHA-256, and bundles them in the phone APK. Later builds reuse
the files. The app itself never goes online.

### Release signing

Release builds are signed with the AGAP release key, which is **not** in this repository. The build reads
`../keys/keystore.properties` (a folder `keys` next to this project folder) with the keys `storeFile`,
`storePassword`, `keyAlias` and `keyPassword`, or the file given with `-Pagap.keystore.properties=<file>`. Without
it, release APKs are built unsigned. The phone and watch apps must be signed with the same key and keep the same
applicationId (`ph.agap.app`), or the watch will not connect to the phone. Keep a backup of the key: an app signed
with a different key cannot update AGAP 1.0.

## Project structure

- `app/`: the phone app (Java 17, XML layouts, Android 10 or newer). Main packages: `alerts` (YAMNet and the
  Annex A mapping), `captions` (Google and Vosk engines), `history` (Room database, CSV export), `mic` (one
  microphone user at a time), `settings`, `vibration`, `watch` (sending to the watch), `ui` (screens).
- `wear/`: the Wear OS watch app: shows alerts and caption text sent by the phone.
- `shared/`: code and icons both apps use (alert groups, vibration patterns, icons, the watch message format).
- `app/src/main/assets/`: `yamnet.tflite` and `annex_a.tsv` (the 13 alerts and their YAMNet classes).
- `gradle/libs.versions.toml`: library versions. `gradle/signing.gradle`: release signing for both apps.
- `docs/`: guides, decisions, known issues, the contract checklist, the detection check and the phase notes.

## Third-party software and licences

| Component | Licence |
|---|---|
| YAMNet sound model (Google) | Apache License 2.0 |
| MediaPipe Tasks Audio (Google AI Edge) | Apache License 2.0 |
| Vosk speech recognition library (Alpha Cephei) and JNA | Apache License 2.0 |
| Vosk Filipino model `vosk-model-tl-ph-generic-0.6` | **CC BY-NC-SA 4.0: non-commercial use only** |
| Vosk English model `vosk-model-small-en-us-0.15` | Apache License 2.0 |
| AndroidX, Material Components, Room | Apache License 2.0 |
| Google Play services Wearable | Android Software Development Kit License |
| Material Symbols icons (Google) | Apache License 2.0 |

Because the Filipino model is licensed CC BY-NC-SA 4.0, **AGAP as built, with that model inside, may only be used
non-commercially**, and the model's attribution and licence must stay with it. The app's About screen lists these
credits. Exact versions and model checksums: [docs/VERSIONS.md](docs/VERSIONS.md).
