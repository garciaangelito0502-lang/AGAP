# AGAP progress

Last updated: 2026-11-06 (Phase 3 done and M1 test build ready, ahead of the 2026-11-08 target)

| Phase | Name | Status | Date finished | Build/APK version |
|---|---|---|---|---|
| 0 | Environment and plan | Done (development phone Xiaomi 13 on adb since 2026-11-04; no watch) | 2026-11-04 | none (no app code) |
| 1 | Project skeleton and shell UI | Done (checked on the emulator; Infinix check pending with the client) | 2026-10-27 | 0.1.0 debug (phone and watch) |
| 2 | Live Captions, English | Done (checked on my phone, online and in airplane mode; Infinix check pending with the client; Vosk English fallback moved to Phase 4) | 2026-11-04 | 0.2.0 debug (phone) |
| 3 | Sound Alerts engine | Done (checked on my phone in airplane mode; knock and doorbell scored low, see docs/DETECTION-CHECK.md; Infinix check pending with the client) | 2026-11-06 | 0.3.0 debug (phone); M1 test build `agap-phone-0.1-test.apk` |
| 4 | Live Captions, Filipino | Not started | | |
| 5 | Alert screen, vibration, Event History, Demonstration Mode | Not started | | |
| 6 | Remaining screens, settings wiring and accessibility polish | Not started | | |
| 7 | Wear OS companion | Not started | | |
| 8 | Offline proof, hardening and tests | Not started | | |
| 9 | Release build, install guide and turnover | Not started | | |
| 10 | Support and tuning until 2026-12-07 | Not started | | |

Milestones (schedule of 2026-11-04; the contract section 5 dates of 10-28, 11-03, 11-07 and 11-23 have passed):
**M1** 2026-11-08 first phone test build (after Phase 3; the group installs it on the Infinix 11-09),
**M2** 2026-11-18 complete test build of phone and watch (after Phase 7),
**M3** 2026-11-21 final build and turnover (Phase 9), support until 2026-12-07 (Phase 10).
The group's testing: alpha (2 days) 11-19 to 11-20 on M2, beta (1 week) 11-23 to 11-29 on 1.0, their data collection from 11-30.
Phase targets: 2 on 11-07, 3 on 11-08, 4 on 11-09 to 11-11, 5 on 11-12 to 11-14, 6 on 11-15, 7 on 11-16 to 11-18,
8 on 11-19 to 11-20, 9 on 11-21. Student copy: `reports/AGAP-Timeline-from-2026-11-07.pdf` (outside this repo).

Phase notes: [docs/phases/](docs/phases/). Contract checklist: [docs/SPEC-CHECKLIST.md](docs/SPEC-CHECKLIST.md).
