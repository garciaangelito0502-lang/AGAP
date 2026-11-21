# AGAP releases

## 1.1 (2026-11-21): background listening

Built 2026-11-21 from the commit tagged `v1.1`. Signed with the same release key as 1.0 (same certificate below), so
it installs over 1.0 and keeps the Event History and settings.

| File | App | Version (code) | Size | SHA-256 |
|---|---|---|---|---|
| `agap-phone-1.1.apk` | Phone (Android 10 or newer) | 1.1 (12) | 426,430,156 bytes | `efd26fc8bdf76d3572dfa9f63d2d022e229e8f296724d4773cd35a2b3347f98a` |
| `agap-watch-1.1.apk` | Watch (Wear OS 3 or newer) | 1.1 (4) | 5,399,206 bytes | `5459aa82afe20665c974ec3e547ed02893bc689487b4de68e7e238d59840d004` |

Permissions of the phone app (`aapt2 dump badging`): `RECORD_AUDIO`, `VIBRATE`, and for background listening
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`,
`SYSTEM_ALERT_WINDOW`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (plus AndroidX's internal receiver permission). Still no
`INTERNET`. Watch: `VIBRATE`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, as in 1.0.

## 1.0 (2026-11-08)

Built 2026-11-08 from the commit tagged `v1.0`. Both files are signed with AGAP's release key (D-054).

| File | App | Version (code) | Size | SHA-256 |
|---|---|---|---|---|
| `agap-phone-1.0.apk` | Phone (Android 10 or newer) | 1.0 (11) | 426,305,350 bytes | `c6e872831ab1ae0950e95d6e25a4f201ae5bd89a8c07981bb16621d900df6e18` |
| `agap-watch-1.0.apk` | Watch (Wear OS 3 or newer) | 1.0 (3) | 5,358,182 bytes | `38106c1931981730d2c795c4185cb308b11c4d143cd7069434515a3bfcffc487` |

Signing certificate (both files): `CN=AGAP, O=AGAP research prototype, L=Pasig City, C=PH`, SHA-256
`ab397c8559d51d52321e39619c1ca5775817f85fce94b2c233207755b92d8064`, APK Signature Scheme v2.

Check a file after copying it (Windows: `certutil -hashfile agap-phone-1.0.apk SHA256`; macOS or Linux:
`shasum -a 256 agap-phone-1.0.apk`). The result must match the table.

## How it was built

```
gradlew :app:assembleRelease :wear:assembleRelease
```

with the release key's properties file next to the project (`../keys/keystore.properties`, see README). The APKs come
out in `app/build/outputs/apk/release/` and `wear/build/outputs/apk/release/` and were renamed as above. R8 shrinking
is off (D-053). Exact tool, library and model versions: [VERSIONS.md](VERSIONS.md).

## Installing

See [INSTALL-GUIDE.md](INSTALL-GUIDE.md). A test build (signed with the debug key) must be uninstalled first (K-015).

## Earlier test builds (debug key, testing only)

| File | Phone app | Watch app | Date |
|---|---|---|---|
| `agap-phone-0.1-test.apk` | 0.3.0 | | 2026-11-06 |
| `agap-phone-0.9-test.apk`, `agap-watch-0.9-test.apk` | 0.7.0 | 0.7.0 | 2026-11-07 |
| `agap-phone-0.9.1-test.apk` (SHA-256 `ccf41b4e...6122`) | 0.8.0 | (0.9 watch file) | 2026-11-08 |
