package ph.agap.app.ui.info;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.ui.BaseActivity;
import ph.agap.app.ui.alerts.AlertLooks;
import ph.agap.app.ui.alerts.VibrationBarsView;
import ph.agap.app.vibration.AlertVibrator;

/**
 * Vibration Patterns guide (contract 1.8): each category's pattern in words and as bars, with a
 * Try button to feel it. Uses the same pattern data as the alerts (VibrationPatterns) and, in
 * Phase 7, the watch.
 */
public class VibrationPatternsActivity extends BaseActivity {

    private AlertVibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_page);
        setUpScreen(true, true);
        vibrator = new AlertVibrator(this);
        ((TextView) findViewById(R.id.page_intro)).setText(
                vibrator.canVibrate() ? R.string.vibration_intro : R.string.vibration_intro_no_vibrator);

        ViewGroup cards = findViewById(R.id.page_cards);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AlertCategory category : AlertCategory.values()) {
            View card = inflater.inflate(R.layout.item_vibration, cards, false);
            bind(card, category);
            cards.addView(card);
        }
        findViewById(R.id.page_note).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vibration may have been switched in Settings meanwhile.
        ((TextView) findViewById(R.id.page_note_text)).setText(
                settings.isVibrationOn() ? R.string.vibration_note_on : R.string.vibration_note_off);
    }

    @Override
    protected void onStop() {
        vibrator.stop();
        super.onStop();
    }

    private void bind(View card, AlertCategory category) {
        int ink = color(AlertLooks.inkAttr(category));
        MaterialCardView view = (MaterialCardView) card;
        view.setCardBackgroundColor(color(AlertLooks.rowAttr(category)));
        view.setStrokeColor(ink);
        ImageView shape = card.findViewById(R.id.vibration_shape);
        shape.setImageResource(AlertLooks.badgeShape(category));
        shape.setImageTintList(ColorStateList.valueOf(ink));
        ImageView icon = card.findViewById(R.id.vibration_icon);
        icon.setImageResource(AlertLooks.categoryIcon(category));
        icon.setImageTintList(ColorStateList.valueOf(color(R.attr.agapSurface)));
        ((TextView) card.findViewById(R.id.vibration_name)).setText(category.displayName);
        TextView words = card.findViewById(R.id.vibration_words);
        words.setText(patternInWords(category));
        words.setTextColor(ink);
        ((VibrationBarsView) card.findViewById(R.id.vibration_bars)).setPattern(category, ink);

        MaterialButton tryButton = card.findViewById(R.id.vibration_try);
        tryButton.setContentDescription(getString(R.string.vibration_try_description, category.displayName));
        tryButton.setEnabled(vibrator.canVibrate());
        tryButton.setOnClickListener(v -> vibrator.preview(category));
    }

    @StringRes
    private static int patternInWords(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.string.vibration_words_emergency;
            case TRAFFIC: return R.string.vibration_words_traffic;
            default: return R.string.vibration_words_household;
        }
    }

    private int color(@AttrRes int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
