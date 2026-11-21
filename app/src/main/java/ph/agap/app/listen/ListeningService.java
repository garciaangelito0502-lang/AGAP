package ph.agap.app.listen;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import ph.agap.app.settings.SettingsStore;

/**
 * Keeps AGAP listening while it is not in view (Phase 11): a foreground service of type
 * "microphone", with the "AGAP is listening" notification and its Stop button. It runs only while
 * background listening is on in Settings and Sound Alerts or Live Captions (not Demonstration Mode)
 * is listening, and it is always started while AGAP is in view: Android 14 and newer do not let an
 * app start using the microphone from the background, nor after a restart. The sessions do the
 * listening; this service only keeps the app (and its microphone access) alive, and holds a partial
 * wake lock so listening continues with the screen off. It does not restart itself if Android
 * stops it. Main thread only.
 */
public final class ListeningService extends Service {

    private static final String TAG = "AGAP/Background";
    /** The notification shows new caption words at most this often (Android limits updates). */
    private static final long CAPTION_UPDATE_MS = 1000;

    @Nullable private static ListeningService running;
    private static boolean starting;
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static boolean captionUpdatePending;

    @Nullable private PowerManager.WakeLock wakeLock;

    /** True when listening should go on in the background now. */
    static boolean wanted(Context context) {
        return SettingsStore.from(context).isBackgroundListeningOn()
                && (AlertsSession.get(context).isRunning() || CaptionsSession.get(context).isListeningForReal());
    }

    /** True while the service is in the foreground, so leaving AGAP will not stop listening. */
    public static boolean isRunning() {
        return running != null;
    }

    /** Starts or stops the service to match what is listening; refreshes its notification. */
    public static void update(Context context) {
        Context app = context.getApplicationContext();
        boolean wanted = wanted(app);
        if (wanted) {
            if (running != null) {
                running.showNotification();
            } else if (!starting) {
                starting = true;
                try {
                    ContextCompat.startForegroundService(app, new Intent(app, ListeningService.class));
                } catch (RuntimeException e) {
                    // For example AGAP was not in view (Android 12+ refuses): listening stays foreground-only.
                    starting = false;
                    Log.w(TAG, "could not start background listening: " + e);
                }
            }
        } else if (running != null && !starting) {
            running.stopNow();
        }
        // While starting, onStartCommand checks again whether the service is still wanted.
    }

    /** New caption words: update the notification text, at most once a second. */
    static void captionsChanged(Context context) {
        if (running == null || captionUpdatePending) return;
        captionUpdatePending = true;
        main.postDelayed(() -> {
            captionUpdatePending = false;
            if (running != null) running.showNotification();
        }, CAPTION_UPDATE_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        starting = false;
        try {
            int type = Build.VERSION.SDK_INT >= 30 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE : 0;
            ServiceCompat.startForeground(this, AgapNotifications.LISTENING_ID,
                    AgapNotifications.listening(this), type);
        } catch (RuntimeException e) {
            // Android refused the microphone in the background (for example the permission was taken away).
            Log.w(TAG, "background listening refused: " + e);
            stopSelf();
            return START_NOT_STICKY;
        }
        running = this;
        if (!wanted(this)) {
            stopNow();
            return START_NOT_STICKY;
        }
        holdWakeLock();
        Log.i(TAG, "background listening on");
        AgapWidget.refresh(this);
        return START_NOT_STICKY;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        // Android 15+ time limits do not apply to microphone services; stop cleanly if one ever does.
        Log.w(TAG, "Android ended background listening (timeout)");
        AlertsSession.get(this).stop();
        CaptionsSession.get(this).stop();
        stopNow();
    }

    private void showNotification() {
        AgapNotifications.notify(this, AgapNotifications.LISTENING_ID, AgapNotifications.listening(this));
        holdWakeLock();
    }

    /** Held while the service runs and released when it stops or is destroyed. */
    @android.annotation.SuppressLint("WakelockTimeout")
    private void holdWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        if (wakeLock == null) {
            PowerManager pm = getSystemService(PowerManager.class);
            if (pm == null) return;
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AGAP:listening");
            wakeLock.setReferenceCounted(false);
        }
        wakeLock.acquire();
    }

    private void stopNow() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
        running = null;
        releaseWakeLock();
        Log.i(TAG, "background listening off");
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    @Override
    public void onDestroy() {
        if (running == this) running = null;
        releaseWakeLock();
        AgapWidget.refresh(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
