# What AGAP 1.1 can and cannot do

A short, plain note for the research group, to read before the beta test and the formal trials. It follows the
contract; the section numbers refer to it. Version 1.1 is 1.0 plus **background listening**, which the contract did
not include (1.10, section 3): it was added at the group's request, free of charge, and is **off until you turn it
on**. With it off, 1.1 works like 1.0.

## What it does

- **Live Captions** in English or Filipino, one language at a time, with no internet (1.3). The text stays on screen
  after Stop for comparing with the script; it is never saved.
- **Sound Alerts** for the 13 sounds of Annex A in three groups, using Google's YAMNet sound model with its built-in
  classes (1.4): a full-screen alert with icon, name, group, confidence and time, and one vibration pattern per group
  (1.5).
- **Event History** of every real alert, wrong ones included, with the time the sound was detected and the time the
  alert was shown, to the millisecond, for response times (1.6). Export to a CSV file.
- **Settings** for text size, high contrast, vibration and the alert threshold (1.7); Home, Sound Categories and
  Vibration Patterns screens (1.8); **Demonstration Mode** with sample captions and alerts, no microphone, nothing
  saved (1.9).
- **A watch app** for Wear OS 3 or newer that shows the alerts with their vibration and the live captions over
  Bluetooth (1.11).
- **No internet, no accounts, no cloud:** the apps have no internet permission, and no sound is ever recorded or
  saved (1.2).
- **Background listening (new in 1.1, Settings, off by default):** Sound Alerts or Live Captions go on after you leave
  AGAP or the screen turns off, until you tap Stop. Alerts appear over the lock screen, as a popup card over other
  apps or as a notification, with their vibration and on the watch, and are saved with how they were shown. Captions
  float over other apps and are all on the Captions screen when you return. A home-screen widget shows whether AGAP
  is listening and the newest alert.

## What it does not promise

- **No accuracy is guaranteed** (6.5). The 95% passing score for Overall Detection Performance is the group's
  target in the Methods, not a promise of the app. My own checks were not the formal test and gave no accuracy figure.
- **Similar sounds get confused.** The sound model names sounds its own way:
  - The smoke alarm we tried was named **Fire Alarm** (same group and vibration, different name).
  - The three-beep fire alarm pattern was heard as an alarm clock or beeps.
  - Plain beeps were heard as a telephone, so they raise Phone Ringing.
  - **Appliance Timer** fires for many other beeps.
  - One sound can raise two alerts in a row (for example Fire Alarm, then Warning Siren).
- **Knocking and Doorbell scored low** in all my checks and may be missed at the 50% threshold. A lower threshold
  catches more of them and also raises more false alerts.
- **Vehicle Nearby** cannot tell a vehicle coming closer from one moving away.
- **How a sound is played matters.** Through speakers, with talking in the room, the same clips scored far lower than
  from the file. Results depend on the speaker, volume, distance and room.
- **Filipino captions are usually less accurate than English**, especially for Taglish, names and English words
  inside Filipino. English writes numbers as digits ("page 12"), which matters when counting word errors.
- **English captions depend on Google's speech service on the phone**, which Google can update. Note its version
  before the formal tests (VERSIONS.md).

## Limits of how it works

- With background listening **off**, it listens only with AGAP open on screen, with the screen kept on (1.10); leaving
  the app stops listening. With it **on**, see the next section. An answered phone call silences the microphone in
  both cases ("AGAP hears nothing right now").
- **One feature at a time:** Sound Alerts pauses while Live Captions runs.
- **A call during a trial** rings as a Phone Ringing alert, which is saved. Run trials in airplane mode.
- **An alert stays on screen until Dismiss.** Dismiss after each trial.
- **Storage:** about 1.5 GB in all once Filipino is set up (the app is 426 MB).
- **The watch needs the phone** nearby with AGAP open, and AGAP open on the watch too. With AGAP closed on the
  watch, alerts arrive only as a notification.
- It is a **research prototype and a supplementary tool**, not a medical device and not the only way to detect an
  emergency (6.7).

## Limits of background listening (1.1)

- **It must be started in AGAP.** Android does not let an app start the microphone from the background, from a
  widget or after a restart. AGAP never starts listening by itself.
- **The phone can still stop it.** Android, or a phone maker's battery saver, can close AGAP in the background; then
  the "AGAP is listening" notification disappears and nothing is heard until AGAP is started again. Allow "No
  battery limits" and the phone maker's auto-start (install guide, step 10).
- **Background response times are measured differently.** An alert shown only as a notification is timed when AGAP
  hands it to Android (Android does not say when it appears); a lock-screen alert includes waking the screen. Compare
  response times only between alerts with the same "Shown as". For the formal trials, keep background listening off.
- **A phone in a pocket or bag hears less**, so quiet or distant sounds are missed more often. Not measured.
- **It uses battery the whole time it runs**, and the microphone indicator stays on. On my phone, Android estimated
  Filipino captions at about 91 mAh an hour (about 2% of its battery); off the charger on the Infinix is not measured.
- **Your study:** the Methods place background operation outside the present study. Ask your adviser before using
  it in data collection.

## Not checked yet (by me)

- Anything on the **Infinix Hot 70**: all my checks ran on a Xiaomi 13 (Android 16). Your alpha and beta tests are the
  first runs on your phone.
- The **watch with a real phone**: the watch app was checked on a simulated watch only, and the Bluetooth link
  between phone and watch has never run end to end.
- The "AGAP hears nothing right now" message, which appears when Android gives the microphone to something else.
- Battery use during long sessions off the charger.
- **Background listening on the Infinix** (XOS battery settings, popups, lock-screen alerts, the widget). All my
  background checks ran on the Xiaomi 13 (HyperOS, Android 16).

Full details: KNOWN-ISSUES.md, DETECTION-CHECK.md and SPEC-CHECKLIST.md.
