# AGAP 1.1: background listening, test on the Infinix

Version 1.1 adds background listening (off until you turn it on). Everything else is as in 1.0. Please try the
steps below on the Infinix Hot 70 and send back the answers and screenshots. About 30 minutes. Do it in airplane mode
except step 1.

## 1. Update

- Copy `agap-phone-1.1.apk` to the phone's Download folder and tap it; **Update** (no need to uninstall 1.0).
- Open AGAP > History: are your old alerts still there? ______
- Settings > About: does it say **Version 1.1**? ______
- If you use the watch, update it too: `adb install -r agap-watch-1.1.apk`.

## 2. Turn it on

- Settings > **Background listening** > turn on **Keep listening when I leave AGAP**. Allow notifications.
- Which rows still say **Not allowed yet**? Tap **Allow** on each. Write what each button opened:
  - Alerts on the lock screen: ______
  - Popups over other apps: ______
  - No battery limits: ______
  - Home-screen widget (Add): did a widget appear? ______
- Infinix battery settings: in Settings > Apps > AGAP (and Phone Master, if there is one), what are the options
  called for running in the background / auto-start? ______ Turn them on. Lock AGAP in the recent-apps list.

## 3. Sound Alerts in the background

Use a clip that worked in your alpha test (for example the smoke or fire alarm).

| Try | What to do | What happened? (alert seen? vibration? "Shown as" in History) |
|---|---|---|
| a | Alerts > Start, press Home, play the sound | |
| b | Open another app (for example Gallery), play the sound | |
| c | Press the power button (screen off), wait 10 s, play the sound | |
| d | Tap the alert's notification in the notification list | |
| e | Pull the notifications down, open "AGAP is listening", tap **Stop** | |

## 4. Live Captions in the background

| Try | What to do | What happened? |
|---|---|---|
| a | Captions > English > Start, press Home, read a sentence aloud | Floating window? Words right? |
| b | Same with Filipino | |
| c | Screen off, read three sentences, then open AGAP | Are the sentences on the Captions screen? |

## 5. Does it keep going?

- Start Sound Alerts, leave AGAP, screen off. After **30 minutes**, play the sound once. Did the alert come? ______
  Battery before ______ % and after ______ % (off the charger).
- With background listening **off** (Settings), Start Sound Alerts, press Home: the notification should not appear
  and nothing is heard. Correct? ______

## 6. Send back (within 2 days)

The answers above, screenshots of anything odd, and the CSV export of the History (top bar, download icon).

Remember: the Methods place background operation outside the present study. Ask your adviser before using it in
data collection, and keep it **off** in the formal trials.
