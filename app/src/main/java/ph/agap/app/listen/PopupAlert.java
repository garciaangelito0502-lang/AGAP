package ph.agap.app.listen;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.history.ShownVia;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.MainActivity;
import ph.agap.app.ui.alerts.AlertLooks;

/**
 * An alert card over whatever app the user has open (Phase 11; needs "Display over other apps"):
 * icon, label, category, confidence and time in the category's strong colour, the same look rules
 * as the full-screen alert (no flashing, colour never the only cue). It stays until dismissed or
 * replaced by a newer alert. The shown time is when its first frame reaches the display. Main thread.
 */
final class PopupAlert {

    private static final String TAG = "AGAP/Alerts";
    private static final long SHOWN_FALLBACK_MS = 1000;

    @Nullable private static View shown;
    private static final Handler main = new Handler(Looper.getMainLooper());

    private PopupAlert() { }

    static void show(Context app, long ledgerId, AlertEvent event) {
        hide(app);
        WindowManager wm = app.getSystemService(WindowManager.class);
        if (wm == null) return;
        Context ui = uiContext(app);
        View card = LayoutInflater.from(ui).inflate(R.layout.popup_alert, null);
        bind(ui, card, event);
        card.findViewById(R.id.popup_dismiss).setOnClickListener(v -> {
            AlertPresenter.stopVibration(app);
            hide(app);
        });
        card.findViewById(R.id.popup_open).setOnClickListener(v -> {
            AlertPresenter.stopVibration(app);
            hide(app);
            app.startActivity(new Intent(app, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_ALERTS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        params.y = Math.round(36 * app.getResources().getDisplayMetrics().density);
        params.setTitle("AGAP alert");

        measureShown(app, card, ledgerId);
        try {
            wm.addView(card, params);
            shown = card;
        } catch (RuntimeException e) {
            // "Display over other apps" was turned off a moment ago: the notification still shows it.
            Log.w(TAG, "popup not shown: " + e.getMessage());
            AlertLedger.record(app, ledgerId, System.currentTimeMillis(), ShownVia.NOTIFICATION);
        }
    }

    static void hide(Context app) {
        View view = shown;
        shown = null;
        if (view == null) return;
        WindowManager wm = app.getSystemService(WindowManager.class);
        try {
            if (wm != null) wm.removeView(view);
        } catch (RuntimeException ignored) {
            // already gone
        }
    }

    private static void measureShown(Context app, View card, long ledgerId) {
        long[] drawnAt = {0};
        card.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver observer = card.getViewTreeObserver();
                observer.removeOnPreDrawListener(this);
                drawnAt[0] = System.currentTimeMillis();
                observer.registerFrameCommitCallback(
                        () -> AlertLedger.record(app, ledgerId, System.currentTimeMillis(), ShownVia.POPUP));
                return true;
            }
        });
        main.postDelayed(() -> AlertLedger.record(app, ledgerId,
                drawnAt[0] > 0 ? drawnAt[0] : System.currentTimeMillis(), ShownVia.POPUP), SHOWN_FALLBACK_MS);
    }

    private static void bind(Context ui, View card, AlertEvent event) {
        AlertCategory category = event.category();
        int strong = ContextCompat.getColor(ui, AlertLooks.strong(category));
        int on = ContextCompat.getColor(ui, AlertLooks.onStrong(category));
        float density = ui.getResources().getDisplayMetrics().density;

        GradientDrawable fill = new GradientDrawable();
        fill.setColor(strong);
        fill.setCornerRadius(20 * density);
        fill.setStroke(Math.round(3 * density), on);
        card.findViewById(R.id.popup_card).setBackground(fill);

        ImageView shape = card.findViewById(R.id.popup_badge_shape);
        shape.setImageResource(AlertLooks.badgeShape(category));
        shape.setImageTintList(ColorStateList.valueOf(on));
        ImageView icon = card.findViewById(R.id.popup_badge_icon);
        icon.setImageResource(AlertLooks.icon(event.label(), category));
        icon.setImageTintList(ColorStateList.valueOf(strong));

        TextView label = card.findViewById(R.id.popup_label);
        label.setText(event.label());
        label.setTextColor(on);
        TextView details = card.findViewById(R.id.popup_details);
        details.setText(ui.getString(R.string.notif_alert_text, category.displayName,
                Math.round(event.confidence * 100f), time(ui, event.detectedAtMillis)));
        details.setTextColor(on);

        MaterialButton dismiss = card.findViewById(R.id.popup_dismiss);
        dismiss.setBackgroundTintList(ColorStateList.valueOf(on));
        dismiss.setTextColor(strong);
        MaterialButton open = card.findViewById(R.id.popup_open);
        open.setTextColor(on);
        open.setStrokeColor(ColorStateList.valueOf(on));
        card.setContentDescription(ui.getString(R.string.popup_description, event.label(), category.displayName));
    }

    /**
     * The alert look (strong colours in both display modes) at the user's text size. AGAP's own
     * theme goes underneath, as it does under the in-app alert, because the buttons' styles use
     * AGAP's theme colours; without it the popup could not be built (found on the phone, K-033).
     */
    private static Context uiContext(Context app) {
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.fontScale = config.fontScale * SettingsStore.from(app).getTextScale();
        Context base = new ContextThemeWrapper(app.createConfigurationContext(config), R.style.Theme_AGAP);
        return new ContextThemeWrapper(base, R.style.Theme_AGAP_AlertOverlay);
    }

    private static String time(Context context, long millis) {
        String pattern = DateFormat.is24HourFormat(context) ? "HH:mm:ss" : "h:mm:ss a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }
}
