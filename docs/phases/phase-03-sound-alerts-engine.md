# Phase 03: Sound Alerts engine

Status: **Done**, checked on my phone (Xiaomi 13, Android 16) in airplane mode; the Infinix check is pending
with the client (M1 test build). Started 2026-11-04, finished 2026-11-06 (target 2026-11-08). Build: 0.3.0
(debug), phone. M1 test build: `agap-phone-0.1-test.apk`.

## Goal and what was built

The Alerts tab now listens. Start opens the microphone and YAMNet, Google's sound model, classifies the sound
about twice a second on the phone through MediaPipe's Audio Classifier, with no internet. When one of the YAMNet
classes that Annex A maps to an alert reaches the user's threshold (Settings, default 50%), the app raises an
alert event with label, category, confidence and the time it was detected, to the millisecond. A sound that goes on
raises its alert once; the same alert can fire again after 3 s of quiet, or after 30 s if it never stops. The screen
shows a listening indicator, the last alert in its category colours, a count since Start, and a grey "Testing info"
line with the closest Annex A sound and its score even when nothing fires (for the group's tests and for tuning).
Live Captions and Sound Alerts never hold the microphone together, listening stops when the screen leaves view,
and the screen stays on only while listening. The big alert screen, vibration and Event History come in Phase 5.

## Files and modules added or changed

- New, `app/src/main/java/ph/agap/app/alerts/`: `AlertCategory`, `AlertType`, `AlertEvent`, `AnnexA` (reads
  `assets/annex_a.tsv`), `AlertDetector` (threshold, strongest alert per window, quiet period),
  `SoundAlertEngine` (AudioRecord, MediaPipe Audio Classifier, debug-only source override).
- New, `app/src/main/java/ph/agap/app/mic/MicOwner.java`; new, `app/src/main/java/ph/agap/app/ui/alerts/`:
  `AlertsFragment`, `AlertsViewModel`.
- New assets: `yamnet.tflite` (MediaPipe's YAMNet, 4,126,810 bytes, stored uncompressed), `annex_a.tsv`.
- New resources: `layout/fragment_alerts.xml`; Sound Alerts and microphone strings in `strings.xml`.
- Changed: `AndroidManifest.xml` (MediaPipe's network permissions removed), `MainActivity` (Alerts tab),
  `CaptionsFragment` and `CaptionsViewModel` (take and release the microphone through `MicOwner`),
  `app/build.gradle` (0.3.0, versionCode 3, ARM-only native libraries, compressed; `tflite` not compressed),
  `gradle/libs.versions.toml` (MediaPipe tasks-audio 1.2.2).
- New tests: `alerts/AlertDetectorTest` (10), `alerts/AnnexATest` (6), `mic/MicOwnerTest` (4). 45 in total.
- New docs: `docs/DETECTION-CHECK.md`, `docs/CLIENT-TEST-CHECKLIST-0.1.md`.

## Decisions made and why

- D-016: MediaPipe tasks-audio 1.2.2; its usage-statistics library is kept (removing it crashes the classifier)
  but the network permissions it asks for are removed, so nothing can leave the phone.
- D-017: ARM-only native libraries, compressed in the APK: 115 MB down to about 52 MB.
- D-018: `AudioRecord` with `VOICE_RECOGNITION` at 16 kHz, 0.975 s windows every 0.4875 s, all 521 scores;
  one alert per window (the strongest at or above the threshold); 3 s quiet period, 30 s repeat.
- D-019: `MicOwner`: one microphone user at a time.
- D-020: `VOICE_RECOGNITION` kept after comparing four sources; debug-only source override for tests.
- D-021: no threshold or class tuning in Phase 3; decide with the group's Infinix results (contract 1.12).

## How it was verified

- `gradlew testDebugUnitTest`: 45 of 45 pass, including `AnnexATest`, which reads the label list out of the
  bundled model and fails on any Annex A class name that is not in it. `lintDebug`: 0 errors, the same 13
  warnings as Phase 2 (K-003), none in the new code.
- Detection check on my phone in **airplane mode** (`airplane_mode_on=1`, ping failed), clips played from the PC
  through the TV's speakers: 140 trials (6 alert clips and 4 no-alert clips, 3 levels, 5 repetitions), the same
  clips through the model on the PC without speakers, 120 more trials comparing four microphone sources, and one
  real knock by the owner. Full numbers and method: `docs/DETECTION-CHECK.md`. In short: car horn fired Vehicle Horn
  (5 of 21 at 0.50-0.85); the smoke alarm fired Fire Alarm (14 of 15, alerts at 0.50-0.89), not Smoke Alarm; knock and doorbell never
  reached 50%; speech, music and silence raised nothing (59 of 59); plain beeps raised Phone Ringing.
- Microphone handover on my phone, read from Android's recording log (`dumpsys audio`): Sound Alerts recorded
  18:03:42-18:04:12 and stopped when Captions opened; Google's speech service (captions) recorded 18:04:22-18:04:26;
  Sound Alerts again 18:04:29-18:04:33, stopped by the Home button. No overlap.
- `dumpsys window`: the keep-screen-on flag is set while Sound Alerts listens and cleared after Stop.
- `aapt2 dump permissions` on the M1 APK: only `RECORD_AUDIO` and Android's own receiver-protection permission.
- **Not verified:** anything on the Infinix; real fire alarms, smoke alarms, doorbells, sirens or car horns; a proper
  speaker or a second phone as the source; the group's distances and volumes; a changed threshold on the phone;
  sessions longer than about 25 minutes; response time (Phase 5).

## Contract items covered

- 1.4 Sound Alerts: built; checked on my phone (detection numbers observed, no accuracy claimed, contract 6.5).
- 1.2 Offline, no audio saved: Sound Alerts ran in airplane mode; no network permission; audio only in memory;
  the log holds alert and class names with scores, never audio.
- 1.10 One microphone user, only on screen, screen kept on: Sound Alerts side done and checked.
- 1.7 Settings: the alert threshold is used by Sound Alerts (vibration in Phase 5).
- Annex A: all 13 alerts are in the mapping and can fire; per-alert status in `SPEC-CHECKLIST.md`.
- Open: the full alert screen, vibration and Event History (Phase 5); tuning (contract 1.12, D-021).

## Known issues and scope flags

- K-010 smoke alarm shows as Fire Alarm; K-011 some test sounds are named differently by YAMNet; K-012 one sound can
  raise several different alerts; K-013 plain beeps raise Phone Ringing; K-014 knock and doorbell score low;
  K-015 test builds use the debug key, so the group must uninstall before installing 1.0.
- Scope flags: none.

## Next phase

Phase 4, Live Captions in Filipino with Vosk, plus the Vosk English fallback (target 2026-11-09 to 11-11). M1:
the group installs `agap-phone-0.1-test.apk` on the Infinix on 2026-11-09 and sends feedback by 2026-11-11.
