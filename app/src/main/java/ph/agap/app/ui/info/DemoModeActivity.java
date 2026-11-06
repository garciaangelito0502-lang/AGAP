package ph.agap.app.ui.info;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.IOException;
import java.io.InputStream;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.alerts.AlertType;
import ph.agap.app.alerts.AnnexA;
import ph.agap.app.demo.DemoMode;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.BaseActivity;
import ph.agap.app.ui.MainActivity;
import ph.agap.app.ui.alerts.AlertLooks;
import ph.agap.app.ui.alerts.AlertOverlayFragment;

/**
 * Demonstration Mode (contract 1.9): a presentation backup for when live detection is unreliable.
 * While it is on, a striped banner is on every screen, Live Captions plays a sample script and
 * Sound Alerts cannot start, so the microphone is never opened. Any of the 13 alerts can be shown
 * from here; they look and vibrate like real ones but are never saved.
 */
public class DemoModeActivity extends BaseActivity {

    /** The confidence shown on sample alerts. */
    private static final float SAMPLE_CONFIDENCE = 0.92f;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        setUpScreen(true, true);

        findViewById(R.id.demo_turn_on).setOnClickListener(v -> DemoMode.setOn(true));
        findViewById(R.id.demo_exit).setOnClickListener(v -> DemoMode.setOn(false));
        findViewById(R.id.demo_captions).setOnClickListener(v -> startActivity(
                new Intent(this, MainActivity.class)
                        .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_CAPTIONS)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        addSampleAlerts(findViewById(R.id.demo_chips));
        showState();
    }

    @Override
    protected void onDemoModeChanged() {
        showState();
    }

    private void showState() {
        boolean on = DemoMode.isOn();
        findViewById(R.id.demo_off).setVisibility(on ? View.GONE : View.VISIBLE);
        findViewById(R.id.demo_on).setVisibility(on ? View.VISIBLE : View.GONE);
    }

    /** One chip per Annex A alert, in the category's tint, ink and badge shape. */
    private void addSampleAlerts(ChipGroup group) {
        AnnexA annex;
        try (InputStream in = getAssets().open(AnnexA.ASSET)) {
            annex = AnnexA.parse(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + AnnexA.ASSET, e);
        }
        int text = color(R.attr.agapText);
        for (AlertType type : annex.alerts()) {
            int ink = color(AlertLooks.inkAttr(type.category));
            Chip chip = new Chip(this);
            chip.setText(type.label);
            chip.setTextColor(text);
            chip.setChipBackgroundColor(ColorStateList.valueOf(color(AlertLooks.rowAttr(type.category))));
            chip.setChipStrokeColor(ColorStateList.valueOf(ink));
            chip.setChipStrokeWidth(2 * getResources().getDisplayMetrics().density);
            chip.setChipIconResource(AlertLooks.badgeShape(type.category));
            chip.setChipIconTint(ColorStateList.valueOf(ink));
            chip.setChipMinHeight(48 * getResources().getDisplayMetrics().density);
            chip.setContentDescription(getString(R.string.demo_chip_description, type.label,
                    type.category.displayName));
            chip.setOnClickListener(v -> showSample(type));
            group.addView(chip);
        }
    }

    private void showSample(AlertType type) {
        if (!DemoMode.isOn()) return;
        AlertEvent sample = new AlertEvent(type, type.yamnetClasses.get(0), SAMPLE_CONFIDENCE,
                System.currentTimeMillis());
        AlertOverlayFragment.show(this, sample, SettingsStore.from(this).getAlertThresholdPercent(), true);
    }

    private int color(@AttrRes int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
