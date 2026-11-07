# Phase 08: Offline proof, hardening and tests

Status: **Done** on my phone (Xiaomi 13, Android 16). Not on the Infinix; no watch (K-005, K-021).
Started 2026-11-07, finished 2026-11-08 (target 2026-11-19 to 11-20). Build: phone 0.8.0 (debug, code 10); watch
unchanged at 0.7.0.

## Goal and what was built

Prove that every feature works with no network, find what breaks in long or repeated use, and fix it.

- **Offline proof:** every feature ran end to end in airplane mode with Wi-Fi off and Bluetooth on.
- **ANR audit:** the `anr-audit-and-fix` checklist over all sources; four main-thread waits fixed (D-046).
- **Robustness:** rotation, dark-mode switch, Home and back, a real phone call, another app recording, the
  microphone permission taken away, low storage, low battery with battery saver, Bluetooth off, quick taps, 50
  Start/Stop cycles per feature, a phone restart. Five bugs found and fixed (K-025 to K-029).
- **New in the app:** a message when Android silences the microphone (D-047); the main screen survives a dark-mode
  switch (D-048); Sound Alerts frees the Filipino model (D-049); pulses drawn in steps (D-051); one Vosk model
  load at a time (D-052).

## Files and modules added or changed

- `alerts/SoundAlertEngine`: microphone opened under a lock; `stop()` waits only while it is open; silenced
  watch (`registerAudioRecordingCallback`); `Listener.onMicSilenced`; timing logs.
- `captions/VoskCaptionEngine`: the same lock and silenced watch; `CaptionListener.onMicSilenced` (default method).
- `captions/VoskModelHolder`: models closed outside the lock, in the background from the main thread; one acquire
  at a time with a "still wanted" check.
- `captions/VoskModelManager`: `discard()` renames, then deletes in the background; leftovers deleted at the next unpack.
- `ui/SlowPulse` (new) and `SlowPulseTest` (4 tests); `ui/alerts/AlertsFragment`, `AlertsViewModel`,
  `AlertOverlayFragment`, `ui/captions/CaptionsFragment`, `CaptionsViewModel`: pulses, silenced message, model freed
  at Sound Alerts start.
- `AndroidManifest.xml`: `configChanges="uiMode"` on MainActivity. `strings.xml`: `mic_silenced`.
- `app/build.gradle`: 0.8.0, code 10.
- Docs: DECISIONS (D-046 to D-052), KNOWN-ISSUES (K-025 to K-031; K-012, K-014, K-019 updated), DETECTION-CHECK
  (Phase 8), SPEC-CHECKLIST, CHANGELOG, DEVICE-NOTES, CLIENT-TEST-CHECKLIST-0.9 (Dismiss between trials, airplane mode).
- Test scripts outside the repo (`test-audio/phase8/`): `dev.py`, `longrun.py`, `cycles.py`, `rapid.py`, `robust.py`.

## Decisions made and why

D-046 main-thread fixes from the ANR audit; D-047 silenced-microphone message; D-048 dark mode does not rebuild the
main screen; D-049 Filipino model freed when Sound Alerts starts; D-050 how Phase 8 was tested; D-051 pulses in
steps; D-052 one Vosk model load at a time.

## How it was verified

Phone: Xiaomi 13, Android 16, debug builds of 0.8.0 (four builds in the evening as fixes went in; which check ran
on which is in the Phase 8 report). Airplane mode on, Wi-Fi off, Bluetooth on, except during the phone call.

### Offline proof (airplane mode, Wi-Fi off, Bluetooth on, `ping`: network unreachable)

| Feature | Result |
|---|---|
| English captions (Google offline) | The owner read 2 sentences: both word for word |
| Filipino captions (Vosk) | The owner read 2 sentences: 11 of 12 words ("mo" for "mong"); model loaded in 1.2 s |
| Sound Alerts | Car horn and smoke alarm clips: Vehicle Horn 59% and Fire Alarm 98%, shown 98 and 27 ms after detection, vibrated (alarm), saved |
| Event History | Both alerts listed; detail with both times to the ms and the response time |
| Demonstration Mode | Sample Knocking alert (not saved) and sample Filipino captions; no recording in Android's list; history count unchanged |
| Watch link | No watch: Home "No watch connected", log "0 AGAP watch(es) connected", no crash |
| No network | No network permission in either merged release manifest; no network code in AGAP's sources; Android's per-app network statistics have no entry for AGAP's uid (10432) |

### ANR audit (static, every source file)

Fixed (all on the main thread, D-046): `SoundAlertEngine.stop()` waited up to 1.5 s, `VoskCaptionEngine.stop()`
up to 0.5 s, while the engine was still starting; `VoskModelHolder` closed a 1 GB model on the main thread and
under the lock the main thread uses; `VoskModelManager.discard()` deleted about 800 MB of files on the main thread.

Dismissed: the audio read loops, YAMNet set-up and classification, the Vosk model load and recognizer run on each
engine's worker thread; the model unpack, Room and the CSV export on their own single threads; the Annex A asset,
the unpack marker file, free-space checks and preferences read on the main thread are small and bounded
(preferences are written with `apply()`); Google recognizer and Play services calls are asynchronous; no
background thread touches a view (every engine, repository and watch callback is posted to the main thread).

### Robustness

| Check | Result |
|---|---|
| Rotation (landscape asked for while listening) | Stays portrait; listening goes on (9 windows in 4 s), no stop |
| Dark-mode switch while listening | Before: the screen was rebuilt and listening stopped without a message (K-025). After D-048: two switches, no stop |
| Home and back, each feature | Stop logged, microphone released (Android's recording list); back on the same screen, stopped |
| Phone call while Sound Alerts listens | Ringing (about 50 s): the phone's own ringtone raised Phone Ringing 3 times (67-92%), saved (K-031). Answered: the call screen covered AGAP, stop in 191 ms. After: Sound Alerts, stopped |
| Another app recording (Sound Recorder in the background) | Android silenced the recorder and gave AGAP the microphone; AGAP not silenced. The "AGAP hears nothing" message (D-047) could not be triggered |
| Low battery (4%) and battery saver | Listening went on (19 windows in 5 s); HyperOS's low-battery dialog covered the screen; the alarm vibration played in full in battery saver |
| Bluetooth off while listening | Listening went on (12 windows in 5 s), same process |
| Watch disconnect and reconnect | Not possible: no watch (K-021) |
| Microphone permission taken away while listening | Android ended the process (that is how Android takes a permission back); nothing left recording; reopened cleanly on Home: "Microphone off and not allowed yet"; crash log empty |
| Low storage (English fallback model, debug switches of D-027, 100 MB "free") | "Not enough free storage to set up English captions. They need about 300 MB free, and this phone has 100 MB"; Start disabled. With the real free storage: the set-up message, unpacked in 0.7 s, English listened on AGAP's own model |
| Quick taps (20-21 taps 40-70 ms apart, each feature) | Start and Stop counts matched the screen; nothing left recording. Filipino with the model freed: 11 model loads at once, 5.3 GB (K-029). After D-052: 1.1 GB peak, one load |
| After a phone restart | First tap worked in each: Sound Alerts listening 1.3 s after Start, English 0.9 s, Filipino 2.4 s (model load 2.3 s) |

### Long run (30 minutes) and cycles

- **Long run:** Sound Alerts 32 minutes in one session on the cable (the phone was charging), with the Phase 3
  detection plan played in the middle. Same process throughout, screen on throughout, no crash or app error in the
  process's log, memory 169-239 MB (227 MB most of the time), thermal status 0, battery temperature 33.5-34.5 °C,
  YAMNet 13 ms per window (median, p95 16 ms). Battery 36% to 29% on the charger: the charger did not keep up;
  real battery drop off the cable not measured. Processor use 37-81% of one core, which led to K-028.
- **Detection check:** in DETECTION-CHECK.md (Phase 8): 0 of 90 priority-sound trials at 50% through the Bluetooth
  speaker in a room with talking; 64 alerts raised, 64 saved, shown 13-99 ms after detection.
- **Start/Stop cycles:** Sound Alerts 90, English 90, Filipino 194; 29-30 of each on the final build, the rest on
  the build before the last fix (D-052). Sound Alerts: all clean, first window 1.06-1.14 s after Start, Stop
  36-79 ms. English: all clean, ready 51-120 ms after Start. Filipino: 2 log misses (K-030), Stop 45-128 ms, first
  Start 1.4-1.6 s (model load), later ones 25-45 ms. After every run nothing was recording and no listening thread
  was left (Android's recording list, the process's thread list). Final build, 10 each with Stop times: Sound
  Alerts Stop 53-75 ms, Filipino 51-82 ms.

### Tests

- `testDebugUnitTest`: 104 of 104 pass (phone 93, shared 7, watch 4); new: `SlowPulseTest` (4).
- Instrumented tests on my phone (`am instrument`, animations off for the run, restored to 1.0 after): 12 of 12 pass.
- `lintDebug`: phone 0 errors, 5 warnings (the same as Phase 7); watch 0 and 2; shared 0 and 1.
- **Not covered by automated tests:** the engines themselves (they need a microphone and the models), the
  microphone lock and the silenced watch, `VoskModelHolder`'s one-load rule, the watch link, the alert screen's
  timing, and every screen except navigation and Settings. These were checked by hand on the phone as above.

**Not verified:** the Infinix; a real watch, and watch disconnect and reconnect; the "AGAP hears nothing" message on
a phone (no app here takes priority over AGAP); battery use off the cable; the history with storage really full
(only the model's low-storage message, with a debug switch).

## Contract items covered

1.2 offline (proof complete on my phone), 1.4 and 1.6 over a long session, 1.5 vibration in battery saver, 1.9
demo in airplane mode, 1.10 one microphone user and listening only on screen (cycles, call, other app, restart),
1.11 phone side with Bluetooth off.

## Known issues and scope flags

New: K-025 to K-029 (fixed), K-030 (watching), K-031 (accepted). Updated: K-012, K-014, K-019. No new scope flag.

## Next phase

Phase 9: release build, install guide and turnover (target 2026-11-21, M3). The group's alpha test (11-19 to 11-20)
should use a build with the Phase 8 fixes.
