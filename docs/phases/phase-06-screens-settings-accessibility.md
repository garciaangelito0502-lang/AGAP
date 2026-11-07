# Phase 06: Remaining screens, settings wiring and accessibility polish

Status: **Done**, checked on my phone (Xiaomi 13, Android 16); the Infinix check is pending with the client.
Finished 2026-11-07 (target 2026-11-15). Build: 0.6.0 (debug), phone.

## Goal and what was built

Every screen in the contract list is now real. Sound Categories shows the 3 categories and the 13 alerts, each with
its icon, a plain sentence of what it means and the YAMNet sound classes it listens for, read from the app's own
Annex A data. The Vibration Patterns guide shows each category's pattern in words and as bars, with a Try button
to feel it. Home has a "Ready to work offline" card: no internet access, microphone state, and whether Sound
Alerts, English and Filipino captions are ready or need a one-time step. The Alerts screen shows the threshold that
the next Start will use. An accessibility pass with Google's automatic checks and screenshots at the largest text
size led to several fixes. The group's logo is now the app icon, with a transparent background. Espresso smoke tests
cover navigation and the four settings, with accessibility checks on every action.

## Files and modules added or changed

- Rewritten: `ui/info/SoundCategoriesActivity`, `ui/info/VibrationPatternsActivity`; new layouts
  `activity_list_page`, `item_category`, `item_category_alert`, `item_vibration`.
- Changed: `ui/home/HomeFragment` and `fragment_home.xml` (status card), `ui/alerts/AlertsFragment` (threshold
  line), `ui/alerts/AlertLooks` (alert meanings, category taglines), `ui/alerts/AlertOverlayFragment` (fits the
  largest text size), `vibration/AlertVibrator` (`preview`, `canVibrate`), `captions/VoskModelManager`
  (`isInstalled`), `ui/captions/CaptionsViewModel` (English check shared with Home), `ui/settings/SettingsActivity`
  and `activity_settings.xml` (screen-reader labels), `strings.xml`.
- Icon: `mipmap-*/ic_launcher_foreground.png` and `ic_launcher_monochrome.png` (5 densities) and
  `mipmap-anydpi/ic_launcher.xml` in `app` and `wear`; the old text icons removed.
- Removed: `SimplePageActivity`, `activity_simple_page.xml`, `bg_status_chip.xml`, 4 unused strings.
- Build: 0.6.0, versionCode 7; Espresso 3.7.0 and espresso-accessibility 3.7.0 (tests only).
- Tests: `androidTest/ui/NavigationTest` (4), `SettingsTest` (4), helper `TestSupport`. 12 instrumented in total.

## Decisions made and why

- D-036 logo icon, transparent background, themed-icon layer. D-037 Categories and Vibration guide from data.
- D-038 Home status card. D-039 the threshold applies at each Start (listening stops while Settings is open).
- D-040 accessibility pass and its fixes. D-041 Espresso with accessibility checks and animations off.

## How it was verified

All on the Xiaomi 13 (adb `44647d98`), build 0.6.0 debug.

| Check | Result |
|---|---|
| `testDebugUnitTest` | 85 of 85 pass |
| Instrumented tests (`am instrument`, animations off, restored after) | 12 of 12 pass: database 4, navigation 4, settings 4 |
| Accessibility checks (on every Espresso action) | No errors. Warnings fixed in Settings (duplicate labels, "selected" said twice); 3 accepted duplicates remain (D-040) |
| `lintDebug` | 0 errors, 4 warnings (K-003) |
| Settings change behaviour (Espresso) | Threshold 70% saved and shown on the Alerts screen; vibration row toggles the setting; high contrast rebuilds the screen with a black top bar; the largest text size makes the sample text more than 1.5 times larger |
| Home status card | Shows no internet, microphone off, Sound Alerts ready, English ready (Google), Filipino ready |
| Sound Categories, Vibration Patterns | All 13 alerts with icon, meaning and classes; Try vibrates (log: "preview Emergency"), also with vibration off for alerts |
| Largest text size, screenshots | Home, Captions, Alerts, History, Sound Categories, Vibration Patterns and the alert screen ("Emergency Vehicle Siren", with the demo banner): no clipped or broken words after the fixes |
| High contrast | Vibration Patterns readable: category ink on black, white Try buttons |
| App icon | New logo in App info; HyperOS shows a dark plate behind it (K-020) |

Empty and error states (brief item 5), and where each was checked:

| Screen | State | Checked |
|---|---|---|
| Live Captions | No microphone permission; permission blocked | Phase 2, my phone (the blocked state only in code) |
| Live Captions | English pack missing; Filipino model not set up; low storage; setup failed; model will not load | Phases 2 and 4 (pack download not seen, the pack was installed; model-load failure only in code) |
| Live Captions, Sound Alerts | Microphone busy (another app recording) | Code only: the session stops with a message. **Not tried on a phone** |
| Sound Alerts | No permission; Demonstration Mode on; no alert yet | Phases 3 and 5, my phone |
| Event History | No alerts yet; export with nothing to export; clear with nothing to clear | Phase 5, my phone (empty list); the two messages in code |
| Vibration Patterns | Phone cannot vibrate; vibration off for alerts | Off: my phone. No vibrator: code only |
| Home status | Microphone not allowed; English pack needed; Filipino not set up | Code only this phase (my phone has all three ready) |

**Not verified:** anything on the Infinix; TalkBack (the brief asks for checks with TalkBack off); the microphone-busy
path on a phone; the icon on other launchers.

## Contract items covered

- 1.8 Home, Sound Categories, Vibration Patterns guide: done. 1.7 Settings: all four visibly change behaviour.
- Accessibility (Methods; participants include deaf and hard-of-hearing users): labels, 48dp targets, largest text size.

## Known issues and scope flags

- K-020 HyperOS dark plate behind the transparent icon. K-003 and K-019 updated.
- Scope flags: none new.

## Next phase

Phase 7: Wear OS companion (target 2026-11-16 to 11-18, M2). It needs a Wear OS 3+ watch, or at least a
watch emulator, to be verified.
