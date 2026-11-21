# How to install AGAP 1.1

This guide is for the research team. It takes about 20 minutes for the phone and 15 for the watch. You need the
internet only once, in step 5. After that AGAP works with no Wi-Fi and no mobile data.

**What you need**

- The test phone (Infinix Hot 70, or any phone with Android 10 or newer) with **at least 1.5 GB of free storage**
  (Settings > Storage). The app file is 426 MB, and Filipino captions need about 1 GB more once they are set up.
- The two files: `agap-phone-1.1.apk` (426 MB) and, for the watch, `agap-watch-1.1.apk` (5 MB).

**Already have AGAP 1.0?** Skip step 1: 1.1 installs over 1.0 and keeps your Event History and settings (steps 2
and 3, then step 10 if you want background listening). Update the watch the same way (step 8).
- For the watch: a Wear OS 3 or newer watch (Galaxy Watch4 or newer, Pixel Watch) already paired with the phone,
  and a computer with the free Android "platform tools" (adb). If you have no computer for this, I can help you by
  video call.

## Phone

### 1. Remove a test build first (only if you installed one)

If an AGAP test build (0.1, 0.9 or 0.9.1) is on the phone, remove it: **Settings > Apps > AGAP > Uninstall**.
Versions 1.0 and 1.1 are signed differently from the test builds, and Android will not install them over a test
build ("App not installed").
Uninstalling deletes the test build's Event History. If you still need it, open **History** first and tap the
download icon at the top (**Export history**): the file goes to the phone's Download/AGAP folder.

### 2. Copy the app to the phone

Connect the phone to the computer with a USB cable, choose **File transfer** on the phone, and copy
`agap-phone-1.1.apk` into the phone's **Download** folder. (Sending the file by chat apps often fails because of
its size.)

### 3. Install it

1. On the phone, open the **Files** app, go to **Download** and tap `agap-phone-1.1.apk`.
2. The first time, Android asks whether the Files app may install apps. Tap **Settings**, turn on
   **Allow from this source**, and go back.
3. Tap **Install**. If the phone shows a security scan, let it finish, then tap **Install** again or **Done**.
4. Tap **Open** (or **Launch**). AGAP's Home screen appears.

Some phones add a notice after installing an app from a file. On a Xiaomi phone it said "Permissions denied: The
system has blocked this app from using sensitive permissions". It did not stop AGAP from asking for the microphone
in step 4, so you can tap **OK** or **Launch**. If the microphone question never appears, tap **Allow this app to
use permissions** on that notice (or find the same switch in Settings > Apps > AGAP).

With a computer and USB debugging turned on, you can do steps 2 and 3 in one go instead:
`adb install -r agap-phone-1.1.apk` (`-r` keeps 1.0's history).

### 4. Allow the microphone

Tap **Captions** at the bottom, then **Allow microphone**, then **While using the app** (or **Allow**). AGAP
listens only while its Captions or Alerts screen is open, and nothing is recorded or saved.

If you tapped "Don't allow" by mistake: **Settings > Apps > AGAP > Permissions > Microphone > Allow only while
using the app**.

### 5. English speech pack (once, with internet)

1. On **Captions**, choose **English**.
2. If the screen says "English captions need Google's offline English speech pack", connect to Wi-Fi and tap
   **Download English pack**. Keep the phone online until it finishes (a few minutes), then tap **Check again**.
3. When the message is gone and **Start** can be tapped, the pack is installed. You can switch the internet off.

On some phones Google's offline English is not available at all. AGAP then says it uses its own built-in English
model and asks for a one-time set-up like Filipino in step 6. That needs no internet.

### 6. Filipino set-up (once)

1. On **Captions**, choose **Filipino**.
2. Tap **Set up now**. A progress bar shows "Setting up Filipino captions…". It takes about a minute (more on a
   slower phone). Keep AGAP open on this screen until it finishes.
3. When **Start** can be tapped, Filipino captions are ready. You never need to do this again unless AGAP is
   uninstalled.

### 7. Check that the phone is ready

1. Open **Home**. The card **Ready to work offline** should show a check mark for: no internet needed, the
   microphone, Sound Alerts, English captions and Filipino captions.
2. Turn on **airplane mode**. Open **Captions**, choose a language, tap **Start**, and say a sentence. The words
   appear. Tap **Stop**.
3. Open **Alerts**, tap **Start**, and play a test sound (for example a car horn from a video on another phone).
   The alert fills the screen and the phone vibrates. Tap **Dismiss**, then open **History**: the alert is there
   with its times.

Run the formal trials in airplane mode: a phone call during a trial rings as a "Phone Ringing" alert, and answering
it stops listening. Tap **Dismiss** after every alert.

## Watch

### 8. Install the watch app (computer and Wi-Fi)

1. **Turn on developer options on the watch:** Settings > System > About > Versions (or Software information) >
   tap **Software version** 7 times until it says you are a developer.
2. **Turn on debugging:** Settings > Developer options > **ADB debugging** on, then **Wireless debugging** on. The
   watch and the computer must be on the same Wi-Fi.
3. In **Wireless debugging**, tap **Pair new device**. The watch shows a pairing code and an address such as
   `192.168.1.23:41234`.
4. On the computer, open a terminal in the folder with the platform tools and the watch file, and type (with the
   numbers from your watch):
   ```
   adb pair 192.168.1.23:41234          (then type the pairing code)
   adb connect 192.168.1.23:<port shown under "IP address & port">
   adb install -r agap-watch-1.1.apk
   ```
   It says `Success`.
5. On the watch, open **AGAP** and allow notifications. Turn **Wireless debugging** off again to save battery.

The watch and phone files must both be release files (1.0 or 1.1, best both 1.1). A test watch app and a release
phone app (or the other way round) are signed differently and will not connect: uninstall the old one first.

### 9. Check the watch link

1. Keep Bluetooth on, the watch paired with the phone, and AGAP **open** on both.
2. The watch should change from "Phone not connected" to **Phone connected** within a few seconds, and the phone's
   Home card should say **Watch connected:** with the watch's name.
3. On the phone, open **Home > Demonstration Mode**, turn it on and tap one sample alert. The watch shows the same
   alert and vibrates. Exit Demonstration Mode afterwards.

Keep AGAP open on the watch during sessions: with it closed, alerts arrive only as a notification (K-022).

## Background listening (optional, new in 1.1)

### 10. Turn it on only if you want it

With background listening **off** (as installed), AGAP works exactly like 1.0. Turn it on only if you want AGAP to
keep listening after you leave it or the screen turns off. The group's formal test trials can stay with it off.

1. In AGAP, tap the settings icon at the top right, scroll to **Background listening** and turn on **Keep
   listening when I leave AGAP**.
2. Android asks whether AGAP may send notifications: tap **Allow**.
3. Under the switch, every row that says **Not allowed yet** has an **Allow** button:
   - **Alerts on the lock screen** (only on Android 14 and newer): opens Android's page; turn AGAP on.
   - **Popups over other apps** (optional): opens "Display over other apps". On some phones this is a list: find
     **AGAP** in it, open it and turn it on.
   - **No battery limits**: tap **Allow** in Android's question, or choose **No restrictions** if the phone shows its
     own battery page.
   - **Home-screen widget** (optional): **Add** asks the home screen to place it. If nothing happens, add it by
     hand (touch and hold an empty spot on the home screen, **Widgets**, **AGAP**). On Xiaomi phones the Add
     button works only after **Settings > Apps > AGAP > Other permissions > Home screen shortcuts** is allowed.
4. Phone makers' own battery savers can still close AGAP in the background. On the **Infinix** (XOS), look in
   **Settings > Apps > AGAP** (and in **Phone Master**, if the phone has it) for **auto-start** or **background
   activity** and allow it, and lock AGAP in the recent-apps list (pull its card down or tap its lock icon). These
   Infinix steps were not tried by me; tell me what the menus are called on your phone. On a Xiaomi phone (seen on
   HyperOS): **Settings > Apps > AGAP** has **Background autostart** and **Pause app activity if unused** (turn the
   second one off), and the battery page's **No restrictions**.
5. Check: open **Alerts**, tap **Start**, press the Home button. A notification **AGAP is listening: Sound Alerts**
   stays at the top. Play a test sound: the alert appears as a popup or notification and vibrates. Tap **Stop** in
   the notification (pull the notification down to see the button).

AGAP never starts listening by itself, not even after a restart: open it and tap **Start**.

## If something goes wrong

| What you see | What to do |
|---|---|
| "App not installed" | A test build is still installed: step 1. Or the phone is short of storage. |
| "There was a problem parsing the package" | The file did not copy completely. Copy it again and compare its SHA-256 with [RELEASE.md](RELEASE.md). |
| Start stays grey on Captions | Read the message above it: the microphone is not allowed (step 4), the English pack is missing (step 5), or Filipino is not set up (step 6). |
| "Not enough free storage to set up…" | Free some space (photos, videos, apps), then tap **Check again**. |
| The watch says "Phone not connected" | Bluetooth on, watch paired, AGAP open on the phone, both release files (1.0 or 1.1). |
| Background listening stops by itself | The notification is gone: the phone closed AGAP. Allow "No battery limits" and the phone maker's auto-start (step 10), then open AGAP and tap Start again. |
| Anything else | Write down what you did and what the screen said, take a screenshot, and send it to me. |
