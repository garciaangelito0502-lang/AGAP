package ph.agap.app.ui.settings;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import ph.agap.app.R;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.BaseActivity;
import ph.agap.app.ui.info.AboutActivity;

/** Text size, display mode, vibration and alert threshold (contract 1.7). Saved at once. */
public class SettingsActivity extends BaseActivity {

    // Wait a moment after the last text-size change before rebuilding the screen,
    // so a TalkBack user stepping through sizes is not interrupted at every step.
    private static final long REBUILD_DELAY_MS = 700;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable rebuild = this::rebuildIfLookChanged;
    private boolean draggingTextSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setUpScreen(true, true);

        setUpTextSize();
        setUpDisplayMode();
        setUpVibration();
        setUpThreshold();
        findViewById(R.id.row_about).setOnClickListener(
                v -> startActivity(new Intent(this, AboutActivity.class)));
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
        card.setContentDescription(getString(
                selected ? R.string.settings_mode_selected : R.string.settings_mode_not_selected, name));
    }

    private void setUpVibration() {
        MaterialSwitch vibration = findViewById(R.id.switch_vibration);
        vibration.setChecked(settings.isVibrationOn());
        vibration.setOnCheckedChangeListener((button, checked) -> settings.setVibrationOn(checked));
        findViewById(R.id.row_vibration).setOnClickListener(v -> vibration.toggle());
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
        });
    }

    private int themeColor(@AttrRes int attr) {
        return MaterialColors.getColor(this, attr, Color.BLACK);
    }
}
