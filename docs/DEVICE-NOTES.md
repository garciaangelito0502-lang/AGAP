# Device notes

## Development phone

| Date | Model | Android | Free storage | RAM | adb serial | How connected |
|---|---|---|---|---|---|---|
| 2026-10-27 | none available | | | | | `adb devices` lists nothing |

To fill in when a phone is connected: `adb -s <serial> shell getprop ro.product.model`,
`getprop ro.build.version.release`, `df -h /data`, `cat /proc/meminfo | head -1`.

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

A Wear OS watch is debugged over Wi-Fi (Settings > Developer options > Wireless debugging, Wear OS 3+).

## Build PC (for reference)

Windows 11 Pro (build 26200), 32 GB RAM with a very small page file, so Gradle and the emulator can run out of
memory. Prefer a physical phone to the emulator.
