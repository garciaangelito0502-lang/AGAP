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

A Wear OS watch is debugged over Wi-Fi (Settings > Developer options > Wireless debugging, Wear OS 3+).

## Build PC (for reference)

Windows 11 Pro (build 26200), 32 GB RAM with a very small page file, so Gradle and the emulator can run out of
memory. Prefer a physical phone to the emulator.
