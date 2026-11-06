package ph.agap.app.ui.alerts;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.history.AlertRecorder;
import ph.agap.app.history.HistoryRepository;
import ph.agap.app.ui.DemoBanner;
import ph.agap.app.vibration.AlertVibrator;

/**
 * The full-screen alert (contract 1.5): icon, label, category, confidence and time, in the
 * category's colour and badge shape, with the category's vibration. It is a dialog over the app,
 * not a new screen, so Sound Alerts keeps listening behind it; a newer alert replaces the one shown.
 *
 * The "alert shown" time is taken when Android reports that the frame showing the alert has been
 * handed to the display (frame-commit callback), so the response time the group reads from the
 * Event History is honest. Real alerts are saved then; Demonstration Mode alerts never are.
 */
public class AlertOverlayFragment extends DialogFragment {

    private static final String TAG = "agap-alert";
    private static final String LOG = "AGAP/Alerts";
    /** If the frame-commit callback never comes, the draw time is used after this long. */
    private static final long SHOWN_FALLBACK_MS = 1000;

    /** One alert on this screen, and whether its shown time has been recorded. */
    private static final class Shown {
        final AlertEvent event;
        final int thresholdPercent;
        final boolean demo;
        long drawnAt;
        boolean recorded;

        Shown(AlertEvent event, int thresholdPercent, boolean demo) {
            this.event = event;
            this.thresholdPercent = thresholdPercent;
            this.demo = demo;
        }
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    @Nullable private Shown first;
    @Nullable private Shown current;
    private int countThisScreen;
    private AlertRecorder recorder;
    private AlertVibrator vibrator;
    @Nullable private ValueAnimator pulse;

    /**
     * Shows an alert, or replaces the one on screen. Call on the main thread from a started screen.
     *
     * @param thresholdPercent the alert threshold in use, saved with the alert
     * @param demo             true for a Demonstration Mode alert: shown and vibrated, never saved
     */
    public static void show(FragmentActivity activity, AlertEvent event, int thresholdPercent, boolean demo) {
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.isStateSaved()) {
            Log.w(LOG, "screen not in view; alert not shown: " + event.label());
            return;
        }
        Fragment existing = fm.findFragmentByTag(TAG);
        if (existing instanceof AlertOverlayFragment && existing.getView() != null) {
            ((AlertOverlayFragment) existing).present(new Shown(event, thresholdPercent, demo));
            return;
        }
        AlertOverlayFragment fragment = new AlertOverlayFragment();
        fragment.first = new Shown(event, thresholdPercent, demo);
        // showNow: the view exists when this returns, so an alert right after this one finds it.
        fragment.showNow(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Theme_AGAP_AlertOverlay);
        recorder = new AlertRecorder(HistoryRepository.get(requireContext()));
        vibrator = new AlertVibrator(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_alert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Restored by Android after the app was rebuilt: that alert is old, and was recorded already.
        if (savedInstanceState != null || first == null) {
            dismissAllowingStateLoss();
            return;
        }
        view.findViewById(R.id.alert_dismiss).setOnClickListener(v -> dismiss());
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        present(first);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            styleSystemBars();
        }
    }

    @Override
    public void onStop() {
        // The alert is no longer in view (Home pressed, screen off): stop buzzing.
        vibrator.stop();
        super.onStop();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        vibrator.stop();
        recordNow(System.currentTimeMillis(), "dismissed before drawn");
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        main.removeCallbacksAndMessages(null);
        if (pulse != null) pulse.cancel();
        super.onDestroyView();
    }

    // ---- Showing one alert ----

    private void present(Shown shown) {
        // An alert replaced before its frame was confirmed: it was on its way to the screen now.
        recordNow(System.currentTimeMillis(), "replaced before drawn");
        current = shown;
        countThisScreen++;
        bind(shown);
        vibrator.play(shown.event.category());
        measureShown(shown);
    }

    private void bind(Shown shown) {
        View root = requireView();
        AlertCategory category = shown.event.category();
        int strong = ContextCompat.getColor(requireContext(), AlertLooks.strong(category));
        int on = ContextCompat.getColor(requireContext(), AlertLooks.onStrong(category));
        int tile = ContextCompat.getColor(requireContext(), AlertLooks.tile(category));

        root.setBackgroundColor(strong);
        DemoBanner.update(root.findViewById(R.id.demo_banner));
        styleSystemBars();

        TextView categoryView = root.findViewById(R.id.alert_category);
        categoryView.setText(category.displayName);
        categoryView.setTextColor(on);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(28 * getResources().getDisplayMetrics().density);
        pill.setStroke(Math.round(3 * getResources().getDisplayMetrics().density), on);
        categoryView.setBackground(pill);

        ImageView shape = root.findViewById(R.id.alert_badge_shape);
        shape.setImageResource(AlertLooks.badgeShape(category));
        shape.setImageTintList(ColorStateList.valueOf(on));
        ImageView icon = root.findViewById(R.id.alert_badge_icon);
        icon.setImageResource(AlertLooks.icon(shown.event.label(), category));
        icon.setImageTintList(ColorStateList.valueOf(strong));
        startPulse(root.findViewById(R.id.alert_badge));

        TextView label = root.findViewById(R.id.alert_label);
        label.setText(shown.event.label());
        label.setTextColor(on);

        int percent = Math.round(shown.event.confidence * 100f);
        TextView confidence = root.findViewById(R.id.alert_confidence);
        confidence.setText(getString(R.string.alert_confidence, percent));
        confidence.setTextColor(on);
        LinearProgressIndicator bar = root.findViewById(R.id.alert_confidence_bar);
        bar.setIndicatorColor(on);
        bar.setTrackColor(ColorUtils.setAlphaComponent(on, 0x59));
        bar.setProgressCompat(percent, false);

        root.findViewById(R.id.alert_tile_time).setBackgroundTintList(ColorStateList.valueOf(tile));
        root.findViewById(R.id.alert_tile_vibration).setBackgroundTintList(ColorStateList.valueOf(tile));
        ((TextView) root.findViewById(R.id.alert_time_label)).setTextColor(on);
        TextView time = root.findViewById(R.id.alert_time);
        time.setText(formatTime(shown.event.detectedAtMillis));
        time.setTextColor(on);
        ((TextView) root.findViewById(R.id.alert_vibration_label)).setTextColor(on);
        VibrationBarsView bars = root.findViewById(R.id.alert_vibration_bars);
        TextView vibrationOff = root.findViewById(R.id.alert_vibration_off);
        boolean vibrates = vibrator.willVibrate();
        bars.setVisibility(vibrates ? View.VISIBLE : View.GONE);
        bars.setPattern(category, on);
        vibrationOff.setVisibility(vibrates ? View.GONE : View.VISIBLE);
        vibrationOff.setTextColor(on);

        TextView more = root.findViewById(R.id.alert_more);
        more.setVisibility(countThisScreen > 1 ? View.VISIBLE : View.GONE);
        more.setText(getResources().getQuantityString(shown.demo ? R.plurals.alert_more_demo : R.plurals.alert_more,
                countThisScreen, countThisScreen));
        more.setTextColor(on);

        MaterialButton dismiss = root.findViewById(R.id.alert_dismiss);
        dismiss.setBackgroundTintList(ColorStateList.valueOf(on));
        dismiss.setTextColor(strong);

        TextView footer = root.findViewById(R.id.alert_footer);
        footer.setText(shown.demo ? R.string.alert_footer_demo : R.string.alert_footer_saved);
        footer.setTextColor(on);
        footer.setCompoundDrawablesRelativeWithIntrinsicBounds(
                shown.demo ? R.drawable.ic_info : R.drawable.ic_history, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(footer, ColorStateList.valueOf(on));
    }

    /** Waits for the frame that shows this alert to reach the display, then records the time. */
    private void measureShown(Shown shown) {
        View root = requireView();
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver observer = root.getViewTreeObserver();
                observer.removeOnPreDrawListener(this);
                shown.drawnAt = System.currentTimeMillis();
                // Runs once, after this frame has been handed to the display (Android 10+).
                observer.registerFrameCommitCallback(() -> record(shown, System.currentTimeMillis(), "frame"));
                return true;
            }
        });
        root.invalidate();
        main.postDelayed(() -> {
            if (!shown.recorded) {
                record(shown, shown.drawnAt > 0 ? shown.drawnAt : System.currentTimeMillis(), "fallback");
            }
        }, SHOWN_FALLBACK_MS);
    }

    private void recordNow(long shownAt, String how) {
        Shown shown = current;
        if (shown != null && !shown.recorded) record(shown, shownAt, how);
    }

    private void record(Shown shown, long shownAtMillis, String how) {
        if (shown.recorded) return;
        shown.recorded = true;
        recorder.shown(shown.event, shownAtMillis, shown.thresholdPercent, shown.demo);
        Log.i(LOG, (shown.demo ? "demo alert (not saved) " : "alert ") + shown.event.label()
                + " shown " + (shownAtMillis - shown.event.detectedAtMillis) + " ms after detection (" + how + ")");
    }

    // ---- Look ----

    /** A slow pulse of the badge only: one per second, never a flash (design note). */
    private void startPulse(View badge) {
        if (pulse != null) return;
        pulse = ObjectAnimator.ofPropertyValuesHolder(badge,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.07f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.07f));
        pulse.setDuration(500);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.start();
    }

    /** Light status-bar icons on red, dark icons on orange and light blue. */
    private void styleSystemBars() {
        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window == null || current == null) return;
        boolean darkFill = current.event.category() == AlertCategory.EMERGENCY;
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(!darkFill);
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightNavigationBars(!darkFill);
    }

    private String formatTime(long millis) {
        String pattern = DateFormat.is24HourFormat(requireContext()) ? "HH:mm:ss" : "h:mm:ss a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }
}
