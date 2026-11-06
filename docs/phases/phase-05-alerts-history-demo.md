# Phase 05: Alert screen, vibration, Event History, Demonstration Mode

Status: **Done**, checked on my phone (Xiaomi 13, Android 16) in airplane mode; the Infinix check is pending
with the client. Finished 2026-11-07 (target 2026-11-12 to 11-14). Build: 0.5.0 (debug), phone.

## Goal and what was built

Every detection now becomes a clear alert and a saved, measurable history entry. When Sound Alerts hears one of the
13 sounds, a full-screen alert opens over the app in the category's colour, with the category's badge shape, the
sound's own icon, its name, the category, a confidence bar, the time it was detected and the vibration rhythm, and a
large Dismiss button. The phone vibrates in the category's pattern (as an alarm, so also on silent), unless Vibration
is off in Settings. Listening carries on behind the alert, so a following sound replaces it. Each real alert is saved
in the Event History with the time the sound was detected and the time its alert reached the screen, both to the
millisecond. The History tab lists them newest first by day; each opens a detail view with both times and the
response time. Clearing asks first. Demonstration Mode, for presentations, shows a striped banner on every screen,
plays sample captions and lets the presenter show any of the 13 alerts, without ever opening the microphone and
without saving anything.

## Files and modules added or changed

- New `history/`: `AlertRecord` (Room entity), `AlertRecordDao`, `AgapDatabase`, `HistoryRepository`,
  `HistoryStore`, `AlertRecorder`. New `vibration/`: `VibrationPatterns`, `AlertVibrator`. New `demo/DemoMode`.
  New `captions/DemoCaptionEngine`.
- New UI: `ui/alerts/AlertOverlayFragment`, `AlertLooks`, `VibrationBarsView`; `ui/history/HistoryFragment`,
  `HistoryAdapter`, `HistoryFormat`, `AlertDetailActivity`; `ui/DemoBanner`; `ui/info/DemoModeActivity` (rewritten).
- Changed: `AlertsFragment` (opens the alert screen; blocked in Demonstration Mode), `CaptionsFragment` (sample
  captions in Demonstration Mode), `MainActivity` (History tab, clear-history action, open-tab extra),
  `BaseActivity` (banner on every screen), `AndroidManifest.xml` (`VIBRATE`, `AlertDetailActivity`).
- Resources: layouts `dialog_alert`, `fragment_history`, `item_history_alert`, `item_history_header`,
  `activity_alert_detail`, `activity_demo`, `demo_banner` (in `top_bar`); 16 icons and 3 badge shapes; colours
  `alert_tile_on_dark`, `alert_tile_on_light`, `demo_stripe`; theme `Theme.AGAP.AlertOverlay`; strings.
- Removed: `PlaceholderFragment`, `fragment_placeholder.xml` and two placeholder strings (no tab uses them now).
- Build: 0.5.0, versionCode 5; Room 2.8.5 (+ compiler), RecyclerView 1.4.0, AndroidX Test runner 1.7.0 and ext-junit
  1.3.0; Room schema in `app/schemas/`.
- Tests: `history/AlertRecorderTest` (3), `history/AlertRecordTest` (3), `vibration/VibrationPatternsTest` (8),
  helper `alerts/TestAlerts`; instrumented `history/AlertRecordDaoTest` (4).

## Decisions made and why

- D-028: the alert is a full-screen dialog over the app, so listening goes on and nothing is missed between trials.
- D-029: "alert shown" = the frame-commit time of the alert's first frame (1 s fallback to the draw time).
- D-030: Room 2.8.5; extra columns `yamnetClass` and `thresholdPercent` for the group's analysis and for tuning.
- D-031: the design note's vibration patterns as data; alarm usage; Emergency repeat capped at 60 s.
- D-032: Demonstration Mode in memory only, so a restart always brings back normal mode.
- D-033: Material Symbols icons (Apache 2.0). D-034: instrumented tests via `am instrument`.
- Scope flag: export of the Event History (not built).

## How it was verified

All on the Xiaomi 13 (adb `44647d98`), build 0.5.0 debug, airplane mode on. Clips played from the PC through the
KMS-268Pro Bluetooth speaker.

| Check | Result |
|---|---|
| `testDebugUnitTest` | 80 of 80 pass (14 new) |
| Instrumented `AlertRecordDaoTest` (`am instrument`) | 4 of 4 pass: every field round-trips to the ms, newest first, ties by save order, clear deletes all |
| `lintDebug` | 0 errors, 5 warnings (K-003), none in the new code; the 6 alert colours are now used |
| Real alert, smoke-alarm clip | Fire Alarm 0.59 raised the red alert screen; Emergency vibration with `usage=ALARM` in Android's vibrator log; saved as #1 |
| Detail view of that alert | Detected 04:11:58.191, shown 04:11:58.321, response 130 ms, heard as "Fire alarm", threshold 50%: the same as the log |
| Response times (log and history) | Alert screen opening: 130, 83, 74, 55, 66, 61, 82, 92, 71, 58 ms (10 alerts; 130 ms was the first after the app started). Replacing an alert on screen: 21, 22, 20, 17, 18 ms |
| Listening during an alert | Second alerts arrived while the screen was open and replaced it; "Alerts since Start: 2"; all saved |
| Other real alerts | Vehicle Horn 0.97 and 0.50 (car-horn clip); wrong alerts Warning Siren and Appliance Timer after the smoke alarm (K-012), saved as such |
| Vibration off in Settings | Alert shown with "Vibration: Off"; no vibration logged |
| Event History | Newest first under "Today, November 7"; high-contrast rows readable; clear: question with the count, Cancel keeps all, "Delete all" empties it (log: cleared 11 alerts) |
| Demonstration Mode | Banner on Demonstration, Captions, Alerts, History and the alert screen; sample Fire Alarm and Vehicle Horn shown 57 ms after the tap with their vibrations, logged "not saved"; history count unchanged; Live Captions played the Filipino sample; Sound Alerts Start disabled with a note; Android's recording log shows no AGAP microphone use from before the demo began to its end; off again after the app was restarted |

**Not verified:** anything on the Infinix (response times there will be longer); real fire alarms, doorbells,
knocks or sirens (only played clips); how the vibration patterns feel to the group and whether they are easy to tell
apart; TalkBack reading of the alert screen; a large history (hundreds of alerts); the 60 s Emergency vibration
cap; the alert screen at the largest text size; process death while an alert is open.

## Contract items covered

- 1.5 Alert screen and vibration per category: built and checked on my phone.
- 1.6 Event History with both times to the ms and a detail view: built and checked; DAO test on the phone.
- 1.9 Demonstration Mode: built and checked (no microphone, nothing saved, labelled on every screen).
- 1.7 Settings: Vibration on/off and the threshold are now used by alerts. 1.10: listening continues while an alert shows.

## Known issues and scope flags

- K-012 updated (second wrong alert after a smoke alarm, now visible and saved); K-019 UI test tools cannot read
  the alert screen while its badge pulses (test tooling only).
- Scope flag: Event History export.

## Next phase

Phase 6: remaining screens (Sound Categories, Vibration Patterns guide with the same pattern data), settings wiring
and accessibility polish (target 2026-11-15).
