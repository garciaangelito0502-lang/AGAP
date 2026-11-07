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
- Backed up off the build PC by the developer on 2026-10-27.
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
text icon. If the client sends Figma exports or a logo later, this entry is updated. 2026-11-07: the group's logo arrived and is the launcher icon (D-036).

## D-005 Development phone and watch (Phase 0, 2026-10-27)

- No Android phone was available to connect on 2026-10-27. Until one is connected, nothing can be marked
  verified on a device. Items for the client's Infinix Hot 70 stay "pending: client checks it" in
  SPEC-CHECKLIST.md, and each milestone gets a short manual test checklist for the client's team, who reply
  within 2 days of each test build (contract 6.2).
- No Wear OS watch and no watch emulator. The watch app is still built (contract 1.11), but its on-device
  verification is recorded as "not possible" unless a watch becomes available.
- Update 2026-11-04: the development phone is a Xiaomi 13 on Android 16 (DEVICE-NOTES.md). Checks run on it
  are marked "my phone" in SPEC-CHECKLIST.md. It does not replace the Infinix: Infinix items stay "pending:
  client checks it", and each milestone still gets the client's test checklist. Still no Wear OS watch
  (asked again on 2026-11-04).

## D-006 Repository layout (Phase 0, 2026-10-27)

`agap-app` is its own git repo with modules `:app` (phone) and `:wear` (watch), standard Gradle layout and the
Gradle wrapper, so it opens in Android Studio. Only app source, README and build/install docs go in it; the
contract, research documents and consent letters stay outside. Gradle memory is capped
(`org.gradle.jvmargs=-Xmx2g`, `org.gradle.workers.max=2`, added in Phase 1) because the build PC has a very
small page file.

## D-007 Build versions (Phase 1, 2026-10-27)

Checked against Google's Maven repository and the Android docs on 2026-10-27; newest stable at the time.

| Item | Version | Note |
|---|---|---|
| Android Gradle plugin | 9.4.1 | Needs Gradle 9.6.0+. Opening the project needs **Android Studio Quail 4 (2026.1.4) or newer**. |
| Gradle (wrapper) | 9.8.2 | `distributionSha256Sum` pinned in `gradle-wrapper.properties`. |
| compileSdk / targetSdk | 36 / 36 | API 37 exists; 36 kept (D-002). The Infinix runs Android 15 (API 35). |
| minSdk | 29 phone, 30 watch | Contract 1.1 (Android 10+); Wear OS 3 is API 30. |
| Java | 17 source and target | Contract 1.1. |
| androidx.appcompat | 1.8.0 | |
| com.google.android.material | 1.14.0 | Material 3 for Views. |
| junit | 4.13.2 | JVM unit tests. |

- Build scripts are Groovy (`build.gradle`) with a version catalog (`gradle/libs.versions.toml`), so the project
  holds no Kotlin at all.
- `android.builtInKotlin=false`: AGP 9 turns built-in Kotlin on by default; AGAP has no Kotlin sources. AGP
  prints a warning that this flag goes away in AGP 10 (KNOWN-ISSUES K-001).
- `local.properties` (SDK path) is machine-specific and not committed, as in any Android Studio project.

## D-008 Package name (Phase 1, 2026-10-27)

applicationId `ph.agap.app` for **both** the phone and the watch app (code namespaces `ph.agap.app` and
`ph.agap.wear`). The app is not published on the Play Store, so the name only has to be unique on the test
devices and identical in both apps. It must not change after the first install at the client, or the old
app and its Event History stay behind as a separate app.

## D-009 Screen structure (Phase 1, 2026-10-27)

- One `MainActivity` with bottom navigation (Home, Captions, Alerts, History), as in the mockups. Only the
  selected tab's screen exists (fragments are replaced, not hidden). Live Captions and Sound Alerts will be
  tabs, so switching tabs stops one before the other starts (contract 1.10).
- Settings, Sound Categories, Vibration Patterns, Demonstration Mode and About are separate activities with
  a Back arrow. Home also lists Settings as a labelled row, not only as the top-bar icon.
- Portrait is locked: the test phone stands on a stand, and every trial should look the same. Android 16+
  ignores the lock only on large screens. Lint's orientation warnings are switched off in `app/lint.xml`.

## D-010 Themes and text size (Phase 1, 2026-10-27)

- Two themes: light (`Theme.AGAP`) and high contrast (`Theme.AGAP.HighContrast`). Layouts use theme
  attributes (`?attr/agapText`, `?attr/agapSurface`...) that each theme maps to the palette names in
  `colors.xml`, so every later screen gets both looks without extra work. Category row attributes are already
  defined for Phases 3 and 6.
- High contrast has no thin purple: icons and selected text are white; purple only fills the Home icon
  circles, the selected tab and buttons, always behind white. Slider tracks are grey there.
- The bottom bar has no elevation, because Material 3 would tint it lavender.
- Text size: five steps (0.85, 1.0, 1.2, 1.45, 1.75) that **multiply** the phone's own font size, applied
  to the whole app through the font scale. Default step "Normal" (1.0). When the text size or display mode
  changes, every open screen rebuilds itself when it comes back into view.
- Edge-to-edge on every Android version (Android 15 forces it anyway); the top bar and screen edges are padded
  for the system bars in `BaseActivity`.

## D-011 Settings storage and defaults (Phase 1, 2026-10-27)

- SharedPreferences file `agap_settings`: `text_size_level` (0-4, default 1), `high_contrast` (default off),
  `vibration_on` (default on), `alert_threshold_percent` (10-90 in steps of 5, default **50**). Values are
  clamped when read. The mockup showed 60%; 50% is the starting point until Phase 3 measures real YAMNet scores
  for the test sounds, and it can be tuned under contract 1.12.
- No INTERNET permission. `allowBackup="false"` plus data-extraction rules that exclude everything from cloud
  backup and device transfer, so settings and the Event History stay on the phone (contract 1.2).

## D-012 Tests and test devices (Phase 1, 2026-10-27)

- Unit tests are plain JUnit 4 on the JVM. `SettingsStore` takes a `SharedPreferences`, and the test uses an
  in-memory version, so no Robolectric is needed yet (smaller download, less memory).
- With no phone available (D-005), screens are checked on an Android 15 emulator: virtual device `AGAP_API35`,
  Google Play x86_64 image (revision 9), 2 GB RAM, 1080x2400 at 420 dpi, accelerated by the Windows Hypervisor
  Platform. The emulator says nothing about the Infinix's speed or microphone; those checks stay with the client.

## D-013 English captions engine (Phase 2, 2026-11-04)

- Contract 1.3: English uses Google's on-device recognizer, or Vosk's English model if offline Google
  recognition is not available on the test phone.
- AGAP uses Android's own on-device recognizer (`SpeechRecognizer.createOnDeviceSpeechRecognizer`) when
  `isOnDeviceRecognitionAvailable` is true. On the Xiaomi 13 (HyperOS 3) it is **false**: the system registers
  no on-device recognizer. Other non-Pixel phones, the Infinix included, may be the same.
- Then AGAP binds to Google's speech service directly ("Speech Services by Google", `com.google.android.tts`,
  first; the Google app second) and sets `EXTRA_PREFER_OFFLINE`, which Android documents as "only use an
  offline speech recognition engine". On the Xiaomi this service is also the phone's default recognizer.
- `checkRecognitionSupport` says whether the English pack is installed. On the Xiaomi: en-US installed,
  30 languages offered offline (googletts 20260928.02). Filipino is not offered, so Filipino stays with Vosk
  (Phase 4). If the pack is missing, the screen offers a one-time download (`triggerModelDownload`, needs
  internet once, contract 1.2). Another installed English variant is used if en-US is not installed.
- Evidence: in airplane mode (Wi-Fi off, no default network, ping failed) English captions worked on the
  Xiaomi: 6 of 6 sentences read by a person, and the same results from the PC's synthetic voice as online.
- Both calls need Android 13 (API 33), so English captions need Android 13+ until D-014.
- Risk: the service is another app and could in principle use the network when the phone is online. AGAP
  itself has no INTERNET permission. The research protocol tests with Wi-Fi and data off, and the Phase 8
  offline proof does the same.
- The service answered one support check with an error and then two results; the check now answers once and
  lets a real result win over an early error.

## D-014 Vosk English fallback moved to Phase 4 (2026-11-04, owner's choice)

- The Phase 2 plan asked for a Vosk English fallback on phones without Google offline English (below API 33).
  Both known phones have Google offline English. Vosk is integrated in Phase 4 for Filipino, so the English
  model (about 40 MB) is added then with the same engine, instead of integrating Vosk twice.
- Until then such phones show a plain message: "Offline English captions need Android 13 or newer" or "This
  phone has no offline Google speech recognizer for English" (K-008).

## D-015 How captions run and are shown (Phase 2, 2026-11-04)

- Android's recognizer hears one sentence per session. The engine starts the next session at once after each
  result and after silence (NO_MATCH, SPEECH_TIMEOUT). Other errors: a new recognizer after 250 ms times the
  attempt number; 5 failed sessions in a row with no words stop captions with a message. A silence result in
  under 400 ms counts as a failure, so a broken recognizer cannot spin.
- `Transcript`: finished lines never change. The partial line is replaced by its final words, or kept as it is
  when a session ends without them, so a restart neither drops nor repeats words already shown.
- Partial words are lighter and underlined (not colour alone). Each sentence is its own paragraph with a
  half-line gap. Auto-scroll only when the view is already at the bottom, so reading back is not disturbed.
- Start clears the page. After Stop the text stays until Clear or the next Start, also across tab switches
  (activity-scoped ViewModel). Memory only; no FLAG_SECURE, so the research team can take screenshots.
- Listening stops when the screen leaves view (fragment onStop). `FLAG_KEEP_SCREEN_ON` is set only while
  listening.
- Logs (tag `AGAP/Captions`) hold event names, error codes, word counts and timings, never caption text.
- No formatting extra is set; the recognizer still writes numbers as digits (K-006).
- `CaptionRelay` passes the newest caption line to one listener; the watch link registers there in Phase 7.

## D-016 MediaPipe Audio Classifier and its usage statistics (Phase 3, 2026-11-04)

- Library: `com.google.mediapipe:tasks-audio` **1.2.2** (Google Maven, released 2026-11-03; it brings
  `tasks-core` 1.2.2, Guava, protobuf-javalite, flogger, Kotlin stdlib 1.9 and coroutines; Kotlin only as a
  library dependency, no Kotlin code in AGAP). Model: MediaPipe's `yamnet.tflite` (float32, 4,126,810 bytes,
  SHA-256 `4d8b4a53282dc83ef04e3e7dbc4fbc98082e34e44ed798e16c3a0cdd4c584faf`, from
  `storage.googleapis.com/mediapipe-models/audio_classifier/yamnet/float32/latest/`), bundled in assets and
  stored uncompressed. Its label list (521 classes) is packed inside the model file.
- `tasks-core` always creates a usage-statistics logger that sends through Google's `datatransport` library
  (checked in the bytecode: `TaskRunner` -> `TasksStatsLoggerFactory` -> `TasksStatsProtoLogger` ->
  `RemoteLoggingClient` -> `TransportRuntime`). Leaving `datatransport` out would crash the classifier.
- So AGAP keeps the library but removes the `INTERNET` and `ACCESS_NETWORK_STATE` permissions that it
  asks for (`tools:node="remove"` in the manifest). Checked with `aapt2 dump permissions`: the APK asks only
  for `RECORD_AUDIO` (and Android's own receiver-protection permission). Without network permission the
  statistics cannot leave the phone. They hold task names and timings, never audio.

## D-017 APK size: phone processors only, compressed native libraries (Phase 3, 2026-11-04)

- MediaPipe's native libraries are large (in the debug APK: 31 MB arm64-v8a, 22 MB armeabi-v7a, 38 MB
  x86_64). The first debug APK was 115 MB.
- `abiFilters` keeps arm64-v8a and armeabi-v7a (the Infinix's processor type is not known yet; one of the two
  covers every Android 10+ phone). x86_64 (the emulator) is left out; the Xiaomi replaces the emulator.
- `jniLibs.useLegacyPackaging = true` stores the native libraries compressed: the debug APK is about 52 MB.
  Android unpacks them once at install, so they take more space on the phone, which is fine at 128 GB.

## D-018 How Sound Alerts listens and decides (Phase 3, 2026-11-04)

- Audio: `AudioRecord`, source `VOICE_RECOGNITION` (the source Android tunes for machine listening, as
  MediaPipe's own helper uses), 16 kHz mono float, read in half-windows. YAMNet hears 0.975 s (15,600
  samples); a new window starts every 0.4875 s. Audio stays in memory only. Classifier mode `AUDIO_CLIPS`,
  `maxResults` 521, because the Annex A classes are rarely in the default top results (seen on the phone:
  without it, no Annex A score ever came back).
- Annex A is data: `assets/annex_a.tsv`, one line per YAMNet class, tab-separated because class names contain
  commas. `AnnexATest` reads the label list out of the bundled model and fails on any name that is not there.
- `AlertDetector`: per window, each alert scores the highest of its classes. At most one alert fires per
  window: the strongest at or above the user's threshold (default 50%, not changed). A sound that goes on
  fires once; the same alert can fire again after 3 s below the threshold, or after 30 s if it never stops.
  An alert that was also above the threshold but weaker (a siren heard as two alerts) does not fire while that
  sound goes on. `detectedAtMillis` is the phone's clock right after the window that crossed the threshold.
- Logs (tag `AGAP/Alerts`): each alert with its score and class, and per window the loudest YAMNet class and
  the three strongest Annex A alerts, for tuning (contract 1.12). No audio.

## D-019 One microphone user (Phase 3, 2026-11-04)

- `MicOwner` holds which feature listens. Live Captions and Sound Alerts take it before they start and give it
  back when they stop; taking it stops the other feature, and the screen says which one was stopped.
- In practice the tabs already make this so: only one tab's screen exists at a time, and each screen stops
  listening when it leaves the display. `MicOwner` is the guard for any later screen (such as the watch link)
  that might start listening on its own.

## D-020 Microphone source stays VOICE_RECOGNITION; debug-only override for tests (Phase 3, 2026-11-06)

- Knock and car-horn clips arrived as "Silence" in the detection check. To rule out the phone's own sound
  processing, the same clips ran with four `AudioRecord` sources on the Xiaomi 13. `UNPROCESSED` (raw) did no
  better than `VOICE_RECOGNITION` (1 against 2 correct alerts of 24; knock lost in both), so the processing is not
  the cause; the TV speakers used for playback are. `MIC` and `CAMCORDER` got 0 of 24, but music played in the room
  during those two runs. Numbers in DETECTION-CHECK.md.
- `VOICE_RECOGNITION` stays. Debug builds read an optional int `audio_source` from shared preferences
  `agap_debug` (written over adb with `run-as`), so a source can be tried on a phone without a new build; release
  builds ignore it. The log line `audio source N, unprocessed supported: ...` shows which source is in use.

## D-021 No threshold or class tuning in Phase 3 (2026-11-06)

- The detection check found low scores for knock and doorbell and YAMNet naming the smoke alarm "Fire alarm"
  (DETECTION-CHECK.md, K-010 to K-014). The default threshold (50%) and the Annex A classes are unchanged.
- Reason: the check used the TV's speakers and internet clips, not the group's sounds, phone or set-up. Tuning
  to these clips could make the group's results worse. Options are listed in DETECTION-CHECK.md and will be
  decided with the group's Infinix results (M1 feedback by 2026-11-11), under contract 1.12. Any change will be
  recorded here with the reason.

## D-022 Vosk models bundled in the APK, fetched by Gradle, not committed (Phase 4, 2026-11-07)

- `vosk-model-tl-ph-generic-0.6` (Alpha Cephei model list, medium wideband Filipino, CC BY-NC-SA 4.0) is a 329 MB
  zip (329,169,068 bytes) and 776 MB unpacked (`HCLG.fst` alone is 702 MB). Bundled in the APK as the contract
  says (1.1). The owner chose to bundle it (2026-11-07), over a side-loaded model file, after being given the
  sizes and the options.
- The English fallback model (D-014) is bundled the same way: `vosk-model-small-en-us-0.15`, 41 MB zip
  (41,205,931 bytes), 68 MB unpacked, Apache-2.0. With both models the debug APK is 431 MB (430,977,753 bytes;
  it was about 52 MB in Phase 3). It installed over USB in 16 s on the Xiaomi 13.
- The Filipino zip is over GitHub's 100 MB file limit, so neither model is committed. `fetchVoskModels` in
  `app/build.gradle` downloads them into `app/src/main/assets-vosk/` (git-ignored) before any asset-merge task and
  checks each SHA-256 (Filipino `0a2e067e...1259`, English `30f26242...2498`, computed on first download; Alpha
  Cephei publishes no checksums). The first build needs the internet once; the app never does.
- The zip is stored uncompressed in the APK (`noCompress "zip"`): it is already compressed, and the app reads its
  exact length to draw the progress bar.

## D-023 Unpack once, to internal storage, with a marker file (Phase 4)

- On first use of Filipino (or of the English fallback) the zip is streamed from the APK into `files/vosk/<version>.tmp`, then renamed to
  `files/vosk/<version>` once every file is written and a marker file holds the model version. A half-done unpack
  (phone full, app killed) is never taken for a model; the `.tmp` folder is deleted on the next try. Zip entries
  that would leave the folder are refused. Runs on its own thread; progress shows as a bar and a percentage;
  the screen stays on while it runs. Never on later launches. On the Xiaomi 13 the Filipino model unpacked in
  5.8-5.9 s and the English one in 0.7 s; the Infinix's storage is slower, so expect longer there.
- Filipino needs about 1 GB free (the model is 776 MB unpacked; English needs 300 MB): below that the screen says how much is needed and how
  much the phone has, and offers Check again. A write that fails with "no space left" is shown the same way.
- Setup is a button the user taps (not automatic on selecting Filipino), so a few minutes of heavy storage use
  never starts by surprise.

## D-024 Vosk engine: library 0.3.75, model kept in memory between sessions (Phase 4)

- `com.alphacephei:vosk-android:0.3.75` (latest on Maven Central, 2025-12-08; Apache-2.0) with JNA 5.18.1.
  Native libraries only for arm64-v8a and armeabi-v7a (the existing ABI filter, D-017).
- `VoskCaptionEngine` sits behind the Phase 2 `CaptionEngine` interface and feeds the microphone to Vosk itself
  (D-025). Vosk listens continuously: partial words as they form, a final result at each pause,
  so there are no restarts. Filipino and English remain two separate engines; the language is chosen before Start
  and the buttons are disabled during a session.
- With the Filipino model loaded the app uses about 980 MB (PSS 977-993 MB, 875 MB of it native) on the Xiaomi
  13; the English fallback about 270 MB. Loading took 0.70-0.86 s (Filipino) and 0.42-0.51 s (English) there. The
  model is kept between sessions and freed when the user picks English or the app leaves the screen
  (`onTrimMemory`). Only one model is kept; `VoskModelHolder` hands out an unkept model if the other is still in
  use, so a session always gets the language it asked for (a race found in review: a slow Filipino load could
  hand its model to an English start).
- Vosk's native log level is set to warnings so no recognised text can reach logcat (contract 1.2).
- The Vosk English fallback (D-014) is built: English uses it on Android below 13, with no Google speech service,
  or when Google does not support English offline (not when the pack only needs downloading). The screen says so.

## D-025 AGAP's own capture loop instead of Vosk's `SpeechService` (Phase 4)

- `VoskCaptionEngine` runs one worker thread per session: it loads the model, opens `AudioRecord`
  (`VOICE_RECOGNITION`, 16 kHz, 16-bit, 2 s buffer), feeds Vosk 0.1 s at a time, and frees the microphone,
  recognizer and model when it ends. Each session checks its own number, so a quick Stop and Start cannot leave an
  old worker running.
- Why not `SpeechService`: it throws an uncaught exception on a microphone read error (an app crash; here the
  session ends with the usual message), posts every partial result to the screen 5 times a second (here only
  changes), and gives `AudioRecord` only 0.2 s of buffer (here 2 s, room for a slower phone while Vosk finishes a
  sentence). Recognition is the same: the same 116 s file of 12 Filipino sentences gave 16.5% raw word error with
  `SpeechService` and 14.8-18.7% with this loop.
- How this came up, for the record: from 02:41 to 02:51 (phone clock) every Filipino run faded after one or two
  sentences, down to no words at all, with the microphone open and the processor idle. Per-sentence resets of
  Vosk's speaker adaptation seemed to fix it, but a check with the old behaviour, and then with `SpeechService`
  itself, showed every variant working from 02:55 on. Between those runs the owner had moved or adjusted the
  test setup (speaker and phone). The reset was dropped (it scored worse: 26.4% against 16.5%). Cause of the failures: the test
  setup, not the app.

## D-026 Words without a letter or digit are dropped from Vosk results (Phase 4)

- The Filipino model's vocabulary holds 44 punctuation-only "words" ("." "--" "'" and others, from its training
  text). Vosk returned "." once in testing. `VoskJson` drops any word with no letter or digit, so the group's word
  error counts are not charged for them. Words like "pang-polo", "ika-3" and "'yan" are kept. 2 unit tests.

## D-027 Two more debug-only switches (Phase 4)

- In preferences `agap_debug` (set over adb; release builds ignore both): boolean `force_vosk_english` makes
  English use the Vosk fallback on a phone that has Google offline English; long `fake_free_mb` stands in for the
  free storage so the low-storage message can be checked on a phone with plenty of room.

## D-028 The alert screen is a full-screen dialog over the app, not a new screen (Phase 5, 2026-11-07)

- A new activity would stop the Alerts tab, and leaving the tab stops listening (contract 1.10, D-009), so
  detection would pause after every alert during the group's trials. `AlertOverlayFragment` is a full-screen
  `DialogFragment`: Sound Alerts keeps listening behind it, and a newer alert replaces the one shown. Every alert
  is still recorded with its own shown time; the screen says how many came while it was open.
- Same strong category colours in both display modes (design note); no window animation, no flashing; only the
  badge pulses (scale 1.0-1.07, once a second). Dismiss button 72dp high; the middle scrolls at large text sizes.
- An alert dialog restored by Android after the app was rebuilt is closed at once, so an old alert is never
  recorded twice.

## D-029 "Alert shown" is the time the alert's frame reaches the display (Phase 5)

- `shownAtMillis` is taken in a frame-commit callback (`ViewTreeObserver.registerFrameCommitCallback`, Android 10
  and newer, so every supported phone) registered just before the first draw of the alert. If no callback comes
  within 1 s, the draw time is used and the log says "fallback". Both times are `System.currentTimeMillis()`.
- Measured on the Xiaomi 13: 55-130 ms from detection to shown when the alert screen opens (10 alerts; 130 ms was
  the first after the app started), 17-22 ms when a new alert replaces one on screen (5 alerts). The detection
  time itself is taken after YAMNet has scored the window (D-018), so the classifier's own 5-30 ms are not in it.

## D-030 Event History: Room 2.8.5, one table, written on one background thread (Phase 5)

- `androidx.room` 2.8.5 (latest stable on Google Maven, 2026-11-07) with its Java annotation processor;
  RecyclerView 1.4.0 for the list. The schema is exported to `app/schemas/` (in git) for later migrations.
- Table `alert_events`: id, label, category, confidence, detectedAtMillis, shownAtMillis (contract 1.6), plus
  `yamnetClass` and `thresholdPercent`, so the group and the tuning in Phase 10 can see what YAMNet heard and
  which threshold was set. Newest first by detection time, then by id.
- `HistoryRepository` reads and writes on one thread, so writes keep their order and the screen never waits.
  `AlertRecorder` (plain Java, unit-tested) saves every real alert, false ones included, and never a demo alert.
- Clear history asks first and states the number of alerts. No export (brief): see SCOPE-FLAGS.

## D-031 Vibration patterns and how they play (Phase 5)

- From the design note's proposal, as data in `VibrationPatterns` (also for the guide in Phase 6 and the watch in
  Phase 7): Emergency 600 ms on x3 with 250 ms gaps, then 900 ms off, repeating until dismissed; Traffic 150-120-150
  ms pairs with 600 ms pauses, three times; Household/Entry 500 ms then 150 ms, 700 ms pause, twice.
- Played as an alarm (`USAGE_ALARM`), so the phone vibrates in silent mode too: for a deaf user the vibration is the
  alert. The Emergency repeat stops after 60 s if nobody dismisses it. Off when Vibration is off in Settings; the
  alert screen then says "Vibration: Off". Needs the `VIBRATE` permission (normal, no prompt).
- Not yet tuned by feel on the Infinix; the group's feedback can change the timings (contract 1.12).

## D-032 Demonstration Mode lives in memory only (Phase 5)

- On from the Demonstration screen, off with Exit, and always off after the app is closed or the phone restarts, so
  a test session can never start in Demonstration Mode by accident. A striped banner is on every screen and on the
  alert screen while it is on.
- The microphone is never opened: Live Captions plays a fixed English or Filipino sample script word by word
  (`DemoCaptionEngine`, same screen path as real captions), and Sound Alerts cannot start. Sample alerts (13 chips,
  confidence 92%) use the same alert screen and vibration, are logged as "not saved", and never reach the history.

## D-033 Icons (Phase 5)

- 13 alert icons and 3 category icons from Google's Material Symbols (filled, Apache 2.0, the family already credited
  in About), converted to vector drawables: local_fire_department, detector_smoke, e911_emergency, campaign,
  taxi_alert, local_shipping, directions_car, doorbell, door_front, ring_volume, alarm, timer, wine_bar;
  categories priority_high, directions_car, notifications. The classic Material Icons set has no smoke detector or
  doorbell. Badge shapes (octagon, diamond, circle) are drawn in the app.

## D-034 Instrumented tests run with `am instrument`, not `connectedDebugAndroidTest` (Phase 5)

- The connected-test task uninstalls the app afterwards, which deletes the unpacked speech models and the history.
  Instead: install the app and `app-debug-androidTest.apk`, then
  `adb shell am instrument -w ph.agap.app.test/androidx.test.runner.AndroidJUnitRunner`. The DAO test uses an
  in-memory database, so the phone's real history is not touched. On the Xiaomi the test APK's first install needs
  the "Install via USB" prompt accepted on the phone.

## D-035 Event History export: a CSV file in Download/AGAP (Phase 5 addendum, 2026-11-07)

- Owner-approved scope flag (SCOPE-FLAGS, 2026-11-07). The History tab's export icon writes the whole history to
  `Download/AGAP/AGAP-history-<date>-<time>.csv` through Android's MediaStore Downloads collection: no internet, no
  permission on Android 10 and newer. A dialog says where the file is and offers Share (Android's share sheet, the
  user's choice of app); nothing is sent by AGAP itself.
- Format (`HistoryCsv`, unit-tested): RFC 4180 with CRLF line ends, UTF-8 with a byte-order mark for Excel; oldest
  first so rows follow the trials; columns No., Date, Alert, Category, Confidence (%) to one decimal, Heard as
  (YAMNet), Threshold (%), Sound detected and Alert shown (HH:mm:ss.SSS, phone's time zone), Response time (ms),
  and both times as epoch milliseconds for exact calculations.

## D-036 Launcher icon from the group's logo (Phase 6, 2026-11-07)

- The group's logo (`design/agap-logo.jpg`, outside this repo: "AGAP" in purple bubble letters with a white
  outline, the G drawn as an ear, on black) is now the launcher icon of the phone and the watch app (D-004).
- Adaptive icon with a transparent background (owner's choice): the wordmark is cut out of its black background
  (black connected to the edge of the image becomes transparent; the outline's soft edge gets partial alpha in the
  outline colour, so there is no dark fringe), centred at 58% of the 108dp layer, and saved as PNGs for the five
  screen densities. A monochrome layer (the purple letters only) serves Android 13+ themed icons.
- Xiaomi's HyperOS draws its own dark plate behind icons with a transparent background (seen in App info); stock
  Android launchers show the wallpaper (K-020).
- Inside the app (owner's request, 0.6.1): the logo replaces the word "AGAP" in Home's top bar (36dp high; its white
  outline keeps it visible on the purple bar and on black in high contrast; read as "AGAP" by screen readers), and
  sits on its own black at the top of About. One cutout PNG (`drawable-xxxhdpi/agap_logo.png`, 64dp high) serves
  both.

## D-037 Sound Categories and Vibration Patterns are built from data (Phase 6)

- Sound Categories reads the app's Annex A file, so it always lists exactly what Sound Alerts listens for: each
  category (badge, name, meaning from the design note) and each alert (icon, one plain sentence, and the YAMNet
  classes it listens for).
- The Vibration Patterns guide reads `VibrationPatterns`, the same data the alerts use. Try plays one round of the
  pattern even when Vibration is off for alerts (the user asked to feel it); Emergency does not repeat there.
  Cards are stacked (name, words, bars, Try) so words keep the full width at the largest text size.

## D-038 Home status card (Phase 6)

- "Ready to work offline": no internet access; microphone (always off on Home: AGAP listens only on the Captions
  and Alerts tabs, contract 1.10; or not allowed yet); Sound Alerts ready; English (Google offline ready, pack
  needed, or AGAP's own English model ready / needs setup); Filipino (ready or needs setup); Demonstration Mode
  when on. Each line has a check or info icon and says its state in words.
- The English check is shared with Live Captions (cached for the session) and runs only with microphone
  permission, as on the Captions tab.

## D-039 The alert threshold applies at each Start (Phase 6)

- Opening Settings takes Sound Alerts off the screen, which stops listening (contract 1.10), so a changed
  threshold always applies from the next Start, with no app restart. The Alerts screen now shows the threshold that
  will be used ("Alert threshold: 50% (Settings)"), and each saved alert keeps the threshold it was raised at.

## D-040 Accessibility pass (Phase 6)

- Google's Accessibility Test Framework runs on the whole screen at every Espresso action (touch targets of 48dp,
  labels, contrast, duplicate or redundant descriptions). Fixed from its findings: the Settings sliders and the
  vibration switch no longer repeat their visible titles (the vibration row is read once, as a switch with its
  state); display-mode cards no longer say "selected" twice. Accepted, as they are not confusing: the Settings icon
  and the Settings row (same destination), "Demonstration Mode" as a Home row and a screen title, two history rows
  with the same alert name.
- Largest text size checked by screenshots on every main screen. Fixed: the Vibration Patterns guide broke words
  ("Emergen-cy"), so its cards are now stacked; on the alert screen, above 1.3 times the normal text size the
  label stays at 1.15 times (51dp) and the badge shrinks to 100dp, so name, confidence, detection time and Dismiss
  fit on screen, even with the Demonstration Mode banner. TalkBack itself was not used (brief: tests with TalkBack off).

## D-041 Espresso smoke tests (Phase 6)

- Espresso 3.7.0 with espresso-accessibility 3.7.0. `NavigationTest` (tabs, Home rows, Demonstration Mode banner
  and microphone lock) and `SettingsTest` (threshold saved and shown on Alerts, vibration row, high contrast
  rebuild, largest text size). Settings tests save and restore the phone's own settings.
- Run with `am instrument` (D-034) with the phone's animation scales set to 0 for the run and restored afterwards
  (badge and status-dot pulses otherwise keep the UI busy, K-019). Home's status card can change height while a
  test scrolls (the English check answers from Google's speech service, which Espresso cannot see), so a scroll to a
  Home row is retried once.
