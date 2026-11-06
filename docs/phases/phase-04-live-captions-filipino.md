# Phase 04: Live Captions, Filipino

Status: **Done**, checked on my phone (Xiaomi 13, Android 16) in airplane mode; the Infinix check is pending
with the client. Finished 2026-11-07 (target 2026-11-09 to 11-11). Build: 0.4.0 (debug), phone, 431 MB.

## Goal and what was built

The Live Captions tab now has two working languages. The user picks English or Filipino before Start; the
buttons are locked while listening, so one session never mixes them. Filipino uses Vosk with the
`vosk-model-tl-ph-generic-0.6` model, bundled in the APK as the contract says. On first use of Filipino a card
asks the user to set it up. Tapping "Set up now" unpacks the model (776 MB) into the app's private storage once,
with a progress bar, the screen kept on, a free-storage check first, and a clean restart if the unpack is cut off.
After that, Filipino starts in under a second. Phones without Google's offline English (Android below 13, or
no Google speech service) now get English captions from Vosk's small English model instead of a "not available"
message (closes K-008), and the screen says which engine is in use. The About text in Settings names the Vosk
library and both models, with the non-commercial note for the Filipino model.

## Files and modules added or changed

- New, `app/src/main/java/ph/agap/app/captions/`: `VoskCaptionEngine` (own capture loop, D-025), `VoskModelSpec`
  (the two bundled models), `VoskModelManager` (state, storage check, unpack thread), `ModelUnpacker` (zip to
  folder with a marker file, plain Java), `VoskModelHolder` (one loaded model kept between sessions), `VoskJson`.
- Changed: `CaptionsFragment` (language picker, setup/unpack/low-storage cards, English fallback),
  `CaptionsViewModel` (language, model managers), `CaptionListener` (`Failure.MODEL`), `MainActivity` (frees the
  model in `onTrimMemory`), `layout/fragment_captions.xml` (Filipino enabled, progress bar), `strings.xml`
  (captions setup texts, About credits).
- Build: `app/build.gradle` (0.4.0, versionCode 4; `fetchVoskModels`; `src/main/assets-vosk` as an asset folder;
  zips stored uncompressed), `gradle/libs.versions.toml` (vosk-android 0.3.75; test-only org.json 20260814),
  `.gitignore` (`app/src/main/assets-vosk/`), README (first build downloads the models).
- New tests: `ModelUnpackerTest` (9), `VoskJsonTest` (6), `VoskModelManagerTest` (6). 66 in total.

## Decisions made and why

- D-022: both Vosk models inside the APK (owner's choice over a side-loaded file); fetched and checked by Gradle,
  not committed (the Filipino zip is over GitHub's 100 MB limit). APK 431 MB.
- D-023: unpack once, by a tap, into `files/vosk/<model>.tmp`, renamed only when complete with a marker; zip
  paths that would leave the folder are refused; needs about 1 GB free for Filipino.
- D-024: vosk-android 0.3.75; one model kept in memory (about 980 MB for Filipino) and freed when not needed;
  fix for a race that could give an English start the Filipino model.
- D-025: AGAP's own capture loop instead of Vosk's `SpeechService` (no crash on a microphone read error, fewer
  screen updates, 2 s audio buffer). Recognition is the same; the record of a misleading test hour is in D-025.
- D-026: words with no letter or digit (the model has 44 such "words", such as ".") are dropped.
- D-027: debug-only switches `force_vosk_english` and `fake_free_mb`.

## How it was verified

All on the Xiaomi 13 (adb `44647d98`), debug build 0.4.0, **airplane mode on** (`airplane_mode_on=1`; ping:
"Network is unreachable"), except the builds.

| Check | Result |
|---|---|
| `testDebugUnitTest` | 66 of 66 pass (21 new) |
| `lintDebug` | 0 errors, 11 warnings (K-003), none in the new code |
| APK and install | 430,977,753 bytes; `adb install -r` over USB in 16.0 s |
| Low storage (`fake_free_mb` = 500, after clearing app data) | "Not enough free storage to set up Filipino captions. They need about 1.0 GB free, and this phone has 500 MB…" with Check again; Start disabled; nothing written to `files/` |
| First-run unpack | Setup card ("about 776 MB of storage"); unpacked in 5,793 ms; 777 MB on disk; marker holds the model version |
| Unpack cut off | App killed at about 21%: only `vosk-model-tl-ph-generic-0.6.tmp` (319 MB) left, no model folder. Next launch offered setup again, removed the `.tmp` and finished in 5,884 ms |
| Later launches | No setup card and no unpack; model loaded in 0.70-0.86 s |
| During a session | Both language buttons disabled; keep-screen-on flag set; screen wake lock released at Stop |
| Memory | Filipino session: PSS 977-993 MB (875 MB native); English fallback: 267-271 MB |
| Language switching in one process | Filipino → English fallback → Filipino: each started and listened; memory followed the model |
| Microphone handover | Android's recording log: captions 02:20:24-02:20:52, Sound Alerts 02:20:57-02:21:04, captions 02:21:10-02:21:21; no overlap |
| English fallback (`force_vosk_english`) | "Google's offline English recognition is not available…" note shown; setup card (71 MB); unpacked in 700 ms; loaded in 0.42-0.51 s |
| Filipino, recorded speech | 12 FLEURS sentences (182 words, one 116 s file) from the KMS-268Pro Bluetooth speaker: all 12 captioned; raw word error 14.8% on the final build (25 substituted, 2 dropped). Names and borrowed words were the usual errors (K-016) |
| Filipino, owner's voice | 6 everyday sentences: 6 of 6 word for word (39 words, 0 errors) |
| English fallback, PC voice (Zira) | 6 of 6 sentences captioned; raw word error 6.4% (47 words) |
| Google English (unchanged code) | Zira through the Bluetooth speaker: 0 of 6, NO_MATCH each time (K-007); owner's voice: 3 of 3 word for word |
| About | Shows the research-prototype sentence and the credits for Vosk, both models (non-commercial note), YAMNet and MediaPipe |

Word error rates were computed roughly (lower case, punctuation removed) from the screen text; they are
observations, not accuracy claims (contract 6.5).

From 02:41 to 02:51 every Filipino run faded after the first one or two sentences. That turned out to be the
test setup, which the owner had moved or adjusted before the runs that worked; the same file then gave 14.8-18.7%
with this engine and 16.5% with Vosk's `SpeechService` (D-025).

**Not verified:** anything on the Infinix (unpack time, load time, memory, recognition); the group's distance
bands; women's voices (FLEURS marks every speaker in its Filipino dev split as male); noisy rooms; sessions
longer than about 2.5 minutes; a phone that really lacks Google offline English (the fallback was forced with
the debug switch); a real "no space left" error in the middle of an unpack (only the check before it); the text
size setting on this screen.

## Contract items covered

- 1.3 Live Captions: Filipino with Vosk tl-ph, bundled; one language at a time; the English fallback.
- 1.2 Offline: both Vosk paths ran in airplane mode; models in the APK; no network permission; no words logged.
- 1.10 One microphone user: Filipino side of the handover checked; screen kept on while listening and unpacking.
- 1.13 Turnover: README says how the first build gets the models.
- Contract 8.2 credits: About names Vosk, the Filipino model (CC BY-NC-SA 4.0, non-commercial) and the English model.

## Known issues and scope flags

- K-016 names and borrowed words in Filipino; K-017 431 MB APK and about 1.3 GB on the phone once Filipino is set
  up; K-018 about 980 MB of memory with Filipino loaded; K-007 updated (Google English and synthetic voices).
  K-008 closed.
- Scope flags: none.

## Next phase

Phase 5: alert screen, vibration, Event History and Demonstration Mode (target 2026-11-12 to 11-14). M1: the
group installs `agap-phone-0.1-test.apk` on the Infinix on 2026-11-09 and sends feedback by 2026-11-11. That
build has no Filipino captions; build 0.4.0 does.
