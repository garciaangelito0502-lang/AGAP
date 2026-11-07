package ph.agap.wear;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertIcons;
import ph.agap.app.vibration.VibrationPatterns;
import ph.agap.app.watch.WatchMessages;

/**
 * An alert that arrives while the AGAP watch screen is not in front becomes a notification. It
 * vibrates in the category's pattern (one channel per category): Wear OS only pops up a
 * notification that vibrates or makes a sound. Where full-screen alerts are allowed, it also opens
 * the AGAP watch screen, which shows the alert in full.
 */
final class AlertNotifier {

    private static final String TAG = "AGAP/Watch";
    private static final String QUIET_CHANNEL = "alerts_quiet";
    private static final int ID = 1;

    private AlertNotifier() { }

    static void show(Context context, WatchMessages.Alert alert) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "notifications not allowed; the alert waits for the AGAP watch screen");
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        String channel = channel(context, manager, alert.category, alert.vibrate);
        if (Build.VERSION.SDK_INT >= 34 && !manager.canUseFullScreenIntent()) {
            Log.i(TAG, "full-screen alerts not allowed; showing a notification only");
        }
        // WatchActivity is singleTop with its own task affinity, so no task flags (Wear recents).
        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, WatchActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = alert.demo ? context.getString(R.string.watch_sample_title, alert.label) : alert.label;
        Notification notification = new Notification.Builder(context, channel)
                .setSmallIcon(AlertIcons.icon(alert.label, alert.category))
                .setContentTitle(title)
                .setContentText(alert.category.displayName)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .setAutoCancel(true)
                .build();
        manager.notify(ID, notification);
    }

    static void cancel(Context context) {
        context.getSystemService(NotificationManager.class).cancel(ID);
    }

    /** The category's channel with its vibration pattern, or a quiet one when vibration is off on the phone. */
    private static String channel(Context context, NotificationManager manager, AlertCategory category,
                                  boolean vibrate) {
        String id = vibrate ? "alerts_" + category.name().toLowerCase(Locale.ROOT) : QUIET_CHANNEL;
        if (manager.getNotificationChannel(id) != null) return id;
        NotificationChannel channel;
        if (vibrate) {
            channel = new NotificationChannel(id, category.displayName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setVibrationPattern(VibrationPatterns.timings(category));
        } else {
            channel = new NotificationChannel(id, context.getString(R.string.watch_channel_quiet),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(false);
        }
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
        return id;
    }
}
