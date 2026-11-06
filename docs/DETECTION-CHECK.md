# Sound Alerts detection check (internal)

An internal check of what the Sound Alerts engine raises for the client's priority sounds and for sounds that
should raise nothing. It is **not** the client's formal test and it does **not** measure accuracy: the clips,
speaker, room and phone differ from the client's set-up. The numbers are reported as observed. Phase 8 repeats
the check with the final build and adds its results below the Phase 3 numbers.

## Phase 3 (build 0.3.0, 2026-11-06)

### Set-up

- **Phone:** the development Xiaomi 13 (Android 16), app 0.3.0 debug, Sound Alerts listening, threshold 50%
  (the default, not changed), **airplane mode on** (`airplane_mode_on=1`, ping failed).
- **Speaker:** the PC's speakers at one fixed volume, the phone lying next to them in the same place for every
  trial (distance not measured).
- **Clips:** free sound files from Wikimedia Commons (public domain, CC0 or CC BY-SA), converted to 44.1 kHz
  mono WAV and cut to 2.5-6 s. Three levels per clip: high, medium (-10 dB) and low (-20 dB). The beeps clip is
  a plain 1 kHz beep (0.12 s on in every 0.4 s) made for this check, as a "confusing" sound. The silence clip is 4 s of digital
  silence.

| Clip | Expected alert | Source | Licence |
|---|---|---|---|
| fire-alarm | Fire Alarm | NFPA Fire Alarm.ogg: three 0.5 s beeps of about 500 Hz, then a pause (the "temporal-3" evacuation pattern) | Public domain |
| smoke-alarm | Smoke Alarm | Smoke alarm.ogg: a high chirping tone of about 2.6 kHz | Public domain |
| car-horn | Vehicle Horn | Car Horn.wav | CC0 |
| doorbell | Doorbell | Doorbell-classic-dingdong.ogg | Public domain |
| doorbell2 | Doorbell | Doorbell-cheap-dingdong.ogg | Public domain |
| knock | Knocking | Knock on door.wav | CC0 |
| speech | none | 7 s of spoken English at 22 kHz, most likely a Windows computer voice (source not recorded) | - |
| music | none | Example of double tracking in a pop-rock song (3 guitar tracks).ogg | CC BY-SA 4.0 |
| beeps | none | made for this check | - |
| silence | none | made for this check | - |

- **Procedure:** a script plays each clip once, waits 1.5 s for the last audio window, then reads the alerts
  the app logged (tag `AGAP/Alerts`: alert name, score and YAMNet class, never audio). Then a 2 s pause so the
  detector's 3 s quiet period passes. 5 repetitions per clip and level.
- **Counting:** for a clip with an expected alert, **TP** if that alert fired at least once during the clip,
  else **FN**; any other alert during the clip is counted as a **wrong alert** (an FP). For a clip with no
  expected alert, **TN** if nothing fired, else **FP**. "Highest score" is the expected alert's highest score in
  any window of the clip, whether or not it reached 50%.

### Results on the phone: full check (140 trials, microphone source `VOICE_RECOGNITION`)

Run 2026-11-06 17:22-17:44, build 0.3.0. "Highest score" is the expected alert's highest score in any window of
the trial (range over the 5 trials). "Strongest Annex A alert" is the alert that scored highest in most trials.

| Clip | Expected alert | Level | TP | FN | Wrong alerts (count) | Highest score of the expected alert | Strongest Annex A alert |
|---|---|---|---|---|---|---|---|
| fire-alarm | Fire Alarm | high | 0 | 5 | Alarm Clock (6), Appliance Timer (5) | 0.00 | Appliance Timer 0.67-0.80 |
| fire-alarm | Fire Alarm | medium | 0 | 5 | Alarm Clock (6), Appliance Timer (4), Glass Breaking (2) | 0.00 | Appliance Timer 0.74-0.85 |
| fire-alarm | Fire Alarm | low | 0 | 5 | Appliance Timer (5), Alarm Clock (3) | 0.00-0.02 | Appliance Timer 0.74-0.80 |
| smoke-alarm | Smoke Alarm | high | 0 | 5 | Fire Alarm (5), Warning Siren (2) | 0.20-0.33 | Fire Alarm 0.89-0.97 |
| smoke-alarm | Smoke Alarm | medium | 0 | 5 | Fire Alarm (5), Emergency Vehicle Siren (3), Warning Siren (3) | 0.00-0.26 | Fire Alarm 0.89-0.94 |
| smoke-alarm | Smoke Alarm | low | 0 | 5 | Fire Alarm (4), Warning Siren (2) | 0.02-0.33 | Fire Alarm 0.11-0.97 |
| car-horn | Vehicle Horn | high | 2 | 3 | none | 0.20-0.74 | Vehicle Horn 0.20-0.74 |
| car-horn | Vehicle Horn | medium | 1 | 4 | none | 0.15-0.50 | Vehicle Horn 0.33-0.50 |
| car-horn | Vehicle Horn | low | 0 | 5 | Appliance Timer (4) | 0.06-0.26 | Appliance Timer 0.33-0.59 |
| doorbell | Doorbell | high | 0 | 5 | Phone Ringing (4) | 0.02-0.15 | Phone Ringing 0.26-0.59 |
| doorbell | Doorbell | medium | 0 | 5 | none | 0.08-0.26 | Doorbell 0.08-0.26 |
| doorbell | Doorbell | low | 0 | 5 | Phone Ringing (2) | 0.04-0.26 | Phone Ringing 0.04-0.50 |
| doorbell2 | Doorbell | high | 0 | 5 | Glass Breaking (3) | 0.15-0.41 | Glass Breaking 0.33-0.59 |
| doorbell2 | Doorbell | medium | 0 | 5 | Phone Ringing (1) | 0.08-0.20 | Glass Breaking 0.15-0.33 |
| doorbell2 | Doorbell | low | 0 | 5 | Glass Breaking (3) | 0.04-0.20 | Glass Breaking 0.26-0.67 |
| knock | Knocking | high | 0 | 5 | none | 0.00 | Glass Breaking 0.01-0.02 |
| knock | Knocking | medium | 0 | 5 | none | 0.00 | Glass Breaking 0.01-0.02 |
| knock | Knocking | low | 0 | 5 | none | 0.00-0.01 | Glass Breaking 0.01-0.02 |

| Clip (no alert expected) | Level | TN | FP | Wrong alerts (count) |
|---|---|---|---|---|
| speech | high / medium / low | 5 / 5 / 5 | 0 / 0 / 0 | none |
| music | high / medium / low | 5 / 5 / 5 | 0 / 0 / 0 | none |
| beeps | high / medium / low | 0 / 0 / 0 | 5 / 5 / 5 | Phone Ringing (16), Appliance Timer (9) |
| silence | - | 5 | 0 | none |

An alert shows the score of the window that crossed the threshold, so its confidence can be lower than the
peak: the smoke alarm's 14 Fire Alarm alerts showed 0.50-0.89 while the peak was 0.89-0.97. The fire-alarm clip
raised 1-4 alerts per trial (two or more in 12 of 15).

Totals: 90 trials with an Annex A sound: 3 TP, 87 FN; 44 of the 90 also raised a wrong alert. 50 trials
without one: 35 TN, 15 FP (all from the beeps clip). In 7 trials YAMNet's loudest class was Speech (0.67-0.99)
during a clip that has no speech in it (fire-alarm, smoke-alarm, doorbell): people were talking in the room during
part of the run.

YAMNet's loudest class (of all 521) during the clips: knock, doorbell2, and car-horn at medium and low were
mostly **Silence** (0.59-0.80); fire-alarm was "Beep, bleep" or "Alarm clock"; smoke-alarm was "Alarm" or
"Fire alarm"; doorbell was "Chime", "Music" or "Speech"; beeps were "Ringtone", "Telephone dialing, DTMF" or
"Beep, bleep"; speech, music and silence were recognised as such (0.97-1.00).

### The same clips without speaker and microphone

To separate what YAMNet makes of a clip from what the speaker and microphone lose, the same WAV files went
straight into the same model (`yamnet.tflite` from the app) through MediaPipe 1.2.2 for Python on the PC,
with the app's windows (0.975 s every 0.4875 s). One pass per file; the model gives the same answer every time.

| Clip | Levels where the expected alert reached 50% | Highest score of the expected alert (high / medium / low) | Strongest class instead |
|---|---|---|---|
| fire-alarm | none | below 0.03 at every level (Fire alarm) | "Busy signal" 0.59-0.80, "Telephone" up to 0.67 (Phone Ringing) |
| smoke-alarm | none | 0.00 / 0.00 / 0.00 (Smoke detector) | "Alarm" 0.94-1.00, **"Fire alarm" 0.98 / 0.94 / 0.67** |
| car-horn | high, medium, low | 0.98 / 0.97 / 0.80 | - |
| doorbell | none | 0.20 / 0.33 / 0.15 (Ding-dong) | "Music" 0.67-0.89, "Bell" 0.41-0.50 |
| doorbell2 | high, medium | 0.50 / 0.50 / 0.26 | "Music" 0.33-0.50 |
| knock | high, medium, low | 0.50 / 0.59 / 0.85 | "Walk, footsteps" 0.50-0.59 |
| speech, music, silence | - | - | Speech 1.00, Music 0.97-1.00, Silence 0.80; no Annex A alert |
| beeps | - | - | "Telephone" 0.94-0.96 (Phone Ringing would fire) |

### Microphone source comparison

To see whether the phone's own sound processing hides knocks and horns, a debug-only switch (D-020) changed the
`AudioRecord` source, and a shorter check ran for each: knock, car-horn, doorbell2, smoke-alarm and music, at high
and medium, 3 repetitions each (30 trials per source), same speaker and position, airplane mode.

| Source | TP of 24 | Knocking, highest score | Vehicle Horn, highest score | YAMNet's loudest class during knock / car-horn |
|---|---|---|---|---|
| `VOICE_RECOGNITION` (the app's choice) | 2 (car horn) | 0.00 | 0.85 | Silence 0.59-0.74 / horn in 2 trials |
| `UNPROCESSED` (raw, the phone says it supports it) | 1 (doorbell2) | 0.01 | 0.11 | Silence 0.59-0.92 / Silence 0.59-0.85 |
| `MIC` | 0 | 0.00 | 0.26 | Music 0.74-0.96 (*) |
| `CAMCORDER` | 0 | 0.00 | 0.01 | Music / Speech 0.67-0.96 (*) |

(*) Music started playing in the room during the last two runs, so they do not compare fairly with the first two.

With the raw source the knock was just as lost and the horn weaker, so the phone's processing is not what hides
them. `VOICE_RECOGNITION` stays (D-018, D-020).

### A real knock

At 18:16-18:17, with the app listening (threshold 50%, airplane mode), the owner knocked firmly on a wooden door or
table, asked for five knocks about 1 m from the phone (count and distance not measured). YAMNet also heard
speech, whistling, a rooster and a dog in that period.

- YAMNet heard the knocks as "Bouncing" (0.26-0.74), "Wood block" (0.15-0.20) and "Chopping (food)" (0.41).
- **Knocking** reached 0.33 in one window and 0.08-0.20 in four others. **No Knocking alert** at 50%.
- During the 13 minutes of listening, two **Phone Ringing** alerts fired (0.50 and 0.59, class "Telephone",
  one window each). Their cause is not known.

### What Phase 3 shows (observations, not accuracy)

1. **The engine works end to end in airplane mode.** An alert fires when one of its YAMNet classes reaches the
   threshold, and the screen shows that score. Speech, music and silence raised nothing (59 of 59 trials across
   all runs).
2. **The speaker set-up limits the check.** Knock and car horn, which YAMNet recognises well from the file (0.50-0.98),
   mostly arrived as Silence through the TV's speakers. A real knock was heard but named "Bouncing".
3. **Some sounds are named by YAMNet differently from Annex A** (K-010, K-011): the smoke alarm is "Fire alarm";
   the three-beep fire alarm is a beep or a telephone; the classic ding-dong is music. No threshold fixes that.
4. **Knock and doorbell are the weakest of the group's priority sounds.** Even on clean files they score 0.15-0.50
   (doorbell) and 0.50-0.85 (knock): close to or below the 50% default.

No threshold or class was changed in Phase 3. Tuning options for contract 1.12, to decide with the group's
results from the Infinix: a lower threshold (Settings allows 10-90%; 30% would have caught one real-knock window
and some doorbell2 trials, and would also let more false alerts through), extra YAMNet classes for Knocking
(such as "Wood block"), or a short hold after any alert (K-012).

### Not tried in Phase 3

- The Infinix Hot 70, and any phone other than the Xiaomi 13.
- A real fire alarm, smoke alarm, doorbell, siren or car horn; a second phone or a proper speaker as the sound source.
- Distances other than "next to the TV speakers" and about 1 m (real knock); the group's controlled volumes.
- Emergency Vehicle Siren, Warning Siren, Reversing Alarm, Vehicle Nearby, Alarm Clock, Glass Breaking on purpose
  (some fired as wrong alerts, see above).
- Response time (alert shown minus detected): the full alert screen and Event History come in Phase 5.
