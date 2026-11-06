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
gradlew testDebugUnitTest      unit tests (on the computer)
gradlew assembleDebugAndroidTest   instrumented tests, then install both APKs and run
    adb shell am instrument -w ph.agap.app.test/androidx.test.runner.AndroidJUnitRunner
```

The phone APK is written to `app/build/outputs/apk/debug/`, the watch APK to `wear/build/outputs/apk/debug/`.

**First build needs the internet once.** The two Vosk speech models are not in the repository: the Filipino
model (`vosk-model-tl-ph-generic-0.6`, 329 MB) is over GitHub's file limit. The first build (task
`fetchVoskModels`) downloads both from Alpha Cephei into `app/src/main/assets-vosk/`, checks their SHA-256, and
bundles them in the phone APK (about 431 MB in all). Later builds reuse the files. The app itself never goes
online. The Filipino model is CC BY-NC-SA 4.0: non-commercial use only. The small English model
(`vosk-model-small-en-us-0.15`, 41 MB, Apache-2.0) is used only on phones without Google's offline English.
On the phone a model is unpacked once, on its first use; Filipino needs about 1 GB of free storage for that.

Release builds are signed with the AGAP release key, which is **not** in this repository. The build looks for
`../keys/keystore.properties` next to this folder (or the file given with `-Pagap.keystore.properties=<file>`).
Without it, release APKs are built unsigned. The phone and watch apps must be signed with the same key and
keep the same applicationId (`ph.agap.app`), or the watch will not connect to the phone.

The full install guide comes with the turnover (Phase 9).
