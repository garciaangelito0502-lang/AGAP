package ph.agap.app.ui.history;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.history.AlertRecord;
import ph.agap.app.history.HistoryRepository;
import ph.agap.app.ui.BaseActivity;
import ph.agap.app.ui.alerts.AlertLooks;

/**
 * One alert from the Event History: date, confidence, the time the sound was detected and the time
 * the alert was shown (both to the millisecond, same clock) and the response time between them,
 * which the research group uses as its response-time measure.
 */
public class AlertDetailActivity extends BaseActivity {

    public static final String EXTRA_ID = "alert_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail);
        setUpScreen(true, true);
        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        HistoryRepository.get(this).load(id, record -> {
            if (isFinishing() || isDestroyed()) return;
            if (record == null) {
                finish(); // cleared meanwhile
            } else {
                show(record);
            }
        });
    }

    private void show(AlertRecord record) {
        AlertCategory category = HistoryAdapter.categoryOf(record);
        int ink = color(AlertLooks.inkAttr(category));

        MaterialCardView header = findViewById(R.id.detail_header);
        header.setCardBackgroundColor(color(AlertLooks.rowAttr(category)));
        header.setStrokeColor(ink);
        ImageView shape = findViewById(R.id.detail_badge_shape);
        shape.setImageResource(AlertLooks.badgeShape(category));
        shape.setImageTintList(ColorStateList.valueOf(ink));
        ImageView icon = findViewById(R.id.detail_badge_icon);
        icon.setImageResource(AlertLooks.icon(record.label, category));
        icon.setImageTintList(ColorStateList.valueOf(color(R.attr.agapSurface)));
        ((TextView) findViewById(R.id.detail_label)).setText(record.label);
        TextView categoryView = findViewById(R.id.detail_category);
        categoryView.setText(record.category);
        categoryView.setTextColor(ink);

        LinearLayout rows = findViewById(R.id.detail_rows);
        rows.removeAllViews();
        addRow(rows, R.string.detail_date, HistoryFormat.date(record.detectedAtMillis), false);
        addRow(rows, R.string.detail_confidence,
                getString(R.string.detail_percent, Math.round(record.confidence * 100f)), false);
        addRow(rows, R.string.detail_detected, HistoryFormat.timeMillis(this, record.detectedAtMillis), false);
        addRow(rows, R.string.detail_shown, HistoryFormat.timeMillis(this, record.shownAtMillis), false);
        addRow(rows, R.string.detail_response,
                getString(R.string.detail_response_ms, record.responseTimeMillis()), true);
        addRow(rows, R.string.detail_heard_as, record.yamnetClass, false);
        addRow(rows, R.string.detail_threshold, getString(R.string.detail_percent, record.thresholdPercent), false);
    }

    /** One "name ... value" line; the response time is larger and in the accent colour. */
    private void addRow(LinearLayout rows, int name, String value, boolean highlight) {
        if (rows.getChildCount() > 0) {
            View line = new View(this);
            line.setBackgroundColor(color(R.attr.agapOutline));
            line.setAlpha(0.4f);
            rows.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Math.round(56 * getResources().getDisplayMetrics().density));
        TextView nameView = new TextView(this);
        nameView.setTextAppearance(R.style.TextAppearance_AGAP_Body);
        nameView.setText(name);
        TextView valueView = new TextView(this);
        valueView.setTextAppearance(highlight ? R.style.TextAppearance_AGAP_Title : R.style.TextAppearance_AGAP_Body);
        valueView.setTypeface(valueView.getTypeface(), Typeface.BOLD);
        valueView.setGravity(Gravity.END);
        valueView.setText(value);
        if (highlight) valueView.setTextColor(color(R.attr.agapAccent));
        row.addView(nameView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(valueView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f));
        rows.addView(row);
    }

    private int color(@AttrRes int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
