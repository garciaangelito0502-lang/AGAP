package ph.agap.wear;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import ph.agap.app.watch.WatchMessages;

/**
 * What the watch knows, in memory only: what the phone is doing, the newest caption lines and an
 * alert waiting to be seen. Messages from the phone arrive here (WatchListenerService) and are
 * handled on the main thread. With the AGAP watch screen in front, an alert vibrates here; with it
 * closed, the alert becomes a notification that vibrates and pops up.
 */
final class WatchHub {

    private static final String TAG = "AGAP/Watch";

    interface Listener {
        /** Something on the watch screen should change. */
        void onWatchChanged();

        /** A new alert arrived while the screen is in front (it may be dimmed). */
        void onAlert();
    }

    @Nullable private static WatchHub instance;

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final WatchVibrator vibrator;
    @Nullable private Listener listener;

    @Nullable private WatchMessages.State state;
    private final CaptionBuffer captions = new CaptionBuffer();
    @Nullable private WatchMessages.Alert pendingAlert;
    /** elapsedRealtime of the last message from the phone; 0 when none yet. */
    private long lastHeardMillis;

    private WatchHub(Context context) {
        this.context = context.getApplicationContext();
        this.vibrator = new WatchVibrator(this.context);
    }

    static synchronized WatchHub get(Context context) {
        if (instance == null) instance = new WatchHub(context);
        return instance;
    }

    /** From any thread. Unknown or damaged messages are ignored. */
    void onMessage(String path, byte[] data) {
        main.post(() -> handle(path, data));
    }

    private void handle(String path, byte[] data) {
        switch (path) {
            case WatchMessages.PATH_ALERT: {
                WatchMessages.Alert alert = WatchMessages.readAlert(data);
                if (alert == null) return;
                heard();
                pendingAlert = alert;
                Log.i(TAG, (alert.demo ? "sample alert " : "alert ") + alert.label);
                if (listener != null) {
                    // The AGAP screen is in front: it shows the alert; the pattern plays here
                    // (Emergency repeats until the alert is closed, at most a minute).
                    if (alert.vibrate) vibrator.play(alert.category); else vibrator.stop();
                    listener.onAlert();
                } else {
                    // Otherwise the notification vibrates in the pattern and pops up.
                    vibrator.stop();
                    AlertNotifier.show(context, alert);
                }
                changed();
                break;
            }
            case WatchMessages.PATH_CAPTION: {
                WatchMessages.Caption caption = WatchMessages.readCaption(data);
                if (caption == null) return;
                heard();
                captions.add(caption.text, caption.isFinal);
                changed();
                break;
            }
            case WatchMessages.PATH_STATE: {
                WatchMessages.State next = WatchMessages.readState(data);
                if (next == null) return;
                heard();
                // A new caption session starts with an empty screen, as on the phone.
                boolean wasCaptions = state != null && state.mode == WatchMessages.Mode.CAPTIONS;
                if (next.mode == WatchMessages.Mode.CAPTIONS && !wasCaptions) captions.clear();
                state = next;
                Log.i(TAG, "phone is now " + next.mode);
                changed();
                break;
            }
            default:
                // A message from a newer phone app: ignore it.
        }
    }

    private void heard() {
        lastHeardMillis = SystemClock.elapsedRealtime();
    }

    void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    WatchMessages.State state() {
        return state;
    }

    /** True when the phone sent something in the last {@code withinMillis}. */
    boolean heardFromPhoneWithin(long withinMillis) {
        return lastHeardMillis != 0 && SystemClock.elapsedRealtime() - lastHeardMillis <= withinMillis;
    }

    /** The phone is gone: what it was doing is no longer known. */
    void forgetState() {
        if (state == null) return;
        state = null;
        changed();
    }

    @Nullable
    WatchMessages.Alert pendingAlert() {
        return pendingAlert;
    }

    void dismissAlert() {
        pendingAlert = null;
        vibrator.stop();
        AlertNotifier.cancel(context);
        changed();
    }

    CaptionBuffer captions() {
        return captions;
    }

    private void changed() {
        if (listener != null) listener.onWatchChanged();
    }
}
