# Contract checklist

One row for each contract item 1.1 to 1.13 and each Annex A alert. Updated every phase.

- **Built:** No / Partly / Yes.
- **Verified on:** emulator / my phone / Infinix / watch / not verified. "Pending: client checks it" means the
  check needs the client's Infinix Hot 70 and is in the milestone test checklist for the client's team.

Last updated: 2026-10-27 (Phase 0). No app code exists yet.

## Contract section 1

| Item | What it is | Phase | Built | Verified on | Notes |
|---|---|---|---|---|---|
| 1.1 | Phone app: Java 17, XML layouts, Android 10+ (minSdk 29), checked on the Infinix Hot 70 | 1 (then all) | No | not verified; Infinix: pending: client checks it | JDK 17 and SDK installed (Phase 0) |
| 1.2 | Fully offline; no audio recorded or saved; no accounts; no cloud database | all, proof in 8 | No | not verified | |
| 1.3 | Live Captions: English (Google on-device, Vosk fallback) or Filipino (Vosk tl-ph), one at a time, Settings text size and contrast | 2 (English), 4 (Filipino) | No | not verified; Infinix: pending: client checks it | Transcript kept on screen in memory only, no FLAG_SECURE (study needs screenshots) |
| 1.4 | Sound Alerts: YAMNet through MediaPipe Audio Classifier, built-in classes, Annex A, 3 categories | 3 | No | not verified; Infinix: pending: client checks it | Fire/smoke alarms, car horns, doorbell/knocking tuned first |
| 1.5 | Alert screen (icon, label, category, confidence, time) and one vibration pattern per category | 5 | No | not verified; Infinix: pending: client checks it | |
| 1.6 | Event History in Room: label, category, confidence, detected time and shown time to the ms; detail view | 5 | No | not verified | Every alert raised is saved (the study counts TP/FP/FN/TN) |
| 1.7 | Settings: text size, high contrast, vibration on/off, alert threshold | 6 | No | not verified | |
| 1.8 | Home, Sound Categories, Vibration Patterns guide (Figure 1 and Figma draft) | 1 (shell), 6 | No | not verified | Design from the 2026-10-27 mockups (D-004) |
| 1.9 | Demonstration Mode, clearly labelled, no microphone, alerts not saved | 5 | No | not verified | |
| 1.10 | One microphone user at a time; listening only with app on screen; screen kept on | 2, 3, 8 | No | not verified; Infinix: pending: client checks it | |
| 1.11 | Wear OS 3+ watch app: alerts with category vibration, live caption text; Bluetooth, no internet | 7 | No | not possible (no watch, D-005) | Same applicationId and signing key as the phone (D-003) |
| 1.12 | Testing support until 2026-11-23: bug fixes, threshold and class tuning | 10 | No | not verified | |
| 1.13 | Turnover: source on GitHub, phone and watch APKs, English install guide, remote help | 9 | No | not verified | Repo created (Phase 0); not pushed |

## Annex A alerts

| # | Category | Alert | YAMNet classes | Phase | Built | Verified on | Notes |
|---|---|---|---|---|---|---|---|
| A1 | Emergency | Fire Alarm | Fire alarm | 3 | No | not verified; Infinix: pending: client checks it | Formal test sound |
| A2 | Emergency | Smoke Alarm | Smoke detector, smoke alarm | 3 | No | not verified; Infinix: pending: client checks it | Formal test sound |
| A3 | Emergency | Emergency Vehicle Siren | Emergency vehicle; Police car (siren); Ambulance (siren); Fire engine, fire truck (siren) | 3 | No | not verified | |
| A4 | Emergency | Warning Siren | Civil defense siren; Siren | 3 | No | not verified | |
| A5 | Traffic | Vehicle Horn | Vehicle horn, car horn, honking; Air horn, truck horn | 3 | No | not verified; Infinix: pending: client checks it | Formal test sound |
| A6 | Traffic | Reversing Alarm | Reversing beeps | 3 | No | not verified | |
| A7 | Traffic | Vehicle Nearby | Car passing by; Motorcycle; Motor vehicle (road) | 3 | No | not verified | |
| A8 | Household/Entry | Doorbell | Doorbell; Ding-dong | 3 | No | not verified; Infinix: pending: client checks it | Formal test sound |
| A9 | Household/Entry | Knocking | Knock | 3 | No | not verified; Infinix: pending: client checks it | Formal test sound |
| A10 | Household/Entry | Phone Ringing | Telephone bell ringing; Ringtone; Telephone | 3 | No | not verified | |
| A11 | Household/Entry | Alarm Clock | Alarm clock | 3 | No | not verified | |
| A12 | Household/Entry | Appliance Timer | Beep, bleep; Buzzer; Microwave oven | 3 | No | not verified | |
| A13 | Household/Entry | Glass Breaking | Shatter; Glass; Breaking | 3 | No | not verified | |

Class names are matched against YAMNet's own label list, not retyped strings (a class name can contain a comma).
