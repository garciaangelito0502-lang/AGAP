# Phase 11: Background listening (change request)

Status: **Done** on my phone (Xiaomi 13, HyperOS, Android 16). Finished 2026-11-21. Release 1.1: phone 1.1 (code 12),
watch 1.1 (code 4). Outside the contract (1.10, section 3); approved by the owner on 2026-11-21 and built free of
charge for the group (D-055). Not tried on the Infinix or with a real watch.

## Goal and what was built

The group asked (2026-11-21) for AGAP to keep listening when minimized, with alerts as a popup or a widget, and
captions that keep coming in the background. Version 1.1 adds one setting, **Keep listening when I leave AGAP**, off
by default; with it off, 1.1 behaves like 1.0. With it on, Sound Alerts or Live Captions go on after the user leaves
AGAP or the screen turns off, until Stop:

- Alerts outside AGAP: the full alert over the lock screen (screen off or locked), a large card over another app
  ("Display over other apps"), or a heads-up notification; vibration and the watch alert at once; each saved once,
  with how it was shown ("Shown as", new database column).
- Captions outside AGAP: a floating window over other apps, the newest line in the listening notification (never on
  the lock screen), and the whole session on the Captions screen on return.
- The "AGAP is listening" notification with Stop; a home-screen widget (state, newest alert, Open, Stop); a Settings
  section that lists what background listening needs, each with an Allow button.
- New launcher icon from the group's 1.1 logo (D-061).

## Files and modules added or changed

- New package `listen`: `AgapApp`, `AppVisibility`, `AlertsSession`, `CaptionsSession`, `ListeningService`,
  `AlertPresenter`, `AlertLedger`, `AlertActivity`, `PopupAlert`, `FloatingCaptions`, `AgapNotifications`,
  `BackgroundPermissions`, `StopReceiver`, `AgapWidget`.
- Changed: `AlertsFragment`, `CaptionsFragment` (show the sessions; stop on leaving only with the setting off),
  `AlertsViewModel`, `CaptionsViewModel` (state moved to the sessions), `AlertOverlayFragment` (where it is shown,
  save once), `MainActivity` (tab links), `HomeFragment` (listening status), `SettingsActivity` (Background listening
  section), `AlertDetailActivity` ("Shown as"), `SoundAlertEngine` (threshold can change while listening),
  `SettingsStore`, `AlertRecord`, `AlertRecorder`, `AgapDatabase` (schema 2, migration), `AlertRecordDao`,
  `HistoryRepository`, `HistoryCsv` (new last column), new `ShownVia`.
- Resources: layouts `popup_alert`, `floating_captions`, `widget_agap`, `item_background_need`, notes in
  `fragment_alerts` and `fragment_captions`, Settings section; `xml/agap_widget_info`; widget and window drawables;
  theme `Theme.AGAP.AlertHost`; strings; launcher icon layers (phone and watch); watch strings.
- Manifest: permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `WAKE_LOCK`, `POST_NOTIFICATIONS`,
  `USE_FULL_SCREEN_INTENT`, `SYSTEM_ALERT_WINDOW`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; still no `INTERNET`.
  Application class, `AlertActivity`, `ListeningService` (type microphone), `StopReceiver`, `AgapWidget`.
- Tests: unit tests for the CSV column, "Shown as", the setting and the recorder (108 in all); instrumented
  `MigrationTest` and `WidgetAndBackgroundTest` (16 in all).
- Docs: this note, DECISIONS (D-055 to D-061), KNOWN-ISSUES (K-033 to K-039), SCOPE-FLAGS, SPEC-CHECKLIST (CR-1 to
  CR-5), CHANGELOG, USER-GUIDE, INSTALL-GUIDE (step 10), WHAT-1.1-CAN-AND-CANNOT-DO, CLIENT-TEST-CHECKLIST-1.1,
  README, VERSIONS, RELEASE, PROGRESS.

## Decisions made and why

D-055 change request, off by default, screen-off included; D-056 sessions and a microphone foreground service started
only from AGAP, no restart, wake lock; D-057 how and when an alert is shown outside AGAP, saved once, "Shown as";
D-058 floating captions, nothing on the lock screen; D-059 widget; D-060 versions 1.1, targetSdk stays 36; D-061 icon.

## How it was verified

All on my phone (Xiaomi 13, HyperOS, Android 16), release 1.1 signed with AGAP's key, test clips from the PC through
the KMS-268Pro speaker, as in Phases 3-9.

| Check | Result |
|---|---|
| Builds and tests | `assembleRelease` (lintVital) for both apps; 108 unit tests pass (97 phone, 7 shared, 4 watch); 16 instrumented tests pass on the phone (12 from 1.0, migration, 3 widget and background) |
| Update from 1.0 | Release 1.1 installed with `adb install -r` over release 1.0 that had 2 alerts: both still in History, read as "App screen" |
| Setting and permissions | Turning the setting on asked for notifications; "Alerts on the lock screen" was already allowed for this side-loaded app; Allow opened "Display over other apps" (HyperOS: the app list) and HyperOS's battery page ("No restrictions"); every row then said Allowed |
| Service | With Sound Alerts or Live Captions started and the setting on: foreground service type microphone (`types=0x80`), "AGAP is listening" notification with Stop |
| Alerts, notification | AGAP behind the home screen, no popup permission: heads-up notifications 24-33 ms after detection; tapping one opened the full alert, not saved a second time |
| Alerts, lock screen | Screen off: the alert turned the screen on over the lock screen, saved as Lock screen, 637 ms after detection (with an alert screen already open: 18-23 ms) |
| Alerts, popup | Another app open, popups allowed: alert card over it, saved as Popup, 80-113 ms. The first try closed AGAP (K-033) and the next showed a notification banner over the card (K-034); both fixed and checked again |
| Captions, English | Google offline English kept captioning from the service with AGAP in the background; floating window over the phone's Settings app; whole session on the Captions screen after Open. 2 of 6 synthetic sentences, the same as with AGAP in view (K-037) |
| Captions, Filipino | Vosk Filipino kept captioning in the background (4 lines from the English clip), floating window over the home screen |
| Stop from the notification | 20 of 20 cycles: Start in AGAP, Home, Stop in the expanded notification, service gone; no crash |
| Widget | Settings' Add placed it on HyperOS once AGAP had "Home screen shortcuts"; I could not find it on a home page (K-036). Its content (states, newest alert, Stop only while listening) passed an instrumented test |
| Long screen-off runs | See "Long runs with the screen off" below |

### Long runs with the screen off

The phone was charging over USB throughout, so there is no battery-drop figure; Android's per-app estimate (battery
statistics, phone set to count as unplugged during the run) is given instead.

| Run | Set-up | Result |
|---|---|---|
| English captions (Google offline) | Started in AGAP, screen turned off; test speech every 2.5 min for 10.5 min, then left running with the phone locked | Captioned for **2 h 57 min** (19:39 to 22:36) until it was stopped in AGAP: 6 lines from the 4 clips, then the notification kept updating with speech in the room to the end. Same process and foreground service throughout; 66-67 MB; CPU 0-7%. AGAP's own estimate 0.67 mAh for the first 10.75 min (the recognizer runs in Google's speech service and is counted there). 18 min after the stop, HyperOS's "LockScreenClean" closed AGAP's process, by then idle (exit reason recorded by Android) |
| Filipino captions (Vosk) | Started in AGAP, screen off, test speech every 2.5 min for 10.5 min | 13 lines (163 words) from 4 clips; same process and foreground service; 910-947 MB (the Filipino model); CPU 30-56% just after each clip; estimate **16.1 mAh in 10 min 39 s** (about 91 mAh an hour; this phone's battery is about 4,500 mAh); battery temperature 29.0 to 36.4 °C while charging |
| Sound Alerts | Single alerts with the screen off (above) | The planned 40-60 min Sound Alerts run with the screen off was **not done**: the phone was locked at the time and I could not unlock it remotely. Sound Alerts did run in the background through the alert and 20-cycle checks |

**Not verified:** the Infinix (XOS battery settings, its popups and lock-screen alerts, the widget on its launcher);
the watch with background listening (no watch); a long Sound Alerts run with the screen off; a phone call during
background listening; swiping AGAP away from the recent-apps list; a phone restart with background listening on (AGAP has no boot receiver, so nothing can start by
itself; not run); the widget on a home screen; battery use off the charger.

## Contract items covered

None of section 1: this is a change request (SCOPE-FLAGS, 2026-11-21). Row 1.10 of SPEC-CHECKLIST notes that with
the setting off, 1.1 keeps the 1.0 behaviour. New rows CR-1 to CR-5.

## Known issues and scope flags

New K-033 and K-034 (found and fixed before release), K-035 (the phone can still stop AGAP), K-036 (widget on HyperOS),
K-037 (recognizer gaps, as Phase 2), K-038 (background response times measured differently), K-039 (HyperOS settings
pages). Scope flag of 2026-11-21: approved and built.

## Next phase

Phase 10 (support and tuning until 2026-12-07) goes on. The group: try CLIENT-TEST-CHECKLIST-1.1 on the Infinix, and
ask their adviser before using background listening in data collection (their Methods place it outside the study).
