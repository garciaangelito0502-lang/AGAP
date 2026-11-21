package ph.agap.app.ui.settings;

import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Switch;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AttrRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import ph.agap.app.R;
import ph.agap.app.listen.AgapWidget;
import ph.agap.app.listen.AlertsSession;
import ph.agap.app.listen.BackgroundPermissions;
import ph.agap.app.listen.CaptionsSession;
import ph.agap.app.listen.ListeningService;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.BaseActivity;
import ph.agap.app.ui.info.AboutActivity;

/**
 * Text size, display mode, vibration and alert threshold (contract 1.7), and background listening
 * with what it needs from the phone (Phase 11). Saved at once.
 */
public class SettingsActivity extends BaseActivity {

    // Wait a moment after the last text-size change before rebuilding the screen,
    // so a TalkBack user stepping through sizes is not interrupted at every step.
    private static final long REBUILD_DELAY_MS = 700;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable rebuild = this::rebuildIfLookChanged;
    private boolean draggingTextSize;
    private boolean askedNotifications;

    private final ActivityResultLauncher<String> askNotifications = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                askedNotifications = true;
                showBackgroundNeeds();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setUpScreen(true, true);

        setUpTextSize();
        setUpDisplayMode();
        setUpVibration();
        setUpThreshold();
        setUpBackground();
        findViewById(R.id.row_about).setOnClickListener(
                v -> startActivity(new Intent(this, AboutActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The user may have changed a permission in the phone's settings meanwhile.
        showBackgroundNeeds();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(rebuild);
        super.onDestroy();
    }

    private void setUpTextSize() {
        String[] names = getResources().getStringArray(R.array.text_size_names);
        TextView sample = findViewById(R.id.text_size_sample);
        // Sample size at the app's "Normal" step, so the preview can be resized before the rebuild.
        float normalSizePx = sample.getTextSize() / settings.getTextScale();

        Slider slider = findViewById(R.id.slider_text_size);
        slider.setValue(settings.getTextSizeLevel());
        slider.setLabelFormatter(value -> names[(int) value]);
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (!fromUser) return;
            int level = (int) value;
            settings.setTextSizeLevel(level);
            sample.setTextSize(TypedValue.COMPLEX_UNIT_PX, normalSizePx * SettingsStore.textScaleFor(level));
            scheduleRebuild();
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider s) {
                draggingTextSize = true;
                handler.removeCallbacks(rebuild);
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider s) {
                draggingTextSize = false;
                scheduleRebuild();
            }
        });
    }

    private void scheduleRebuild() {
        handler.removeCallbacks(rebuild);
        if (!draggingTextSize) {
            handler.postDelayed(rebuild, REBUILD_DELAY_MS);
        }
    }

    private void setUpDisplayMode() {
        boolean highContrast = settings.isHighContrast();
        showMode(R.id.mode_standard, R.id.mode_standard_label, R.string.settings_mode_standard, !highContrast);
        showMode(R.id.mode_high_contrast, R.id.mode_high_contrast_label, R.string.settings_mode_high_contrast, highContrast);
        findViewById(R.id.mode_standard).setOnClickListener(v -> chooseHighContrast(false));
        findViewById(R.id.mode_high_contrast).setOnClickListener(v -> chooseHighContrast(true));
    }

    private void chooseHighContrast(boolean on) {
        if (on != settings.isHighContrast()) {
            settings.setHighContrast(on);
            rebuildIfLookChanged();
        }
    }

    /** Marks the chosen mode with a thick border, a check mark and the word "selected", not colour alone. */
    private void showMode(@IdRes int cardId, @IdRes int labelId, @StringRes int nameId, boolean selected) {
        MaterialCardView card = findViewById(cardId);
        TextView label = findViewById(labelId);
        String name = getString(nameId);

        label.setText(selected ? name + " " + getString(R.string.settings_selected_mark) : name);
        label.setTextColor(themeColor(selected ? R.attr.agapAccent : R.attr.agapText));
        card.setStrokeWidth(Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, selected ? 3 : 1, getResources().getDisplayMetrics())));
        card.setStrokeColor(themeColor(selected ? R.attr.agapAccent : R.attr.agapOutline));
        card.setSelected(selected);
        // Just the name: TalkBack adds "selected" from the card's selected state.
        card.setContentDescription(name);
    }

    private void setUpVibration() {
        MaterialSwitch vibration = findViewById(R.id.switch_vibration);
        vibration.setChecked(settings.isVibrationOn());
        View row = findViewById(R.id.row_vibration);
        vibration.setOnCheckedChangeListener((button, checked) -> {
            settings.setVibrationOn(checked);
            row.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        });
        switchRow(row, vibration);
    }

    /** The whole row toggles the switch, and screen readers meet it once, as a switch with its state. */
    private static void switchRow(View row, MaterialSwitch toggle) {
        row.setOnClickListener(v -> toggle.toggle());
        ViewCompat.setAccessibilityDelegate(row, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Switch.class.getName());
                info.setCheckable(true);
                info.setChecked(toggle.isChecked());
            }
        });
    }

    // ---- Background listening (Phase 11) ----

    private void setUpBackground() {
        MaterialSwitch toggle = findViewById(R.id.switch_background);
        toggle.setChecked(settings.isBackgroundListeningOn());
        View row = findViewById(R.id.row_background);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            settings.setBackgroundListeningOn(checked);
            row.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            if (checked) {
                // Notifications first: without them background alerts can only vibrate.
                if (Build.VERSION.SDK_INT >= 33 && !BackgroundPermissions.notificationsAllowed(this)) {
                    askedNotifications = true;
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
                ListeningService.update(this);
            } else {
                // As in 1.0: listening stops once its screen is not in view, and this screen is on top.
                AlertsSession.get(this).stop();
                CaptionsSession.get(this).stop();
            }
            showBackgroundNeeds();
        });
        switchRow(row, toggle);
    }

    /** One row per thing background listening needs, each with its state in words and an Allow button. */
    private void showBackgroundNeeds() {
        ViewGroup box = findViewById(R.id.background_permissions);
        boolean on = settings.isBackgroundListeningOn();
        box.setVisibility(on ? View.VISIBLE : View.GONE);
        box.removeAllViews();
        if (!on) return;
        need(box, R.string.bg_need_notifications, R.string.bg_need_notifications_text,
                BackgroundPermissions.notificationsAllowed(this), v -> {
                    if (Build.VERSION.SDK_INT >= 33 && !askedNotifications) {
                        askedNotifications = true;
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        BackgroundPermissions.openNotificationSettings(this);
                    }
                });
        if (Build.VERSION.SDK_INT >= 34) {
            need(box, R.string.bg_need_lock_screen, R.string.bg_need_lock_screen_text,
                    BackgroundPermissions.fullScreenAllowed(this),
                    v -> BackgroundPermissions.openFullScreenSettings(this));
        }
        need(box, R.string.bg_need_overlay, R.string.bg_need_overlay_text,
                BackgroundPermissions.overlayAllowed(this), v -> BackgroundPermissions.openOverlaySettings(this));
        need(box, R.string.bg_need_battery, R.string.bg_need_battery_text,
                BackgroundPermissions.batteryUnrestricted(this), v -> BackgroundPermissions.askBatteryUnrestricted(this));
        showWidgetRow(box);

        TextView maker = new TextView(this);
        maker.setText(R.string.bg_need_phone_maker);
        maker.setTextAppearance(R.style.TextAppearance_AGAP_BodySecondary);
        int pad = Math.round(8 * getResources().getDisplayMetrics().density);
        maker.setPadding(0, pad, 0, pad);
        box.addView(maker);
    }

    private void need(ViewGroup box, @StringRes int title, @StringRes int text, boolean allowed,
                      View.OnClickListener allow) {
        need(box, title, text, allowed, R.string.bg_need_allowed, R.string.bg_need_not_allowed,
                R.string.bg_need_allow, allow);
    }

    private void need(ViewGroup box, @StringRes int title, @StringRes int text, boolean done,
                      @StringRes int doneText, @StringRes int notDoneText, @StringRes int action,
                      @Nullable View.OnClickListener onAction) {
        View row = getLayoutInflater().inflate(R.layout.item_background_need, box, false);
        ((TextView) row.findViewById(R.id.need_title)).setText(title);
        ((TextView) row.findViewById(R.id.need_text)).setText(text);
        TextView state = row.findViewById(R.id.need_state);
        state.setText(done ? doneText : notDoneText);
        ImageView icon = row.findViewById(R.id.need_icon);
        icon.setImageResource(done ? R.drawable.ic_check : R.drawable.ic_info);
        MaterialButton button = row.findViewById(R.id.need_allow);
        button.setVisibility(done || onAction == null ? View.GONE : View.VISIBLE);
        button.setText(action);
        button.setOnClickListener(onAction);
        button.setContentDescription(getString(action) + ": " + getString(title));
        box.addView(row);
    }

    /**
     * The home-screen widget: Android's own "Add to home screen?" question where the launcher
     * supports it, otherwise where to find it in the launcher's widget list.
     */
    private void showWidgetRow(ViewGroup box) {
        AppWidgetManager widgets = getSystemService(AppWidgetManager.class);
        if (widgets == null) return;
        ComponentName widget = new ComponentName(this, AgapWidget.class);
        boolean placed = widgets.getAppWidgetIds(widget).length > 0;
        boolean canAsk = widgets.isRequestPinAppWidgetSupported();
        need(box, R.string.bg_widget, R.string.bg_widget_text, placed, R.string.bg_widget_placed,
                canAsk ? R.string.bg_widget_not_placed : R.string.bg_widget_by_hand, R.string.bg_widget_add,
                canAsk ? v -> widgets.requestPinAppWidget(widget, null, null) : null);
    }

    private void setUpThreshold() {
        TextView title = findViewById(R.id.threshold_title);
        Slider slider = findViewById(R.id.slider_threshold);
        int percent = settings.getAlertThresholdPercent();
        title.setText(getString(R.string.settings_threshold, percent));
        slider.setValue(percent);
        slider.setLabelFormatter(value -> getString(R.string.settings_threshold_value, (int) value));
        slider.addOnChangeListener((s, value, fromUser) -> {
            settings.setAlertThresholdPercent((int) value);
            title.setText(getString(R.string.settings_threshold, (int) value));
            // With background listening, Sound Alerts may be listening behind this screen.
            AlertsSession.get(this).thresholdChanged();
        });
    }

    private int themeColor(@AttrRes int attr) {
        return MaterialColors.getColor(this, attr, Color.BLACK);
    }
}
