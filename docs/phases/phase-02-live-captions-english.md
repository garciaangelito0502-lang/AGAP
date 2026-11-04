# Phase 02: Live Captions, English

Status: **Done**, checked on my phone (Xiaomi 13, Android 16), online and in airplane mode; the Infinix check
is pending with the client. Date: 2026-11-04 (target was 2026-11-07). Build: 0.2.0 (debug), phone.

## Goal and what was built

The user opens Captions, keeps English selected, taps Start, and sees what is said as text, with no internet.
Words appear while the sentence is still being spoken (lighter and underlined), then settle as a finished line.
Each sentence gets its own paragraph, and the box scrolls to the newest line. Stop ends listening and keeps the
text on screen until Clear or the next Start, so the research team can compare it with their script. The
microphone permission has an explanation screen and a "blocked" state with a button to the app settings. If
the English offline pack is missing, the screen offers Google's one-time download. Filipino shows in the
language picker but stays disabled until Phase 4. Captions stop when the screen leaves view, and the screen
stays on only while listening.

## Files and modules added or changed

- New, `app/src/main/java/ph/agap/app/captions/`: `CaptionEngine`, `CaptionListener`, `Transcript`,
  `RecognizerErrors`, `GoogleOnDeviceEngine`, `GoogleOnDeviceSupport`, `CaptionRelay`.
- New, `app/src/main/java/ph/agap/app/ui/captions/`: `CaptionsFragment`, `CaptionsViewModel`.
- New resources: `layout/fragment_captions.xml`; drawables `ic_mic`, `ic_stop`, `ic_delete`,
  `bg_caption_box`, `bg_note`, `bg_status_dot`; colour lists `toggle_*`, `button_background`, `button_text`.
- Changed: `AndroidManifest.xml` (RECORD_AUDIO, `<queries>` for the recognition service), `MainActivity`
  (Captions tab), `attrs.xml` and `themes.xml` (`agapCaptionBackground`, `agapNote`), `styles.xml` (disabled
  look for filled buttons, `Widget.AGAP.Button.Outlined`, `Widget.AGAP.ToggleButton`), `strings.xml`,
  `app/build.gradle` (0.2.0, versionCode 2).
- New tests, `app/src/test/java/ph/agap/app/captions/`: `TranscriptTest` (8), `RecognizerErrorsTest` (4),
  `GoogleOnDeviceSupportTest` (7).

## Decisions made and why

- D-013: Google offline recognition. The Xiaomi has no system on-device recognizer, so AGAP binds to Google's
  speech service with `EXTRA_PREFER_OFFLINE` ("only use an offline engine"); the English pack was installed.
- D-014: the Vosk English fallback for phones without Google offline English moves to Phase 4, where Vosk is
  integrated for Filipino anyway (owner's choice).
- D-015: one recognizer session per sentence with immediate restarts; a transcript that neither drops nor
  repeats words across restarts; partial words styled without relying on colour; logs without caption text.

## How it was verified

All on the Xiaomi 13 (adb serial `44647d98`), build 0.2.0 debug unless noted.

| Check | Result |
|---|---|
| `testDebugUnitTest` | 25 of 25 passed (19 new) |
| `lintDebug` | 0 errors, 13 warnings (K-003) |
| Permission flow | Explanation card shown without the permission; Start greyed out; system dialog opened from "Allow microphone"; after granting, the pack check ran |
| Recognizer check | No system on-device recognizer; Google speech service found; en-US pack installed |
| Online, PC voice (David), 5-6 sentences, 5 runs | 3 or 4 sentences per run captioned in full; the others gave nothing or only their first words (K-007); numbers as digits (K-006) |
| Online, PC voice (Zira), 6 sentences | 3 sentences captioned in full |
| Airplane mode on (Wi-Fi off, no default network, ping failed), PC voice, 3 runs | Same pattern as online: 3 sentences in full per run, plus the first words of one more in two runs |
| Airplane mode, real voice reading 6 sentences | 6 of 6 sentences word for word; raw word error rate 8.3% (an extra "Hello" before the script, and "12" and "5" as digits) |
| Silence for 60 s while listening | 3 sessions, 2 silence restarts, no failures, no crash |
| Stop | Android's recording log shows the recognizer's recording stopped at Stop; nothing recorded until the next Start |
| Home pressed while listening | Recording stopped at once; "stop" logged; back in the app the screen showed "Not listening" |
| Keep screen on | `FLAG_KEEP_SCREEN_ON` on the window only while listening (`dumpsys window`) |
| High contrast | Captions on black, white text, grey underlined partial words; screenshot taken |
| Crashes | None in logcat during the session |

**Not verified:** anything on the Infinix Hot 70; the one-time pack download (the pack was already
installed); the "blocked permission" state (only the first-time explanation was seen); the text-size
setting on this screen; that the text survives leaving the app (it is kept in memory, and survived the
switch to high contrast and back); long sessions beyond a few minutes; a phone with the system on-device recognizer.

## Contract items covered

- 1.3 Live Captions, English: built and checked on my phone. Filipino in Phase 4.
- 1.2 Offline: English captions worked in airplane mode; no caption text saved or logged.
- 1.10 One microphone user, on screen only, screen kept on: the captions side is done; Sound Alerts in Phase 3.
- 1.7 Settings: high contrast checked on the new screen.
- 1.11 (prepared): `CaptionRelay` gives the watch link the newest caption line.

## Known issues and scope flags

- K-006 numbers as digits; K-007 synthetic voices partly rejected; K-008 no English captions on phones
  without Google offline English until Phase 4; K-009 the Xiaomi drops off adb at times.
- No scope flags.

## Next phase

Phase 3, Sound Alerts engine (target 2026-11-08, completes M1): YAMNet through the MediaPipe Audio Classifier,
the 13 Annex A alerts, and the hand-over of the microphone between Sound Alerts and Live Captions.
