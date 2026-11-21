package ph.agap.app.listen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.ui.MainActivity;
import ph.agap.app.ui.alerts.AlertLooks;

/**
 * AGAP's notifications (Phase 11): the "AGAP is listening" notice of ListeningService, and one
 * notification per alert while AGAP is not in view, on one channel per category. The alert channels
 * make no sound and do not vibrate: AGAP plays the category's own vibration pattern itself, so the
 * pattern is the same as in the app (contract 1.5).
 */
public final class AgapNotifications {

    private static final String TAG = "AGAP/Background";
    static final String CHANNEL_LISTENING = "listening";
    /** The notification-list copy of an alert already shown as a popup: never pops up itself. */
    static final String CHANNEL_LIST = "alerts_list";
    static final int LISTENING_ID = 1;
    private static int nextAlertId = 100;

    private AgapNotifications() { }

    static void createChannels(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel listening = new NotificationChannel(CHANNEL_LISTENING,
                context.getString(R.string.notif_channel_listening), NotificationManager.IMPORTANCE_LOW);
        listening.setDescription(context.getString(R.string.notif_channel_listening_text));
        listening.setShowBadge(false);
        // Caption words are someone's speech: not on the lock screen (D-058).
        listening.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        nm.createNotificationChannel(listening);
        NotificationChannel list = new NotificationChannel(CHANNEL_LIST,
                context.getString(R.string.notif_channel_list), NotificationManager.IMPORTANCE_LOW);
        list.setDescription(context.getString(R.string.notif_channel_list_text));
        list.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(list);
        for (AlertCategory category : AlertCategory.values()) {
            NotificationChannel channel = new NotificationChannel(channel(category),
                    context.getString(R.string.notif_channel_alerts, category.displayName),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notif_channel_alerts_text));
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(channel);
        }
    }

    static String channel(AlertCategory category) {
        return "alerts_" + category.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Posts one alert's notification and returns the time it was handed to Android.
     *
     * @param fullScreen also open the full alert over the lock screen (screen off or locked)
     * @param silent     only add it to the notification list, without popping up (a popup card
     *                   already shows the alert)
     */
    static long postAlert(Context context, long ledgerId, AlertEvent event, boolean fullScreen, boolean silent) {
        AlertCategory category = event.category();
        int percent = Math.round(event.confidence * 100f);
        String text = context.getString(R.string.notif_alert_text, category.displayName, percent,
                time(context, event.detectedAtMillis));
        PendingIntent open = PendingIntent.getActivity(context, (int) ledgerId,
                AlertActivity.intent(context, ledgerId),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        // Some phones (HyperOS) still pop up a "silent" notification on a high-importance channel,
        // over the popup card: the list copy goes to a low-importance channel instead.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, silent ? CHANNEL_LIST : channel(category))
                .setSmallIcon(R.drawable.ic_alerts)
                .setContentTitle(event.label())
                .setContentText(text)
                .setSubText(context.getString(R.string.app_name))
                .setColor(ContextCompat.getColor(context, AlertLooks.strong(category)))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(event.detectedAtMillis)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(open)
                .setSilent(silent);
        if (fullScreen) builder.setFullScreenIntent(open, true);
        return notify(context, nextAlertId++, builder.build());
    }

    /** The ListeningService notice: what is listening, a Stop button, tap to open AGAP. */
    static Notification listening(Context context) {
        AlertsSession alerts = AlertsSession.get(context);
        CaptionsSession captions = CaptionsSession.get(context);
        boolean captioning = captions.isListeningForReal();
        String title = captioning
                ? context.getString(R.string.notif_listening_captions, captions.languageName())
                : context.getString(R.string.notif_listening_alerts);
        String words = captioning ? captions.transcript().latest() : "";
        String text = captioning
                ? (words.isEmpty() ? context.getString(R.string.notif_listening_captions_waiting) : words)
                : context.getString(R.string.notif_listening_alerts_text);

        Intent openApp = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TAB, captioning ? MainActivity.TAB_CAPTIONS : MainActivity.TAB_ALERTS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(context, 1, openApp,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // On the lock screen: only that AGAP is listening, never the words (D-058).
        Notification publicVersion = new NotificationCompat.Builder(context, CHANNEL_LISTENING)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle(title)
                .build();
        return new NotificationCompat.Builder(context, CHANNEL_LISTENING)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setColor(ContextCompat.getColor(context, R.color.brand))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(open)
                .addAction(R.drawable.ic_stop, context.getString(R.string.notif_stop), StopReceiver.pendingIntent(context))
                .build();
    }

    /** Hands a notification to Android and returns the time; Android may refuse it (notifications off). */
    static long notify(Context context, int id, Notification notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification);
        } catch (SecurityException e) {
            Log.w(TAG, "notification not allowed: " + e.getMessage());
        }
        return System.currentTimeMillis();
    }

    static void cancel(Context context, int id) {
        NotificationManagerCompat.from(context).cancel(id);
    }

    @Nullable
    private static String time(Context context, long millis) {
        String pattern = DateFormat.is24HourFormat(context) ? "HH:mm:ss" : "h:mm:ss a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }
}
