# Contract checklist

One row for each contract item 1.1 to 1.13 and each Annex A alert. Updated every phase.

- **Built:** No / Partly / Yes.
- **Verified on:** emulator / my phone / Infinix / watch / not verified. "Pending: client checks it" means the
  check needs the client's Infinix Hot 70 and is in the milestone test checklist for the client's team.

Last updated: 2026-11-06 (Phase 3). Emulator = Android 15 virtual device `AGAP_API35` (D-012). "My phone" = the
development Xiaomi 13 on Android 16 (D-005, DEVICE-NOTES.md).

## Contract section 1

| Item | What it is | Phase | Built | Verified on | Notes |
|---|---|---|---|---|---|
| 1.1 | Phone app: Java 17, XML layouts, Android 10+ (minSdk 29), checked on the Infinix Hot 70 | 1 (then all) | Partly | emulator; Infinix: pending: client checks it | Phase 1: app skeleton in Java 17 + XML Views, minSdk 29, builds, installs and opens on the emulator |
| 1.2 | Fully offline; no audio recorded or saved; no accounts; no cloud database | all, proof in 8 | Partly | emulator; my phone (captions and Sound Alerts in airplane mode) | Phase 1: no INTERNET permission (checked with `dumpsys package`); backup and device transfer excluded. Phase 2: English captions worked in airplane mode on my phone (Wi-Fi off, no network, ping failed); recognition runs in Google's speech service on the phone in offline-only mode (D-013); caption text is never saved or logged. Full airplane-mode proof in Phase 8. Phase 3: Sound Alerts ran in airplane mode on my phone; the APK asks for no network permission (MediaPipe's statistics permissions removed, D-016); audio stays in memory, the log holds alert and class names with scores only |
| 1.3 | Live Captions: English (Google on-device, Vosk fallback) or Filipino (Vosk tl-ph), one at a time, Settings text size and contrast | 2 (English), 4 (Filipino) | Partly | my phone (English, online and airplane mode); Infinix: pending: client checks it | Phase 2: English with Google offline recognition (D-013): 6 of 6 sentences from a real voice in airplane mode. Language picker; Filipino disabled until Phase 4. Vosk English fallback moved to Phase 4 (D-014). Transcript in memory only, kept after Stop until Clear or Start, no FLAG_SECURE. High contrast checked on my phone; text size follows the setting (sp) but was not checked on this screen |
| 1.4 | Sound Alerts: YAMNet through MediaPipe Audio Classifier, built-in classes, Annex A, 3 categories | 3 | Yes | my phone (airplane mode, detection check); Infinix: pending: client checks it | Fire/smoke alarms, car horns, doorbell/knocking checked first. Phase 3: built (D-016 to D-018): YAMNet through MediaPipe Audio Classifier 1.2.2, model in the app, Annex A as data checked against the model's own labels (unit test). Detection check in airplane mode: 140 + 120 trials from the TV speakers and one real knock; car horn and smoke alarm (as Fire Alarm) fire, knock and doorbell did not reach 50%; numbers in DETECTION-CHECK.md, K-010 to K-014. No accuracy claimed |
| 1.5 | Alert screen (icon, label, category, confidence, time) and one vibration pattern per category | 5 | No | not verified; Infinix: pending: client checks it | Category colours and row attributes ready in both themes |
| 1.6 | Event History in Room: label, category, confidence, detected time and shown time to the ms; detail view | 5 | No | not verified | History tab and placeholder exist. Every alert raised is saved (the study counts TP/FP/FN/TN) |
| 1.7 | Settings: text size, high contrast, vibration on/off, alert threshold | 1, wired in 3, 5, 6 | Partly | emulator; my phone (high contrast on Live Captions); Infinix: pending: client checks it | All four saved and kept after the app is killed (emulator). Text size and high contrast apply to every screen. Vibration and threshold are stored but not used until Phases 3 and 5. Phase 3: Sound Alerts reads the alert threshold at each Start (log: threshold 0.5); a changed value was not tried on the phone |
| 1.8 | Home, Sound Categories, Vibration Patterns guide (Figure 1 and Figma draft) | 1 (shell), 6 | Partly | emulator | Home built from the mockups (D-004); Sound Categories and Vibration Patterns are placeholders |
| 1.9 | Demonstration Mode, clearly labelled, no microphone, alerts not saved | 5 | No | not verified | Placeholder screen reachable from Home |
| 1.10 | One microphone user at a time; listening only with app on screen; screen kept on | 2, 3, 8 | Partly | my phone (captions and Sound Alerts); Infinix: pending: client checks it | Phase 2: Stop releases the microphone, and leaving the screen (Home) stops captions, both seen in Android's recording log; the screen is kept on only while listening (`dumpsys window`). Only one tab's screen exists at a time (D-009); the Sound Alerts side comes in Phase 3. Phase 3: Sound Alerts side checked on my phone: opening Captions stops Sound Alerts before captions record, with no overlap in Android's recording log; Home stops it; screen kept on only while listening (`dumpsys window`). `MicOwner` guard with 4 unit tests (D-019) |
| 1.11 | Wear OS 3+ watch app: alerts with category vibration, live caption text; Bluetooth, no internet | 7 | Partly | not possible (no watch, D-005) | Phase 1: `:wear` shell builds; same applicationId and same signing key as the phone, checked with `apksigner` for debug and release |
| 1.12 | Testing support until 2026-12-07 (contract: 2026-11-23; schedule reset 2026-11-04): bug fixes, threshold and class tuning | 10 | No | not verified | |
| 1.13 | Turnover: source on GitHub, phone and watch APKs, English install guide, remote help | 9 | Partly | not verified | Standard Gradle project with wrapper; README has build steps. Not pushed |

## Annex A alerts

| # | Category | Alert | YAMNet classes | Phase | Built | Verified on | Notes |
|---|---|---|---|---|---|---|---|
| A1 | Emergency | Fire Alarm | Fire alarm | 3 | Yes | my phone (fired for the smoke alarm clip); Infinix: pending: client checks it | Formal test sound. Fired at 0.89-0.97 for the smoke alarm clip (K-010). The three-beep NFPA fire alarm clip never scored as Fire alarm, even from the file (K-011) |
| A2 | Emergency | Smoke Alarm | Smoke detector, smoke alarm | 3 | Yes | my phone (did not fire); Infinix: pending: client checks it | Formal test sound. Never fired: YAMNet names the test smoke alarm "Fire alarm"; Smoke detector at most 0.33 (K-010) |
| A3 | Emergency | Emergency Vehicle Siren | Emergency vehicle; Police car (siren); Ambulance (siren); Fire engine, fire truck (siren) | 3 | Yes | not verified on purpose | Fired as a wrong alert during the smoke alarm clip (Police car (siren)). No siren tried |
| A4 | Emergency | Warning Siren | Civil defense siren; Siren | 3 | Yes | not verified on purpose | Fired as a wrong alert during the smoke alarm clip (Siren). No siren tried |
| A5 | Traffic | Vehicle Horn | Vehicle horn, car horn, honking; Air horn, truck horn | 3 | Yes | my phone (fired); Infinix: pending: client checks it | Formal test sound. 5 of 21 car-horn trials from the TV speakers at 0.50-0.85; the file itself scores 0.80-0.98, so the speakers lose most of it (K-014) |
| A6 | Traffic | Reversing Alarm | Reversing beeps | 3 | Yes | not verified | Not tried |
| A7 | Traffic | Vehicle Nearby | Car passing by; Motorcycle; Motor vehicle (road) | 3 | Yes | not verified | Not tried |
| A8 | Household/Entry | Doorbell | Doorbell; Ding-dong | 3 | Yes | my phone (did not fire); Infinix: pending: client checks it | Formal test sound. 0 of 36 doorbell trials with the app's microphone source fired it (highest 0.41); the two doorbell files score at most 0.33 and 0.50 even without speakers (K-011, K-014) |
| A9 | Household/Entry | Knocking | Knock | 3 | Yes | my phone (did not fire); Infinix: pending: client checks it | Formal test sound. Knock clip lost by the TV speakers (0.00); a real knock scored 0.33 ("Bouncing" was YAMNet's top class) (K-014) |
| A10 | Household/Entry | Phone Ringing | Telephone bell ringing; Ringtone; Telephone | 3 | Yes | not verified on purpose | Fired as a wrong alert for plain beeps ("Telephone", K-013) and twice for unknown room sounds. No ringtone tried |
| A11 | Household/Entry | Alarm Clock | Alarm clock | 3 | Yes | not verified on purpose | Fired as a wrong alert for the three-beep fire alarm clip (K-011). No alarm clock tried |
| A12 | Household/Entry | Appliance Timer | Beep, bleep; Buzzer; Microwave oven | 3 | Yes | not verified on purpose | Fired as a wrong alert for the fire alarm and beeps clips ("Beep, bleep"). No appliance timer tried |
| A13 | Household/Entry | Glass Breaking | Shatter; Glass; Breaking | 3 | Yes | not verified on purpose | Fired as a wrong alert for a doorbell and the fire alarm clip ("Glass"). No glass tried |

Class names are matched against YAMNet's own label list, not retyped strings (a class name can contain a comma);
`AnnexATest` fails if any is missing from the bundled model. "Built: Yes" means the alert is in the mapping and can
fire. "Not verified on purpose" means its sound was not played on purpose; it may have fired as a wrong alert.
Detection numbers: DETECTION-CHECK.md.
