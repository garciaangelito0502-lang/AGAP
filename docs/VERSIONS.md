# Software versions (AGAP 1.0)

For the research write-up (the Methods ask for software versions) and for rebuilding the same app later. Recorded
2026-11-08 for the release tagged `v1.0`.

## AGAP

| Part | Version |
|---|---|
| Phone app | 1.0 (version code 11), package `ph.agap.app`, Android 10 (API 29) or newer, built for API 36 |
| Watch app | 1.0 (version code 3), package `ph.agap.app`, Wear OS 3 (API 30) or newer, built for API 36 |
| Release files | [RELEASE.md](RELEASE.md) (sizes, SHA-256, signing certificate) |

## Build tools

| Tool | Version |
|---|---|
| JDK | Eclipse Temurin 17.0.21 (Java 17, contract 1.1) |
| Gradle (wrapper) | 9.8.2 |
| Android Gradle plugin | 9.4.1 |
| Android SDK platform / build-tools | 36 / 36.0.0 |
| R8 shrinking | off (D-053) |

## Libraries (from `gradle/libs.versions.toml`)

| Library | Version | Used for | Licence |
|---|---|---|---|
| MediaPipe Tasks Audio (`com.google.mediapipe:tasks-audio`) | 1.2.2 | Sound Alerts: runs YAMNet | Apache 2.0 |
| Vosk Android (`com.alphacephei:vosk-android`), with JNA 5.18.1 | 0.3.75 | Filipino captions, English fallback | Apache 2.0 |
| Room (`androidx.room`) | 2.8.5 | Event History database | Apache 2.0 |
| AndroidX AppCompat | 1.8.0 | Screens | Apache 2.0 |
| Material Components | 1.14.0 | Buttons, cards, navigation | Apache 2.0 |
| AndroidX RecyclerView | 1.4.0 | Event History list | Apache 2.0 |
| Google Play services Wearable | 20.0.1 | Phone-to-watch messages (phone and watch) | Android SDK licence |
| AndroidX Wear | 1.4.0 | Watch screen layout | Apache 2.0 |
| AndroidX Annotation | 1.11.0 | Code annotations | Apache 2.0 |
| Tests: JUnit 4.13.2, org.json 20260814, AndroidX Test runner 1.7.0, ext-junit 1.3.0, Espresso 3.7.0 (with accessibility checks) | | Unit and phone tests only, not in the apps | various open source |

## Models inside the phone app

| Model | File | Size | SHA-256 | Source | Licence |
|---|---|---|---|---|---|
| YAMNet (521 sound classes, float32) | `assets/yamnet.tflite` | 4,126,810 bytes | `4d8b4a53282dc83ef04e3e7dbc4fbc98082e34e44ed798e16c3a0cdd4c584faf` | MediaPipe audio classifier models (Google) | Apache 2.0 |
| Vosk Filipino | `vosk-model-tl-ph-generic-0.6.zip` | 329,169,068 bytes | `0a2e067e716c9a084379f9d2c0d7b4764a035adc9f23a8cdc167bcfbfde51259` | alphacephei.com/vosk/models | CC BY-NC-SA 4.0 (non-commercial) |
| Vosk English (small, fallback) | `vosk-model-small-en-us-0.15.zip` | 41,205,931 bytes | `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498` | alphacephei.com/vosk/models | Apache 2.0 |

The two Vosk zips are not in git (over GitHub's file limit). The first build downloads them (`fetchVoskModels`) and
stops if a checksum differs from the one above. The sound classes AGAP listens for are in `assets/annex_a.tsv`
(contract Annex A, threshold 50% by default).

## English speech recognition on the phone

English captions use Google's offline speech recognition on the phone, which is part of the phone, not of AGAP: it
can change when Google updates it. On my test phone: "Speech Services by Google" (`com.google.android.tts`), version
`googletts.google-speech-apk_20260928.02_p0.989914902` (code 210688745), with the en-US offline pack installed.
Record the version on the Infinix before the formal tests (Settings > Apps > Speech Services by Google), and keep
the phone from updating it during the test period if the results must be comparable.

## Test phones

| Phone | Android build | Used for |
|---|---|---|
| Xiaomi 13 (`2211133G`), development phone | Android 16, HyperOS `OS3.0.2.0.WMCCNXM`, security patch 2025-12-05, build `BP2A.250605.031.A3` | All checks in Phases 2 to 9 |
| Infinix Hot 70 (the client's test phone) | Android 15 (per the Methods) | Not checked by me; the group's alpha and beta tests |
| Wear OS 5 emulator `AGAP_Wear_Round` | Android 14, 454x454 round | Watch app (Phase 7); no real watch |
