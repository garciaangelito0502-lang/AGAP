# Changelog

One line per notable change, newest first.

- 2026-11-04 Phase 2: phone app 0.2.0: Live Captions in English with Google offline speech recognition (Android's on-device recognizer, or Google's speech service in offline-only mode); language picker (Filipino in Phase 4); microphone permission screens; one-time English pack download; Start/Stop/Clear; screen kept on while listening; stops when the screen leaves view.
- 2026-11-04 Phase 2: `Transcript`, error policy and pack-support logic with 19 new JVM unit tests (25 in total); `CaptionRelay` hook for the watch (Phase 7).
- 2026-11-04 Phase 2: filled buttons show a grey disabled state; new outlined and toggle button styles; caption box on plain white or black.
- 2026-11-04 Phase 0 done: development phone Xiaomi 13 (Android 16) connected over USB and recorded in DEVICE-NOTES.md; still no watch.

- 2026-10-27 Phase 1: Gradle project (AGP 9.4.1, Gradle 9.8.2 wrapper) with `:app` and `:wear`, one shared release-signing script, version catalog.
- 2026-10-27 Phase 1: phone app 0.1.0: Home, bottom navigation (Home, Captions, Alerts, History), Settings (text size, high contrast, vibration, alert threshold), About, placeholders for the screens of later phases; light and high-contrast themes; "AGAP" text launcher icon.
- 2026-10-27 Phase 1: `SettingsStore` with 6 JVM unit tests; lint fixes (decorative icons hidden from screen readers, data-extraction rules, no duplicate backgrounds).
- 2026-10-27 Phase 1: watch app shell 0.1.0 (same applicationId and key as the phone).

- 2026-10-27 Phase 0: created the repo, `.gitignore`, `.gitattributes`, README, PROGRESS.md and the docs set (DECISIONS, SCOPE-FLAGS, KNOWN-ISSUES, SPEC-CHECKLIST, DEVICE-NOTES). No app code yet.
