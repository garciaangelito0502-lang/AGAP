# AGAP 1.0 user guide

AGAP helps a deaf or hard-of-hearing person with two things, with no internet: **Live Captions** turns speech into
text on the screen, and **Sound Alerts** shows and vibrates an alert when it hears one of 13 important sounds. A
Wear OS watch can show the same alerts and captions. AGAP is a research prototype and a supplementary tool: it is not
a medical device and not the only way to notice an emergency.

AGAP listens **only while its Captions or Alerts screen is open**, and keeps the screen on while it listens. It
never records or saves sound, and only one of the two features listens at a time. Installing: [INSTALL-GUIDE.md](INSTALL-GUIDE.md).

## The bottom bar

**Home**, **Captions**, **Alerts** and **History**. The icon at the top right opens **Settings**.

## Home

- **Live Captions** and **Sound Alerts**: open each feature.
- **Ready to work offline**: one line per thing AGAP needs, with a check mark when it is ready, or an "i" with what
  to do: internet (never needed), the microphone, Sound Alerts, English captions, Filipino captions, and whether a
  watch is connected.
- **Sound Categories**, **Vibration Patterns**, **Demonstration Mode** and **Settings**.

## Live Captions

1. Choose **English** or **Filipino** (one at a time; the check mark shows which). The language cannot change while
   listening.
2. Tap **Start** and speak. Words that may still change are shown lighter and underlined; finished sentences become
   normal text, each in its own paragraph. The text follows the newest words; scroll up to read back.
3. Tap **Stop**. The text stays on screen so you can read it, copy it by hand or take a screenshot. **Clear** empties
   it; the next **Start** also begins with an empty page. The text is never saved.

The status line says "Listening · English" (or Filipino). Sound Alerts is paused while captions run. A message card
explains anything that blocks Start: the microphone permission, the English speech pack, Filipino set-up, or low
storage. If Android gives the microphone to a phone call or another app, the card says "AGAP hears nothing right
now"; listening goes on by itself when the microphone is free.

English uses Google's offline speech recognition on the phone; Filipino uses AGAP's own built-in Filipino model.
Numbers come out as digits in English ("page 12"). Names and English words inside Filipino are often wrong.

## Sound Alerts

1. Tap **Start**. The status changes to "Listening for 13 sounds" with a slowly pulsing dot.
2. When AGAP hears one of the 13 sounds at or above the **alert threshold** (Settings, 50% to start with), the alert
   screen opens over everything and the phone vibrates in the sound group's pattern. Listening goes on behind it.
3. **Last alert** shows the newest alert; "Alerts since Start" counts them. The **Testing info** line shows the sound
   AGAP is closest to right now and the loudest sound it hears, for checking test sounds.
4. Tap **Stop**, or leave the screen: listening stops.

The 13 sounds, in three groups: **Emergency** (Fire Alarm, Smoke Alarm, Emergency Vehicle Siren, Warning Siren),
**Traffic** (Vehicle Horn, Reversing Alarm, Vehicle Nearby) and **Household/Entry** (Doorbell, Knocking, Phone
Ringing, Alarm Clock, Appliance Timer, Glass Breaking).

## The alert screen

The whole screen takes the group's colour: red for Emergency (octagon), orange for Traffic (diamond), blue for
Household/Entry (circle). It shows the group, the sound's icon, its name, **Confidence** (how sure AGAP is), the
time it was **Detected**, and the vibration rhythm. "Saved to Event History" confirms it was saved. A newer alert
replaces it, and the screen counts how many came while it was open. Tap **Dismiss** to close it and stop the
vibration (Emergency repeats until you do, at most one minute). In trials, Dismiss after every alert.

## Event History

Every real alert is saved, including wrong ones, newest first and grouped by day: name, group, confidence and time.
Tap an alert for its **details**: date, confidence, the exact time the sound was detected and the alert was shown (to
the millisecond, from the phone's clock), the **response time** (shown minus detected), what YAMNet heard, and the
threshold in use. The top bar has **Export history** (download icon), which saves a CSV file in the phone's
Download/AGAP folder and offers to share it, and **Clear history** (bin icon), which asks before deleting.
Demonstration Mode alerts are never saved.

## Sound Categories

The three groups and all 13 sounds, each with its icon, what it means and which of the sound model's classes AGAP
listens for.

## Vibration Patterns

One pattern per group, drawn as a rhythm. Tap **Try** to feel it (it plays even if vibration is off in Settings).
Emergency: three long pulses, repeating. Traffic: short-short, three times. Household/Entry: long-short, twice.

## Demonstration Mode

For presentations when live detection is not reliable. **Turn on Demonstration Mode**: a striped banner says
"Demonstration Mode · sample alerts only · microphone off · not saved" on every screen. **Sample captions** opens
Captions, where Start plays a sample script in the chosen language. **Show a sample alert** has a button for each of
the 13 sounds: the real alert screen and vibration, never saved. **Exit Demonstration Mode** to go back. It is off
again after AGAP restarts.

## Settings

- **Text size**: five sizes, applied to every screen including captions and alerts. It multiplies the phone's own font
  size.
- **Display mode**: Standard, or High contrast (black background, white text).
- **Vibration**: vibrate with each alert (on or off). The alert screen then says "Vibration: Off".
- **Alert threshold**: 10% to 90% in steps of 5 (50% to start with). Higher means fewer false alerts but more missed
  quiet sounds. It applies from the next Start.
- **About AGAP and credits**: the version, what AGAP is and is not, and the licences of the sound and speech models.

## The watch

Open **AGAP** on the watch and keep it open during sessions. It shows "Phone not connected" (keep the phone near,
Bluetooth on, AGAP open on the phone), "Phone connected", or "Sound Alerts on". With Live Captions running on the
phone it shows the captions in large text. Each alert fills the watch screen in the group's colour and shape and
vibrates in the group's pattern; tap it to close (this does not close it on the phone). The watch has no microphone
of its own in AGAP: the phone does all the listening.
