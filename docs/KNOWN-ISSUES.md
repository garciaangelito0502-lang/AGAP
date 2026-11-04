# Known issues

Open bugs and limits of the current build. Closed items move to the bottom with the phase that fixed them.

| ID | Found | Area | Issue | Impact | Status |
|---|---|---|---|---|---|
| K-001 | Phase 1 | Build | AGP 9.4.1 warns that `android.builtInKotlin=false` is deprecated and will be removed in AGP 10. | None now. When AGP 10 is adopted, drop the flag (built-in Kotlin does nothing without Kotlin sources). | Open, accepted |
| K-002 | Phase 1 | Build | Gradle reports a deprecated call (`Configuration.setVisible`) made by AGP 9.4.1 itself, not by AGAP's build scripts. | None. Fixed by a later AGP. | Open, accepted |
| K-003 | Phase 1 | Lint | `lintDebug` has 0 errors and 13 warnings (12 in Phase 1): newer compileSdk/targetSdk and AGP 9.5.0 available (kept at 36 and 9.4.1, D-007), 7 palette colours not used yet (used from Phase 3), `allowBackup` still read on Android 10-11, one overdraw note, one compound-drawable hint. | None. | Open, accepted |
| K-004 | Phase 1 | Testing | No physical phone yet. Phase 1 was checked on the Android 15 emulator only. Since 2026-11-04 a Xiaomi 13 (Android 16) is on adb as the development phone, but it is not the Infinix. | Speed, microphone and look on the Infinix are unknown until the client checks them. | Open (narrowed 2026-11-04) |
| K-005 | Phase 1 | Watch | The `:wear` shell builds but has never been installed: no watch and no watch emulator. | Watch behaviour unverified. | Open (D-005) |
| K-006 | Phase 2 | Captions | The English recognizer writes numbers as digits ("page 12", "5 minutes"). | A word error count against a script that says "twelve" counts a substitution unless numbers are normalised first. The group should write numbers the same way in both, or normalise before counting. | Open, accepted (recognizer behaviour) |
| K-007 | Phase 2 | Captions | With the PC's synthetic voices played through speakers, Google's offline recognizer returned nothing for 1 or 2 of 5-6 sentences per run, mostly the same ones, online and offline. A real voice in airplane mode got 6 of 6. | Seen only with synthetic test audio so far. Watch it in the group's trials. | Open, watching |
| K-008 | Phase 2 | Captions | Phones without Google offline English (Android 12 or older, or no Google speech service) show a "not available" message instead of English captions. | Only such phones. The Xiaomi and the Infinix (Android 15) are Android 13+. Fixed when the Vosk English fallback is added in Phase 4 (D-014). | Open until Phase 4 |
| K-009 | Phase 2 | Testing | The Xiaomi dropped off adb (USB) for a few seconds several times during testing. | Test tooling only: the app kept running. Scripts wait for the device. | Open, accepted |
