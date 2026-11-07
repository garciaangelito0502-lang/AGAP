# Device notes

## Development phone

| Date | Model | Android | Free storage | RAM | adb serial | How connected |
|---|---|---|---|---|---|---|
| 2026-10-27 | none available | | | | | `adb devices` lists nothing |
| 2026-11-04 | Xiaomi 13 (`2211133G`, codename fuxi), arm64-v8a | 16 (API 36), HyperOS 3 (`OS3.0.2.0.WMCCNXM`, xiaomi.eu build), security patch 2025-12-05 | 97 GB of 224 GB | 11 GB (MemTotal 11,462,892 kB) | `44647d98` | USB, `adb devices` shows `device` |

Read with `adb -s <serial> shell getprop ro.product.model`, `getprop ro.build.version.release`,
`df -h /data` (in Git Bash set `MSYS_NO_PATHCONV=1`, or `/data` becomes a Windows path),
`cat /proc/meminfo | head -1`.

The Xiaomi 13 is the development phone from 2026-11-04 (D-005). It is much faster than the Infinix Hot 70 and
runs Android 16, not 15, so speed, microphone and battery results on it do not stand in for the Infinix.
Google's speech services are installed (`com.google.android.tts`, the Google app), which Phase 2 needs.

Xiaomi blocks `adb install` until **Developer options > Install via USB** is on (stock HyperOS asks for a
signed-in Mi account first). Until then `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED`, and each
install may show an "Install via USB?" prompt on the phone that has to be accepted.
Install via USB was turned on 2026-11-04; installs now succeed.

Speech recognition on this phone (2026-11-04): no system on-device recognizer
(`SpeechRecognizer.isOnDeviceRecognitionAvailable` is false). Default recognizer: Speech Services by Google
(`com.google.android.tts`, googletts 20260928.02), which has the en-US offline pack installed (D-013).
The phone dropped off adb for a few seconds several times while testing (K-009).

## Emulator (used while no phone is available)

| Date | Name | Android | Screen | RAM | Data partition | adb serial |
|---|---|---|---|---|---|---|
| 2026-10-27 | `AGAP_API35` (Google Play x86_64 image, revision 9) | 15 (API 35) | 1080x2400, 420 dpi | 2 GB | 6 GB | emulator-5554 |

Accelerated by the Windows Hypervisor Platform. Start it with
`emulator -avd AGAP_API35 -no-boot-anim -no-snapshot-save` from the SDK's `emulator` folder. It uses about
3-4 GB of the build PC's memory: do not run it next to a Gradle build when free memory is under about 3 GB.

## Client's test phone (from the Methods, not yet seen)

Infinix Hot 70, Android 15, 8 GB RAM, 128 GB storage. The client restarts it before tests. Every check
marked "Infinix" in SPEC-CHECKLIST.md stays "pending: client checks it" until it is run on this phone.

Connecting an Infinix to Windows: turn on Developer options (Settings > About phone > tap Build number seven
times), then USB debugging, plug in with a data cable, and allow the computer's key on the phone. If
`adb devices` shows nothing, Windows may need the phone maker's USB driver.

## Watch

| Date | Model | Wear OS | Status |
|---|---|---|---|
| 2026-10-27 | none available | | No watch and no watch emulator. On-device watch checks are "not possible". |
| 2026-11-04 | none available | | Asked again: still no watch and no watch emulator. On-device watch checks stay "not possible". |
| 2026-11-07 | Wear OS emulator `AGAP_Wear_Round` (`system-images;android-34;android-wear;x86_64`, device `wearos_large_round`, 454x454 round, 320 dpi, 2 GB) | Wear OS 5 (Android 14) | Watch app tested on it (D-045). Not paired with a phone; test messages fed with the debug-only receiver. Start: `emulator -avd AGAP_Wear_Round -no-snapshot-save -no-audio -no-boot-anim` (stop Gradle first: `gradlew --stop`). |

A Wear OS watch is debugged over Wi-Fi (Settings > Developer options > Wireless debugging, Wear OS 3+).

## Build PC (for reference)

Windows 11 Pro (build 26200), 32 GB RAM with a very small page file, so Gradle and the emulator can run out of
memory. Prefer a physical phone to the emulator.

## Test audio setup

Sounds and speech are played from the build PC toward the phone; the phone listens through its own microphone.

| Date | Phase | Played through | Notes |
|---|---|---|---|
| 2026-11-06 | 3 | TV speakers over HDMI ("Beyond TV") | Detection check (DETECTION-CHECK.md). |
| 2026-11-07 | 4 | KMS-268Pro Bluetooth speaker (Windows: "Headphones") | The TV was not connected. The laptop's own speakers made no sound although Windows sent them a full-level signal (output meter 0.96), so they were not used. Clips must be 16-bit PCM WAV: Windows' `SoundPlayer` plays 32-bit float files (the FLEURS originals) as noise. |

Filipino speech for Phase 4: 12 sentences from Google's FLEURS dataset (`fil_ph`, dev split, CC BY 4.0), read
speech with reference transcripts, converted to 16-bit PCM and peak-normalised to -1 dBFS. FLEURS marks every
speaker in that split as male. Plus the owner reading 6 everyday Filipino and 3 English sentences.
