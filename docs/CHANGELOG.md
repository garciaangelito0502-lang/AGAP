# Changelog

One line per notable change, newest first.

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
