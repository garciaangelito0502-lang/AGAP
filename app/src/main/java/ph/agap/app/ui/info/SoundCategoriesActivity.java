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

import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.io.InputStream;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertType;
import ph.agap.app.alerts.AnnexA;
import ph.agap.app.ui.BaseActivity;
import ph.agap.app.ui.alerts.AlertLooks;

/**
 * Sound Categories (contract 1.8): the 3 categories and the 13 alerts, each with its icon, what it
 * means and the YAMNet sound classes it listens for. Built from the app's Annex A data, so the screen
 * always matches what Sound Alerts actually listens for.
 */
public class SoundCategoriesActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_page);
        setUpScreen(true, true);
        ((TextView) findViewById(R.id.page_intro)).setText(R.string.categories_intro);

        AnnexA annex;
        try (InputStream in = getAssets().open(AnnexA.ASSET)) {
            annex = AnnexA.parse(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + AnnexA.ASSET, e);
        }
        ViewGroup cards = findViewById(R.id.page_cards);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AlertCategory category : AlertCategory.values()) {
            View card = inflater.inflate(R.layout.item_category, cards, false);
            bindCategory(card, category);
            ViewGroup alerts = card.findViewById(R.id.category_alerts);
            for (AlertType type : annex.alerts()) {
                if (type.category != category) continue;
                View row = inflater.inflate(R.layout.item_category_alert, alerts, false);
                bindAlert(row, type);
                alerts.addView(row);
            }
            cards.addView(card);
        }

        View note = findViewById(R.id.page_note);
        note.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.page_note_text)).setText(R.string.categories_note);
    }

    private void bindCategory(View card, AlertCategory category) {
        int ink = color(AlertLooks.inkAttr(category));
        MaterialCardView view = (MaterialCardView) card;
        view.setCardBackgroundColor(color(AlertLooks.rowAttr(category)));
        view.setStrokeColor(ink);
        ImageView shape = card.findViewById(R.id.category_shape);
        shape.setImageResource(AlertLooks.badgeShape(category));
        shape.setImageTintList(ColorStateList.valueOf(ink));
        ImageView icon = card.findViewById(R.id.category_icon);
        icon.setImageResource(AlertLooks.categoryIcon(category));
        icon.setImageTintList(ColorStateList.valueOf(color(R.attr.agapSurface)));
        ((TextView) card.findViewById(R.id.category_name)).setText(category.displayName);
        TextView tagline = card.findViewById(R.id.category_tagline);
        tagline.setText(AlertLooks.tagline(category));
        tagline.setTextColor(ink);
    }

    private void bindAlert(View row, AlertType type) {
        int ink = color(AlertLooks.inkAttr(type.category));
        ImageView icon = row.findViewById(R.id.sound_icon);
        icon.setImageResource(AlertLooks.icon(type.label, type.category));
        icon.setImageTintList(ColorStateList.valueOf(ink));
        ((TextView) row.findViewById(R.id.sound_label)).setText(type.label);
        ((TextView) row.findViewById(R.id.sound_description)).setText(AlertLooks.description(type.label));
        ((TextView) row.findViewById(R.id.sound_classes)).setText(
                getString(R.string.categories_listens_for, String.join("; ", type.yamnetClasses)));
    }

    private int color(@AttrRes int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
