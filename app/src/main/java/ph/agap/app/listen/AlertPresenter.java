package ph.agap.app.listen;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.history.ShownVia;
import ph.agap.app.ui.alerts.AlertOverlayFragment;
import ph.agap.app.vibration.AlertVibrator;
import ph.agap.app.watch.WatchSender;

/**
 * Shows each detected alert in the best way for where the user is (Phase 11):
 *
 * - AGAP in view: the full-screen alert over AGAP, exactly as in 1.0 (APP_SCREEN).
 * - Screen off or locked: the full-screen alert over the lock screen, through a full-screen
 *   notification (LOCK_SCREEN). If Android does not open it within LOCK_SCREEN_WAIT_MS, the
 *   notification itself counts (NOTIFICATION).
 * - Another app in use, and "Display over other apps" allowed: an alert card over that app (POPUP),
 *   plus a quiet entry in the notification list.
 * - Otherwise: a heads-up notification (NOTIFICATION).
 *
 * Outside AGAP the category's vibration and the watch alert start at once, before any screen is
 * drawn. Every real alert is saved once (AlertLedger). Main thread only.
 */
public final class AlertPresenter {

    private static final String TAG = "AGAP/Alerts";
    /** How long Android has to open the lock-screen alert before the notification counts as shown. */
    static final long LOCK_SCREEN_WAIT_MS = 3000;

    private static final Handler main = new Handler(Looper.getMainLooper());
    @Nullable private static AlertVibrator vibrator;

    private AlertPresenter() { }

    static void present(Context app, AlertEvent event, int thresholdPercent) {
        FragmentActivity host = AppVisibility.alertHost();
        if (host != null) {
            ShownVia via = host instanceof AlertActivity && isLocked(app) ? ShownVia.LOCK_SCREEN : ShownVia.APP_SCREEN;
            AlertOverlayFragment.show(host, event, thresholdPercent, false, via, 0);
            return;
        }

        long id = AlertLedger.add(event, thresholdPercent);
        WatchSender.get(app).sendAlert(event, false);
        vibrator(app).play(event.category());
        boolean locked = isLocked(app);
        boolean notifications = BackgroundPermissions.notificationsAllowed(app);

        if (locked && notifications && BackgroundPermissions.fullScreenAllowed(app)) {
            long posted = AgapNotifications.postAlert(app, id, event, true, false);
            main.postDelayed(() -> {
                if (AlertLedger.record(app, id, posted, ShownVia.NOTIFICATION)) {
                    Log.w(TAG, "lock-screen alert did not open within " + LOCK_SCREEN_WAIT_MS + " ms");
                }
            }, LOCK_SCREEN_WAIT_MS);
        } else if (BackgroundPermissions.overlayAllowed(app) && (!locked || !notifications)) {
            if (locked) {
                // No notifications allowed, but "Display over other apps" lets AGAP open its
                // lock-screen alert itself.
                openLockScreenAlert(app, id);
            } else {
                boolean popup;
                try {
                    PopupAlert.show(app, id, event);
                    popup = true;
                } catch (RuntimeException e) {
                    // The popup must never stop listening: fall back to the heads-up notification (K-033).
                    Log.e(TAG, "popup failed, notification instead", e);
                    popup = false;
                }
                long posted = AgapNotifications.postAlert(app, id, event, false, popup);
                if (!popup) AlertLedger.record(app, id, posted, ShownVia.NOTIFICATION);
            }
        } else {
            long posted = AgapNotifications.postAlert(app, id, event, false, false);
            // With notifications off the alert still vibrates and goes to the watch; it is saved the same way.
            AlertLedger.record(app, id, posted, ShownVia.NOTIFICATION);
        }
    }

    /** Stops the vibration of a background alert (its popup or lock-screen alert was dismissed). */
    public static void stopVibration(Context context) {
        if (vibrator != null) vibrator.stop();
    }

    static boolean isLocked(Context context) {
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        PowerManager power = context.getSystemService(PowerManager.class);
        return (keyguard != null && keyguard.isKeyguardLocked()) || (power != null && !power.isInteractive());
    }

    private static void openLockScreenAlert(Context app, long id) {
        try {
            app.startActivity(AlertActivity.intent(app, id));
        } catch (RuntimeException e) {
            Log.w(TAG, "could not open the lock-screen alert: " + e.getMessage());
        }
        main.postDelayed(() -> AlertLedger.record(app, id, System.currentTimeMillis(), ShownVia.NOTIFICATION),
                LOCK_SCREEN_WAIT_MS);
    }

    private static AlertVibrator vibrator(Context app) {
        if (vibrator == null) vibrator = new AlertVibrator(app);
        return vibrator;
    }
}
