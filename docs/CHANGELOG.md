# Changelog

One line per notable change, newest first.

- 2026-11-08 M2 update: test build 0.9.1 (`agap-phone-0.9.1-test.apk`, phone 0.8.0 debug, SHA-256 ccf41b4e...) replaces the phone file of 0.9 for the alpha test; the watch file of 0.9 stays.
- 2026-11-08 Phase 8: phone app 0.8.0 (code 10): offline proof, ANR audit, robustness checks and long run on my phone; watch app unchanged (0.7.0). 104 JVM unit tests and 12 instrumented tests pass.
- 2026-11-08 Phase 8: only one Vosk model load at a time; a Start stopped while waiting does not load (D-052, fixes K-029: 11 loads at once reached 5.3 GB).
- 2026-11-07 Phase 8: the listening dot and the alert badge pulse in 100 ms steps (`SlowPulse`, D-051): 19% instead of 54% of a processor core while listening (fixes K-028); 4 new unit tests.
- 2026-11-07 Phase 8: a change of the phone's dark mode no longer rebuilds the main screen and stops listening (`configChanges="uiMode"`, D-048, fixes K-025).
- 2026-11-07 Phase 8: starting Sound Alerts frees the Filipino model's memory (D-049, fixes K-026).
- 2026-11-07 Phase 8: Sound Alerts and Vosk captions say when Android silences the microphone for a call or another app ("AGAP hears nothing right now"), and go on listening (D-047).
- 2026-11-07 Phase 8: ANR audit fixes: Stop waits only while the microphone is open; the Vosk model is freed and deleted off the main thread (D-046, fixes K-027).
- 2026-11-07 M2: test build 0.9 (`agap-phone-0.9-test.apk`, phone 0.7.0 debug; `agap-watch-0.9-test.apk`, watch 0.7.0 debug) and `docs/CLIENT-TEST-CHECKLIST-0.9.md`.
- 2026-11-07 Phase 7: watch app 0.7.0: one screen with the phone's status ("Phone not connected", "Phone connected", "Sound Alerts on", Demonstration Mode), live caption text (large, scrollable, last 30 lines) and the full-screen alert in the category's colour and badge shape with its vibration; tap to close (D-044).
- 2026-11-07 Phase 7: phone app 0.7.0: `WatchSender` sends alerts, caption text (at most 4 partial updates a second) and the phone's state to a connected AGAP watch over the Wearable Data Layer (play-services-wearable 20.0.1), never waiting and doing nothing without a watch (D-042); Home says whether a watch is connected.
- 2026-11-07 Phase 7: new `:shared` library with the alert categories, vibration patterns, icons, badge shapes and the watch message format, used by phone and watch (D-043); 11 new JVM unit tests (100 in total: phone 89, shared 7, watch 4).
- 2026-11-07 Phase 7: watch alerts with the AGAP watch screen closed arrive as a notification that vibrates in the category's pattern (K-022); debug-only `DebugMessageReceiver` to test the watch on an emulator (D-045).

- 2026-11-07 Phase 6 addendum: phone app 0.6.1: the group's logo in Home's top bar (instead of the word "AGAP") and at the top of About (D-036).
- 2026-11-07 Phase 6: phone app 0.6.0: Sound Categories and Vibration Patterns guide (Try) built from the alert data (D-037); Home status card "Ready to work offline" (D-038); the Alerts screen shows the threshold in use (D-039).
- 2026-11-07 Phase 6: accessibility pass (D-040): duplicate and redundant screen-reader labels fixed in Settings, Vibration guide cards stacked, alert screen fits at the largest text size; unused placeholder screen, chip drawable and strings removed.
- 2026-11-07 Phase 6: the group's logo as the launcher icon of phone and watch, transparent background, with a themed-icon layer (D-036).
- 2026-11-07 Phase 6: Espresso 3.7.0 smoke tests with Google's accessibility checks: navigation (4) and settings (4), 12 instrumented tests in total (D-041).
- 2026-11-07 Phase 5 addendum: phone app 0.5.1: Event History export to a CSV file in Download/AGAP, with Share (D-035; scope flag approved by the owner); 5 new JVM unit tests (85 in total).
- 2026-11-07 Phase 5: phone app 0.5.0: full-screen alert over the app (icon, label, category, confidence, time, vibration rhythm, Dismiss); Sound Alerts keeps listening behind it (D-028).
- 2026-11-07 Phase 5: one vibration pattern per category, played as an alarm, honouring the Vibration setting (D-031); `VIBRATE` permission.
- 2026-11-07 Phase 5: Event History in Room 2.8.5 with both times to the millisecond (shown time from the frame-commit callback, D-029), day-grouped list, detail view with response time, clear with confirmation; schema exported to `app/schemas/` (D-030).
- 2026-11-07 Phase 5: Demonstration Mode: banner on every screen, sample captions, 13 sample alerts, microphone never opened, nothing saved, off after a restart (D-032).
- 2026-11-07 Phase 5: 16 Material Symbols icons and 3 badge shapes (D-033); 14 new JVM unit tests (80 in total) and an instrumented DAO test (4 tests) run with `am instrument` (D-034). Unused placeholder screen removed.
- 2026-11-07 Phase 4: phone app 0.4.0 (debug, 431 MB): Filipino Live Captions with Vosk `vosk-model-tl-ph-generic-0.6` bundled in the APK; language picker enabled; one-time unpack to the phone's storage with a progress bar, a free-storage check and a clean restart after an interrupted unpack.
- 2026-11-07 Phase 4: Vosk English fallback (`vosk-model-small-en-us-0.15`) for phones without Google offline English (closes K-008); the screen says when it is in use.
- 2026-11-07 Phase 4: `VoskCaptionEngine` feeds the microphone to Vosk itself instead of Vosk's `SpeechService` (D-025); punctuation-only words dropped from Vosk results (D-026); only one Vosk model kept in memory, with a fix for a cross-language race (D-024).
- 2026-11-07 Phase 4: `fetchVoskModels` Gradle task downloads and checks both models on the first build (they are not in git, D-022); About credits name the Vosk library and both models; 21 new JVM unit tests (66 in total).
- 2026-11-07 Phase 4: debug-only switches `force_vosk_english` and `fake_free_mb` (D-027); test-only `org.json` 20260814.
- 2026-11-06 M1: test build 0.1 (`agap-phone-0.1-test.apk`, app 0.3.0 debug) and `docs/CLIENT-TEST-CHECKLIST-0.1.md`.
- 2026-11-06 Phase 3: detection check in airplane mode (`docs/DETECTION-CHECK.md`): 140 trials from the TV speakers, the same clips through the model without speakers, 4 microphone sources compared, one real knock. No threshold or class changed (D-021); findings K-010 to K-014.
- 2026-11-06 Phase 3: debug builds can take a test microphone source from `agap_debug` preferences (D-020); the log names the source in use.
- 2026-11-04 Phase 3: phone app 0.3.0: Sound Alerts listening with YAMNet through the MediaPipe Audio Classifier 1.2.2 (model bundled); the 13 Annex A alerts as data (`assets/annex_a.tsv`); one alert per window with a quiet period; minimal Sound Alerts screen (Start/Stop, listening indicator, last alert with category colours, testing info line).
- 2026-11-04 Phase 3: `MicOwner` guard: Live Captions and Sound Alerts never hold the microphone together; 20 new JVM unit tests (45 in total), including Annex A against the model's own label list.
- 2026-11-04 Phase 3: network permissions brought in by MediaPipe's statistics library removed; APK limited to ARM processors with compressed native libraries (about 52 MB).
- 2026-11-04 Phase 2: phone app 0.2.0: Live Captions in English with Google offline speech recognition (Android's on-device recognizer, or Google's speech service in offline-only mode); language picker (Filipino in Phase 4); microphone permission screens; one-time English pack download; Start/Stop/Clear; screen kept on while listening; stops when the screen leaves view.
- 2026-11-04 Phase 2: `Transcript`, error policy and pack-support logic with 19 new JVM unit tests (25 in total); `CaptionRelay` hook for the watch (Phase 7).
- 2026-11-04 Phase 2: filled buttons show a grey disabled state; new outlined and toggle button styles; caption box on plain white or black.
- 2026-11-04 Phase 0 done: development phone Xiaomi 13 (Android 16) connected over USB and recorded in DEVICE-NOTES.md; still no watch.

- 2026-10-27 Phase 1: Gradle project (AGP 9.4.1, Gradle 9.8.2 wrapper) with `:app` and `:wear`, one shared release-signing script, version catalog.
- 2026-10-27 Phase 1: phone app 0.1.0: Home, bottom navigation (Home, Captions, Alerts, History), Settings (text size, high contrast, vibration, alert threshold), About, placeholders for the screens of later phases; light and high-contrast themes; "AGAP" text launcher icon.
- 2026-10-27 Phase 1: `SettingsStore` with 6 JVM unit tests; lint fixes (decorative icons hidden from screen readers, data-extraction rules, no duplicate backgrounds).
- 2026-10-27 Phase 1: watch app shell 0.1.0 (same applicationId and key as the phone).

- 2026-10-27 Phase 0: created the repo, `.gitignore`, `.gitattributes`, README, PROGRESS.md and the docs set (DECISIONS, SCOPE-FLAGS, KNOWN-ISSUES, SPEC-CHECKLIST, DEVICE-NOTES). No app code yet.
