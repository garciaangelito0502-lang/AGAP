# AGAP test build 0.9: phone and watch

Test build 0.9 is the complete test build: the phone app and, for the first time, the watch app (both show version
0.7.0 inside the app). Please try the steps below during your alpha test and send the answers back by
**20 November**.

The watch part matters most: I tested the watch app only on a computer simulation of a watch, not on a real one, and
the link between phone and watch over Bluetooth has **not been tried yet**. Your results are the first real test.

## What is new since test build 0.1

- **Phone:** Filipino captions; the full-screen alert with its vibration; Event History with export; Demonstration
  Mode; Sound Categories and Vibration Patterns pages; Home shows what is ready, and whether a watch is connected.
- **Watch:** each alert full screen (icon, name, group, colour and shape) with the group's vibration; the phone's
  live captions; a clear message when the phone is not connected.

## 1. Install the phone app

1. Copy `agap-phone-0.9-test.apk` (about 430 MB) to the Infinix. It needs about 1.5 GB free (K-017).
2. Tap the file and tap **Install**. It replaces test build 0.1; you do not need to uninstall it.
3. Open **AGAP**. Allow the microphone when asked.

## 2. Install the watch app (I can help remotely)

You need a Wear OS 3 or newer watch (Galaxy Watch4 or newer, Pixel Watch), already paired with the Infinix in the
usual way (Galaxy Wearable or Pixel Watch app), and a computer with Android's `adb` tool. Message me and I will go
through it with you on a call.

1. On the watch: in Settings > About, tap the build number (Galaxy Watch: software version) 7 times to turn on
   Developer options. Then in Developer options turn on **ADB debugging** and **Wireless debugging**, with the
   watch and the computer on the same Wi-Fi.
2. On the computer: `adb pair <address and code shown on the watch>`, then `adb connect <address>`, then
   `adb install agap-watch-0.9-test.apk`.
3. Open **AGAP** on the watch and tap **Allow** for notifications.

## 3. Watch link

Bluetooth on, phone and watch near each other. Keep **AGAP open on both** unless a step says otherwise.

| # | Do this | What should happen | What happened |
|---|---|---|---|
| 1 | Open Home on the phone | "Watch connected: (your watch's name)" in the status card | |
| 2 | Look at the watch | "Phone connected" | |
| 3 | Phone: Alerts > **Start** | Watch: "Sound Alerts on" | |
| 4 | Play a fire alarm, a car horn and a doorbell (as in your trials) | Each alert also shows on the watch and vibrates in its group's pattern. Is it at the same time as the phone, about 1 second later, or later? | |
| 5 | Tap the alert on the watch | It closes (the phone's alert stays until you tap Dismiss there) | |
| 6 | Let the watch screen go dim (AGAP still open on it), then play a sound | The watch vibrates, the screen wakes and shows the alert | |
| 7 | Phone: Captions > **Start**, English, then Filipino; read 2 sentences each | The words appear on the watch, large. Readable? How far behind the phone? | |
| 8 | Close AGAP on the watch (watch face showing), play a sound with Sound Alerts on | The watch vibrates and a notification shows. Tell me exactly what you see. | |
| 9 | Turn off Bluetooth on the phone for 1 minute, then turn it on | Watch: "Phone not connected" within about 30 seconds, then "Phone connected" again | |
| 10 | Phone in **airplane mode** with Bluetooth on; repeat step 4 once | Same as step 4: no internet needed | |

If you have a phone that can record video, filming both screens in step 4 lets me measure the delay exactly.

## 4. Phone (if not done with 0.1)

- **Filipino captions:** Captions > Filipino > Start (the first time it sets up for about a minute). Read 3 sentences.
- **Alert screen:** with Sound Alerts on, play a fire alarm. The alert should fill the screen in red and vibrate.
  Tap Dismiss. Open **History** and tap the alert: both times and the response time are shown.
- **Demonstration Mode:** Home > Demonstration Mode > turn on > tap one sample alert of each group. Then Exit.
- **Settings:** try the largest text size and high contrast once. Is anything cut off or hard to read?

## 5. Send back (by 20 November)

- Watch model and Wear OS version (watch Settings > System > About).
- The table in section 3, filled in, with screenshots or videos.
- Anything that crashed, froze, looked wrong, or was hard to read or feel.
