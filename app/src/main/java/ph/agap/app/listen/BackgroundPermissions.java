package ph.agap.app.listen;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

/**
 * What background listening needs from the user (Phase 11), each optional: notifications (alerts and
 * the "AGAP is listening" notice), full-screen alerts on the lock screen, display over other apps
 * (popup alerts and floating captions), and no battery limits. Each check is quick and safe to call
 * on the main thread.
 */
public final class BackgroundPermissions {

    private static final String TAG = "AGAP/Background";

    private BackgroundPermissions() { }

    public static boolean notificationsAllowed(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Android 14+ lets the user turn full-screen alerts off per app; before that they are always allowed. */
    public static boolean fullScreenAllowed(Context context) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        return nm != null && nm.canUseFullScreenIntent();
    }

    public static boolean overlayAllowed(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public static boolean batteryUnrestricted(Context context) {
        PowerManager pm = context.getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    // ---- Opening the phone's settings ----

    public static void openNotificationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        open(context, intent);
    }

    public static void openFullScreenSettings(Context context) {
        if (Build.VERSION.SDK_INT < 34) return;
        open(context, new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context)));
    }

    public static void openOverlaySettings(Context context) {
        open(context, new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context)));
    }

    /** Android's own "Let AGAP run in the background?" question, or its battery list if a phone hides it. */
    @android.annotation.SuppressLint("BatteryLife") // sideloaded research app; the user is asked, never forced
    public static void askBatteryUnrestricted(Context context) {
        Intent ask = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context));
        if (!open(context, ask)) open(context, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
    }

    public static void openAppSettings(Context context) {
        open(context, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)));
    }

    private static Uri packageUri(Context context) {
        return Uri.fromParts("package", context.getPackageName(), null);
    }

    private static boolean open(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "cannot open " + intent.getAction() + ": " + e.getMessage());
            // Some phones hide the exact page: show AGAP's own app page instead.
            if (!Settings.ACTION_APPLICATION_DETAILS_SETTINGS.equals(intent.getAction())) {
                try {
                    context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)));
                } catch (RuntimeException ignored) {
                    // nothing more to try
                }
            }
            return false;
        }
    }
}
