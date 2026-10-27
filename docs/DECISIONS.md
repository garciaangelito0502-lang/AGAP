# Decisions

Numbered, newest last. Each entry: what was decided, why, and the phase.

## D-001 Build PC toolchain (Phase 0, 2026-10-27)

- **JDK:** Eclipse Temurin 17.0.20.1+1 (zip, SHA-256 checked against the Adoptium release), unpacked to
  `D:\Android\jdk-17`, set as `JAVA_HOME`. Java 17 because contract 1.1 fixes JDK 17.
- **Android SDK:** installed with Google's `android` CLI 1.1.16717390 into the default
  `%LOCALAPPDATA%\Android\Sdk`: platform-tools 37.0.1 (adb 1.0.41), platforms/android-36 (r02),
  build-tools 36.0.0. No Android Studio on the build PC; the command-line SDK and the Gradle wrapper are enough.
  The project keeps the standard Gradle layout so the client can open it in Android Studio.
- The `android` CLI is run with `--no-metrics` (it sends usage data to Google by default). This is about the
  build tool only; the app itself has no analytics.

## D-002 SDK levels and plugin version: chosen in Phase 1 (Phase 0, 2026-10-27)

Facts gathered for the choice (Android docs, "About Android Gradle plugin", fetched 2026-10-27): the newest AGP
is 9.4 (needs Gradle 9.6.0); API 37 needs AGP 9.1.1 or newer; API 36.1 needs AGP 8.13.0 or newer. Working
assumption: compileSdk and targetSdk 36 (Android 16), minSdk 29 (contract 1.1). The client's test phone runs
Android 15 (API 35). Final versions are recorded here in Phase 1 after a test build.

## D-003 One release keystore for phone and watch (Phase 0, 2026-10-27)

- File: `D:\Project\AGAP\keys\agap-release.jks` on the build PC, **outside this repo**. PKCS12, alias `agap`,
  RSA 4096, SHA384withRSA, valid until 2054-03-14. Certificate SHA-256:
  `AB:39:7C:85:59:D5:1D:52:32:1E:39:61:9C:1C:A5:77:58:17:F8:5F:CE:94:B2:C2:33:20:77:55:B9:2D:80:64`.
- Passwords are in `D:\Project\AGAP\keys\keystore.properties` next to it. Neither file is ever committed
  (`.gitignore` blocks `*.jks`, `*.keystore`, `*.p12`, `keystore.properties`).
- Why one key: the phone and watch apps must share the applicationId **and** the signing key, or the Wear Data
  Layer will not connect them. Debug builds of both modules use the same debug keystore on the build PC, so
  this also holds during development.
- Open (Phase 9): how the keystore is handed to the client. Anyone who ships an update later needs this same
  key, or Android refuses to install the update over the old app.

## D-004 Screen design source (Phase 0, 2026-10-27)

No PNG exports of the client's Figma frames and no AGAP logo were available. The screens follow the screen
mockups made on 2026-10-27 (15 frames: Home, Live Captions, Sound Alerts, Settings, the three alert screens,
Event History, alert detail, Sound Categories, Vibration Patterns, Demonstration Mode, two high-contrast frames
and the watch), which were drawn from the contract text, the readable Figma preview and the approved colour
palette (design note "AGAP colour palette - choice and rationale", 2026-10-27). The palette replaces the Figma
draft's colours. Methods Figure 1 could not be rendered on the build PC. The launcher icon is a plain "AGAP"
text icon. If the client sends Figma exports or a logo later, this entry is updated.

## D-005 Development phone and watch (Phase 0, 2026-10-27)

- No Android phone was available to connect on 2026-10-27. Until one is connected, nothing can be marked
  verified on a device. Items for the client's Infinix Hot 70 stay "pending: client checks it" in
  SPEC-CHECKLIST.md, and each milestone gets a short manual test checklist for the client's team, who reply
  within 2 days of each test build (contract 6.2).
- No Wear OS watch and no watch emulator. The watch app is still built (contract 1.11), but its on-device
  verification is recorded as "not possible" unless a watch becomes available.

## D-006 Repository layout (Phase 0, 2026-10-27)

`agap-app` is its own git repo with modules `:app` (phone) and `:wear` (watch), standard Gradle layout and the
Gradle wrapper, so it opens in Android Studio. Only app source, README and build/install docs go in it; the
contract, research documents and consent letters stay outside. Gradle memory is capped
(`org.gradle.jvmargs=-Xmx2g`, `org.gradle.workers.max=2`, added in Phase 1) because the build PC has a very
small page file.
