# Phase 09: Release build, install guide and turnover

Status: **Done.** Source pushed on 2026-11-08 to the owner's private GitHub repository `AGAP` (`main` and all tags,
`v1.0` on commit `b26cfbd`). Finished 2026-11-08
(target 2026-11-21, M3). Release 1.0: phone 1.0 (code 11), watch 1.0 (code 3).

## Goal and what was built

The handover set for the group: signed release APKs of both apps, an install guide a student can follow, a user
guide, a README for the GitHub repository, a list of exact versions for the research write-up, and a plain note on
what 1.0 can and cannot do. Before it, test build 0.9.1 (phone 0.8.0, the Phase 8 fixes) replaced the phone file of
test build 0.9 for the group's alpha test.

## Files and modules added or changed

- `app/build.gradle` 1.0 (11); `wear/build.gradle` 1.0 (3). No code change.
- New docs: `RELEASE.md`, `VERSIONS.md`, `INSTALL-GUIDE.md`, `USER-GUIDE.md`, `WHAT-1.0-CAN-AND-CANNOT-DO.md`,
  this note. Rewritten: `README.md`. Updated: DECISIONS (D-053, D-054), KNOWN-ISSUES (K-015, K-032), SPEC-CHECKLIST,
  CHANGELOG, PROGRESS, CLIENT-TEST-CHECKLIST-0.9 (0.9.1), and local machine paths removed from DECISIONS,
  DEVICE-NOTES and the Phase 0 note.
- Outside the repo: `builds/agap-phone-1.0.apk`, `builds/agap-watch-1.0.apk`, `builds/agap-phone-0.9.1-test.apk`.

## Decisions made and why

D-053 no R8 shrinking (little to gain, JNA/JNI keep-rule risk); D-054 version codes and release signing.

## How it was verified

| Check | Result |
|---|---|
| Release builds | `:app:assembleRelease :wear:assembleRelease`, lintVital passed; phone 426,305,350 bytes, watch 5,358,182 bytes |
| Signing | `apksigner verify`: both verify (v2), same certificate SHA-256 `ab397c85...8064`, `CN=AGAP` |
| Versions and permissions | `aapt2 dump badging`: phone 1.0 (11) RECORD_AUDIO and VIBRATE only; watch 1.0 (3) VIBRATE, POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT |
| Repo hygiene | No keystore, password, PDF, consent letter or APK in the tracked or untracked files; local paths replaced (the `%LOCALAPPDATA%` default and the phone's adb serial in old test records remain) |
| Install guide on my phone (Xiaomi 13, Android 16, airplane mode) | Step 1: test build uninstalled. Step 2: APK copied to Download (checksum matched). Step 3: tapped in the file manager, Xiaomi's installer, Install, about 6 s; it then showed "Permissions denied ... sensitive permissions" (K-032); Launch. Step 4: Captions > Allow microphone > "Allow only while using the app": granted. Step 5: English ready at once (Google's pack was already on this phone). Step 6: Filipino Set up now: unpacked in 3.7 s. Step 7: features below |
| 10-minute smoke run of 1.0 (9.4 minutes) | Sound Alerts with clips from the laptop speakers: Fire Alarm, Appliance Timer and Alarm Clock alerts, all saved; English and Filipino captions listened 2 minutes each; Demonstration Mode sample Doorbell (not saved); History 4 alerts; About "Version 1.0" with the credits; same process throughout, crash log empty, nothing recording at the end |
| Watch 1.0 on the Wear OS 5 emulator | Uninstalled the debug 0.7.0 watch app first, as the guide says; installed 1.0 (3) with `adb install`; first start asked for notifications (allowed), then "Phone not connected" |

**Not verified:** the Infinix (all of it); the "Allow from this source" step (my phone's file manager was already
allowed); step 5's download of the English pack (already on my phone); watch steps 8-9 on a real watch over Wi-Fi,
and the phone-to-watch link (no watch, K-021).

## Contract items covered

1.13 turnover: APKs, install guide, source ready for GitHub (push pending), remote help offered in the guide. 1.1
release build checked on my phone. Everything not verified is listed in WHAT-1.0-CAN-AND-CANNOT-DO.md and in the
SPEC-CHECKLIST.

## Known issues and scope flags

New K-032 (HyperOS notice after a file install). K-015 updated. No new scope flag.

## Next phase

Phase 10: support and tuning until 2026-12-07, with the group's alpha test (11-19 to 11-20, test build 0.9.1) and
beta test (11-23 to 11-29, 1.0). The source is on GitHub (private).
