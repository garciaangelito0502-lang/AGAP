# Phase 00: Environment and plan

Status: **Blocked** on one step (no phone on adb). Date: 2026-10-27.

## Goal and what was built

Set up the build PC and the project records before any app code. The PC had no JDK, Android SDK or adb.
I installed Java 17 (Temurin), Google's `android` CLI and, through it, the SDK packages needed to build
(platform-tools, the Android 16 platform, build-tools). I created this repository with its `.gitignore`,
`.gitattributes`, README, PROGRESS.md and the docs set, including SPEC-CHECKLIST.md with every contract item
1.1 to 1.13 and all 13 Annex A alerts. I made the single release keystore for the phone and watch apps,
outside the repo. No phone was available, so the device connection step is still open. No watch is available.

## Files and modules added or changed

- `.gitignore`, `.gitattributes`, `README.md`, `PROGRESS.md`
- `docs/CHANGELOG.md`, `docs/DECISIONS.md`, `docs/SCOPE-FLAGS.md`, `docs/KNOWN-ISSUES.md`,
  `docs/SPEC-CHECKLIST.md`, `docs/DEVICE-NOTES.md`, this file
- No Gradle modules yet (`:app` and `:wear` come in Phase 1 and Phase 7).
- Outside the repo: `D:\Android\jdk-17`, `%LOCALAPPDATA%\Android\Sdk`, `D:\Project\AGAP\keys\`.

## Decisions made and why

See DECISIONS.md D-001 to D-006: toolchain versions, SDK levels deferred to Phase 1 (working assumption API 36),
one release keystore for both apps, design from the 2026-10-27 mockups with a text icon, no phone or watch
today, repo layout and Gradle memory caps.

## How it was verified

| Command | Result |
|---|---|
| `java -version` | OpenJDK 17.0.20.1 Temurin |
| `adb version` | 1.0.41, platform-tools 37.0.1 |
| `aapt2 version` (build-tools 36.0.0) | 2.20 |
| `adb devices -l` | empty list: no phone connected |
| `keytool -list -v` on the release keystore | alias `agap`, RSA 4096, valid to 2054-03-14 |
| JDK and Poppler downloads | SHA-256 matched the publishers' checksums |

**Not verified:** a phone connection (`adb devices` shows no device), anything on the Infinix Hot 70, anything
on a watch, a Gradle build (no project yet).

## Contract items covered

- 1.1: build toolchain for Java 17 and Android 10+ in place. No app yet.
- 1.11: one signing key for phone and watch (needed for the Data Layer). No watch for testing.
- 1.13: repository created, not pushed.

## Known issues and scope flags

- Open: no development phone. Phase 0 is not Done until `adb devices` lists a phone as `device` and
  DEVICE-NOTES.md has its model, Android version, free storage and RAM.
- M1 (2026-10-28) is at risk: Phases 1 to 3 are still ahead of it.
- No scope flags.

## Next phase

Phase 1, Project skeleton and shell UI: Gradle project with `:app` (and the `:wear` shell), wrapper, palette in
`colors.xml`, Home screen with navigation to the other screens, no listening yet.
