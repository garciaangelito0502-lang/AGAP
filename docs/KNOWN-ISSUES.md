# Known issues

Open bugs and limits of the current build. Closed items move to the bottom with the phase that fixed them.

| ID | Found | Area | Issue | Impact | Status |
|---|---|---|---|---|---|
| K-001 | Phase 1 | Build | AGP 9.4.1 warns that `android.builtInKotlin=false` is deprecated and will be removed in AGP 10. | None now. When AGP 10 is adopted, drop the flag (built-in Kotlin does nothing without Kotlin sources). | Open, accepted |
| K-002 | Phase 1 | Build | Gradle reports a deprecated call (`Configuration.setVisible`) made by AGP 9.4.1 itself, not by AGAP's build scripts. | None. Fixed by a later AGP. | Open, accepted |
| K-003 | Phase 1 | Lint | `lintDebug` has 0 errors and 12 warnings: newer compileSdk/targetSdk available (kept at 36, D-007), 7 palette colours not used yet (used from Phase 3), `allowBackup` still read on Android 10-11, one overdraw note, one compound-drawable hint. | None. | Open, accepted |
| K-004 | Phase 1 | Testing | No physical phone yet. Phase 1 was checked on the Android 15 emulator only. Since 2026-11-04 a Xiaomi 13 (Android 16) is on adb as the development phone, but it is not the Infinix. | Speed, microphone and look on the Infinix are unknown until the client checks them. | Open (narrowed 2026-11-04) |
| K-005 | Phase 1 | Watch | The `:wear` shell builds but has never been installed: no watch and no watch emulator. | Watch behaviour unverified. | Open (D-005) |
