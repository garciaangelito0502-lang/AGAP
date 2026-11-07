# How to install AGAP 1.0

This guide is for the research team. It takes about 20 minutes for the phone and 15 for the watch. You need the
internet only once, in step 5. After that AGAP works with no Wi-Fi and no mobile data.

**What you need**

- The test phone (Infinix Hot 70, or any phone with Android 10 or newer) with **at least 1.5 GB of free storage**
  (Settings > Storage). The app file is 426 MB, and Filipino captions need about 1 GB more once they are set up.
- The two files: `agap-phone-1.0.apk` (426 MB) and, for the watch, `agap-watch-1.0.apk` (5 MB).
- For the watch: a Wear OS 3 or newer watch (Galaxy Watch4 or newer, Pixel Watch) already paired with the phone,
  and a computer with the free Android "platform tools" (adb). If you have no computer for this, I can help you by
  video call.

## Phone

### 1. Remove a test build first (only if you installed one)

If an AGAP test build (0.1, 0.9 or 0.9.1) is on the phone, remove it: **Settings > Apps > AGAP > Uninstall**.
Version 1.0 is signed differently, and Android will not install it over a test build ("App not installed").
Uninstalling deletes the test build's Event History. If you still need it, open **History** first and tap the
download icon at the top (**Export history**): the file goes to the phone's Download/AGAP folder.

### 2. Copy the app to the phone

Connect the phone to the computer with a USB cable, choose **File transfer** on the phone, and copy
`agap-phone-1.0.apk` into the phone's **Download** folder. (Sending the file by chat apps often fails because of
its size.)

### 3. Install it

1. On the phone, open the **Files** app, go to **Download** and tap `agap-phone-1.0.apk`.
2. The first time, Android asks whether the Files app may install apps. Tap **Settings**, turn on
   **Allow from this source**, and go back.
3. Tap **Install**. If the phone shows a security scan, let it finish, then tap **Install** again or **Done**.
4. Tap **Open** (or **Launch**). AGAP's Home screen appears.

Some phones add a notice after installing an app from a file. On a Xiaomi phone it said "Permissions denied: The
system has blocked this app from using sensitive permissions". It did not stop AGAP from asking for the microphone
in step 4, so you can tap **OK** or **Launch**. If the microphone question never appears, tap **Allow this app to
use permissions** on that notice (or find the same switch in Settings > Apps > AGAP).

With a computer and USB debugging turned on, you can do steps 2 and 3 in one go instead:
`adb install agap-phone-1.0.apk`.

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
   adb install agap-watch-1.0.apk
   ```
   It says `Success`.
5. On the watch, open **AGAP** and allow notifications. Turn **Wireless debugging** off again to save battery.

The watch and phone files must both be the 1.0 files. A test watch app and a 1.0 phone app (or the other way
round) are signed differently and will not connect: uninstall the old one first.

### 9. Check the watch link

1. Keep Bluetooth on, the watch paired with the phone, and AGAP **open** on both.
2. The watch should change from "Phone not connected" to **Phone connected** within a few seconds, and the phone's
   Home card should say **Watch connected:** with the watch's name.
3. On the phone, open **Home > Demonstration Mode**, turn it on and tap one sample alert. The watch shows the same
   alert and vibrates. Exit Demonstration Mode afterwards.

Keep AGAP open on the watch during sessions: with it closed, alerts arrive only as a notification (K-022).

## If something goes wrong

| What you see | What to do |
|---|---|
| "App not installed" | A test build is still installed: step 1. Or the phone is short of storage. |
| "There was a problem parsing the package" | The file did not copy completely. Copy it again and compare its SHA-256 with [RELEASE.md](RELEASE.md). |
| Start stays grey on Captions | Read the message above it: the microphone is not allowed (step 4), the English pack is missing (step 5), or Filipino is not set up (step 6). |
| "Not enough free storage to set up…" | Free some space (photos, videos, apps), then tap **Check again**. |
| The watch says "Phone not connected" | Bluetooth on, watch paired, AGAP open on the phone, both 1.0 files. |
| Anything else | Write down what you did and what the screen said, take a screenshot, and send it to me. |
