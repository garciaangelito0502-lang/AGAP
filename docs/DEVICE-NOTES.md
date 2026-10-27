# Device notes

## Development phone

| Date | Model | Android | Free storage | RAM | adb serial | How connected |
|---|---|---|---|---|---|---|
| 2026-10-27 | none available | | | | | `adb devices` lists nothing |

To fill in when a phone is connected: `adb -s <serial> shell getprop ro.product.model`,
`getprop ro.build.version.release`, `df -h /data`, `cat /proc/meminfo | head -1`.

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
