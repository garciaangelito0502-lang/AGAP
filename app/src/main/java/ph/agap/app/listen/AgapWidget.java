package ph.agap.app.listen;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.history.AlertRecord;
import ph.agap.app.history.HistoryRepository;
import ph.agap.app.ui.MainActivity;
import ph.agap.app.ui.alerts.AlertLooks;

/**
 * The home-screen widget (Phase 11): whether AGAP is listening, the newest alert in the Event
 * History, a button that opens AGAP and, while listening, a Stop button. It cannot start listening:
 * Android lets AGAP open the microphone only from its own screen, so "Open AGAP" goes there. AGAP
 * updates it when something changes; there are no timed updates.
 */
public final class AgapWidget extends AppWidgetProvider {

    private static final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "agap-widget"));

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        Listening now = Listening.read(app);
        io.execute(() -> {
            try {
                draw(app, now, HistoryRepository.get(app).newestNow());
            } finally {
                pending.finish();
            }
        });
    }

    /** What is listening, read on the main thread (the sessions are main-thread only). */
    private static final class Listening {
        final boolean alerts;
        final boolean captions;
        final String language;

        private Listening(boolean alerts, boolean captions, String language) {
            this.alerts = alerts;
            this.captions = captions;
            this.language = language;
        }

        static Listening read(Context app) {
            CaptionsSession c = CaptionsSession.get(app);
            return new Listening(AlertsSession.get(app).isRunning(), c.isListeningForReal(), c.languageName());
        }
    }

    /** Redraws every AGAP widget, if the user has placed any. Main thread. */
    public static void refresh(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        if (manager == null || manager.getAppWidgetIds(new ComponentName(app, AgapWidget.class)).length == 0) return;
        Listening now = Listening.read(app);
        io.execute(() -> draw(app, now, HistoryRepository.get(app).newestNow()));
    }

    private static void draw(Context app, Listening now, @Nullable AlertRecord newest) {
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        int[] ids = manager.getAppWidgetIds(new ComponentName(app, AgapWidget.class));
        if (ids.length == 0) return;
        manager.updateAppWidget(ids, views(app, now.alerts, now.captions, now.language, newest));
    }

    /** The widget's content; public for the instrumented widget test. */
    public static RemoteViews views(Context app, boolean alerts, boolean captions, String language,
                                    @Nullable AlertRecord newest) {
        RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.widget_agap);
        String status = alerts ? app.getString(R.string.widget_listening_alerts)
                : captions ? app.getString(R.string.widget_listening_captions, language)
                : app.getString(R.string.widget_not_listening);
        views.setTextViewText(R.id.widget_status, status);
        views.setViewVisibility(R.id.widget_status_dot, alerts || captions ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_stop, alerts || captions ? View.VISIBLE : View.GONE);

        if (newest == null) {
            views.setTextViewText(R.id.widget_last_label, app.getString(R.string.widget_no_alerts));
            views.setTextViewText(R.id.widget_last_details, app.getString(R.string.widget_no_alerts_text));
            views.setImageViewResource(R.id.widget_last_icon, R.drawable.ic_alerts);
            views.setInt(R.id.widget_last_icon, "setColorFilter", ContextCompat.getColor(app, R.color.brand));
            views.setInt(R.id.widget_last_row, "setBackgroundResource", R.drawable.bg_widget_row);
        } else {
            AlertCategory category = AlertCategory.fromDisplayName(newest.category);
            views.setTextViewText(R.id.widget_last_label, app.getString(R.string.widget_last_alert, newest.label));
            views.setTextViewText(R.id.widget_last_details, app.getString(R.string.widget_last_details,
                    newest.category, time(app, newest.detectedAtMillis)));
            views.setImageViewResource(R.id.widget_last_icon, AlertLooks.icon(newest.label, category));
            views.setInt(R.id.widget_last_icon, "setColorFilter", ContextCompat.getColor(app, ink(category)));
            views.setInt(R.id.widget_last_row, "setBackgroundResource", row(category));
        }

        Intent open = new Intent(app, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TAB, captions ? MainActivity.TAB_CAPTIONS : MainActivity.TAB_ALERTS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        views.setOnClickPendingIntent(R.id.widget_open, PendingIntent.getActivity(app, 2, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        Intent history = new Intent(app, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_HISTORY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        views.setOnClickPendingIntent(R.id.widget_last_row, PendingIntent.getActivity(app, 3, history,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        views.setOnClickPendingIntent(R.id.widget_stop, StopReceiver.pendingIntent(app));
        return views;
    }

    private static int ink(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.color.emergency_ink;
            case TRAFFIC: return R.color.traffic_ink;
            default: return R.color.household_ink;
        }
    }

    private static int row(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.drawable.bg_widget_row_emergency;
            case TRAFFIC: return R.drawable.bg_widget_row_traffic;
            default: return R.drawable.bg_widget_row_household;
        }
    }

    private static String time(Context context, long millis) {
        String pattern = DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }
}
