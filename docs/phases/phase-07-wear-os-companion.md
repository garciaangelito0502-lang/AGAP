# Phase 07: Wear OS companion

Status: **Done** on a Wear OS 5 emulator and my phone. The phone-to-watch link over Bluetooth was **not** run end to
end: no watch, and the emulator was not paired with a phone (owner's choice, D-045). The group checks it on a real
watch with test build 0.9.
Finished 2026-11-07 (target 2026-11-16 to 11-18). Build: 0.7.0 (debug), phone and watch. M2: test build 0.9.

## Goal and what was built

The watch mirrors alerts and live captions from the phone over Bluetooth, with no internet. The phone does all the
listening; the watch never uses a microphone.

- **Phone:** `WatchSender` sends each alert as the phone shows it, the caption text (partial updates at most 4 times
  a second, final lines at once) and what the phone is doing (idle, Live Captions and its language, Sound Alerts,
  Demonstration Mode). It sends only when a watch with the AGAP app is connected, never waits for an answer, and
  does nothing on a phone without Google Play services. Home's status card says whether a watch is connected.
- **Watch:** one screen. Status: "Phone not connected" (with what to do), "Phone connected" (start Sound Alerts or
  Live Captions on the phone), "Sound Alerts on", and a yellow Demonstration Mode line. Live Captions: the phone's
  text, large (20sp), the last 30 lines, following the newest words unless scrolled back, screen kept on. Alert:
  full screen in the category's colour with its badge shape and icon, label, category, confidence and time, and
  the category's vibration (Emergency repeats until closed, at most a minute). Tap to close.
- **Alert delivery:** with the AGAP watch screen in front, the app vibrates and shows the alert, and wakes the
  display if the watch has dimmed. With it closed, the alert becomes a notification that vibrates in the
  category's pattern; it does not open the full alert by itself (K-022).

## Files and modules added or changed

- New module `shared/` (Android library, `ph.agap.shared`): moved `alerts/AlertCategory`,
  `vibration/VibrationPatterns`, 19 drawables (13 alert icons, 3 category icons, 3 badges); new `alerts/AlertIcons`,
  `watch/WatchMessages` and `WatchMessagesTest` (7).
- Phone: new `watch/WatchSender`, `watch/CaptionThrottle` and `CaptionThrottleTest` (4); hooks in `MainActivity`,
  `AlertOverlayFragment`, `CaptionsFragment`, `AlertsFragment`, `HomeFragment`; `AlertLooks` uses `AlertIcons`;
  3 strings. Version 0.7.0, code 9.
- Watch: `WatchActivity` (rewritten), `WatchHub`, `WatchListenerService`, `WatchVibrator`, `AlertNotifier`,
  `CaptionBuffer` and `CaptionBufferTest` (4); debug only `DebugMessageReceiver`; layout, strings, colours, theme,
  dimens, `wear.xml` (capability), data-extraction rules; permissions VIBRATE, POST_NOTIFICATIONS,
  USE_FULL_SCREEN_INTENT. Version 0.7.0, code 2.
- Build: `settings.gradle` (`:shared`), version catalog (play-services-wearable 20.0.1, androidx.wear 1.4.0,
  androidx.annotation 1.11.0, library plugin).

## Decisions made and why

- D-042 Data Layer messages, format and rules. D-043 the `:shared` module. D-044 the watch screen and how an alert
  reaches the user. D-045 tested on an emulator, not paired, with a debug-only test receiver.

## How it was verified

Watch: Wear OS 5 emulator `AGAP_Wear_Round` (Android 14, 454x454 round), watch 0.7.0 debug, messages fed with
`DebugMessageReceiver` (built by the same `WatchMessages` code as the phone's). Phone: Xiaomi 13, phone 0.7.0 debug,
airplane mode.

| Check | Result |
|---|---|
| `testDebugUnitTest` | 100 of 100 pass (phone 89, shared 7, watch 4) |
| Instrumented tests on my phone (`am instrument`, animations off, restored after) | 12 of 12 pass |
| `lintDebug` | 0 errors; warnings: phone 5, watch 2, shared 1 (K-003) |
| Network and microphone permissions | Merged release manifests: phone RECORD_AUDIO and VIBRATE only (no network); watch VIBRATE, POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT. The debug receiver is not in release |
| Same signing key | `apksigner`: debug APKs share the debug key, release APKs share AGAP's key (SHA-256 ab397c85...) |
| Phone with no watch | Log "0 AGAP watch(es) connected"; Home shows "No watch connected..."; a sample Doorbell alert and Filipino sample captions ran and stopped; same process throughout, no crash |
| Watch first start | Asks to allow notifications; then "Phone not connected" |
| Status screens | "Sound Alerts on", "Phone connected", Demonstration Mode line in yellow; back to "Phone not connected" within 30 s after the phone went quiet |
| Alerts, 3 categories | Fire Alarm (red, octagon), Vehicle Horn (orange, diamond), Doorbell (blue, circle), with icon, label, category, % and time |
| Vibration | Android's vibrator log: Emergency 600-250-600-250-600 ms repeating, Traffic short-short x3, Household long-short x2, usage ALARM; tap to close stops it; an unanswered Emergency stopped at 60,007 ms; with the phone's vibration off, no vibration |
| Sample alerts | "Sample alert" on top; the longest name ("Emergency Vehicle Siren") fits |
| Largest text size (font scale 1.24) | Alerts fit after the badge shrinks (first try cut "Tap to close"; fixed); captions readable |
| Live Captions | Filipino and English, partial then final lines; follows the newest words; scrolling back stays put while new words arrive; "Sample captions" header in Demonstration Mode; an alert over captions, then captions again after closing |
| Watch dimmed, AGAP screen in front | Alert wakes the display (`mWakefulness` Dozing to Awake) and shows in full |
| AGAP watch screen closed | Watch face awake: notification pops up ("Doorbell, Household/Entry, Open on watch"), vibrates; "Open on watch" shows the full alert. Watch dimmed: vibration and a dot only. Full-screen request ignored by Wear OS, also with the permission allowed by adb (K-022) |

**Not verified:** the phone-to-watch link over Bluetooth and its delay (K-021); anything on a real watch (look,
vibration strength, battery, K-024); the `WearableListenerService` path (it runs only with a paired phone);
the watch's crown scrolling; Wear OS 3 (API 30-32) devices.

## Contract items covered

- 1.11 Wear OS 3+ watch app: built; checked on the emulator and on my phone without a watch; real watch pending.
- 1.5 vibration per category: the watch plays the same pattern data as the phone.
- 1.2 offline: no network permission on either app; the link is Bluetooth through Google Play services.

## Known issues and scope flags

- K-021 link not run end to end; K-022 alert with the AGAP watch screen closed; K-023 closing on one device does not
  close the other; K-024 captions keep the watch screen on. K-005 and K-003 updated.
- Scope flag: closing an alert on both devices at once (not built).

## Next phase

Phase 8: offline proof, hardening and tests (target 2026-11-19 to 11-20, during the group's alpha test). It includes
"Bluetooth off" and "watch disconnect and reconnect", which need a real watch.
