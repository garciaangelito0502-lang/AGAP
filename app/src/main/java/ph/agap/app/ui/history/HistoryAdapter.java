package ph.agap.app.ui.history;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.history.AlertRecord;
import ph.agap.app.ui.alerts.AlertLooks;

/** The Event History list: a hint line, then alerts grouped under day headings, newest first. */
final class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int HEADER = 0;
    private static final int ALERT = 1;

    /** A list item: the hint line, a day heading (text), or an alert (record). */
    private static final class Item {
        final String text;
        final AlertRecord record;
        final boolean hint;

        Item(String text, AlertRecord record, boolean hint) {
            this.text = text;
            this.record = record;
            this.hint = hint;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final LongConsumer onOpen;

    HistoryAdapter(LongConsumer onOpen) {
        this.onOpen = onOpen;
    }

    // The whole list is rebuilt after each change (a save or a clear); it stays small.
    @SuppressLint("NotifyDataSetChanged")
    void submit(Context context, List<AlertRecord> newestFirst) {
        items.clear();
        if (!newestFirst.isEmpty()) {
            items.add(new Item(context.getResources().getQuantityString(
                    R.plurals.history_hint, newestFirst.size(), newestFirst.size()), null, true));
        }
        long now = System.currentTimeMillis();
        long lastDay = Long.MIN_VALUE;
        for (AlertRecord record : newestFirst) {
            if (lastDay == Long.MIN_VALUE || !HistoryFormat.sameDay(lastDay, record.detectedAtMillis)) {
                items.add(new Item(HistoryFormat.dayHeading(context, record.detectedAtMillis, now), null, false));
                lastDay = record.detectedAtMillis;
            }
            items.add(new Item(null, record, false));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).record == null ? HEADER : ALERT;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HEADER) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_history_header, parent, false)) { };
        }
        return new AlertHolder(inflater.inflate(R.layout.item_history_alert, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        if (item.record == null) {
            TextView text = (TextView) holder.itemView;
            // Day headings in small capitals; the hint line in plain secondary text.
            text.setTextAppearance(item.hint ? R.style.TextAppearance_AGAP_BodySecondary
                    : R.style.TextAppearance_AGAP_SectionLabel);
            text.setAllCaps(!item.hint);
            text.setAccessibilityHeading(!item.hint);
            text.setText(item.text);
        } else {
            ((AlertHolder) holder).bind(item.record);
        }
    }

    private final class AlertHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView shape;
        private final ImageView icon;
        private final TextView label;
        private final TextView details;
        private final TextView time;

        AlertHolder(View view) {
            super(view);
            card = view.findViewById(R.id.history_card);
            shape = view.findViewById(R.id.history_badge_shape);
            icon = view.findViewById(R.id.history_badge_icon);
            label = view.findViewById(R.id.history_label);
            details = view.findViewById(R.id.history_details);
            time = view.findViewById(R.id.history_time);
        }

        void bind(AlertRecord record) {
            Context context = itemView.getContext();
            AlertCategory category = categoryOf(record);
            int ink = color(context, AlertLooks.inkAttr(category));
            card.setCardBackgroundColor(color(context, AlertLooks.rowAttr(category)));
            card.setStrokeColor(ink);
            shape.setImageResource(AlertLooks.badgeShape(category));
            shape.setImageTintList(ColorStateList.valueOf(ink));
            icon.setImageResource(AlertLooks.icon(record.label, category));
            icon.setImageTintList(ColorStateList.valueOf(color(context, R.attr.agapSurface)));
            label.setText(record.label);
            label.setTextColor(color(context, R.attr.agapText));
            int percent = Math.round(record.confidence * 100f);
            details.setText(context.getString(R.string.history_row_details, record.category, percent));
            details.setTextColor(ink);
            time.setText(HistoryFormat.time(context, record.detectedAtMillis));
            card.setContentDescription(context.getString(R.string.history_row_description, record.label,
                    record.category, percent, time.getText()));
            card.setOnClickListener(v -> onOpen.accept(record.id));
        }
    }

    static AlertCategory categoryOf(AlertRecord record) {
        try {
            return AlertCategory.fromDisplayName(record.category);
        } catch (IllegalArgumentException e) {
            return AlertCategory.HOUSEHOLD;
        }
    }

    private static int color(Context context, @AttrRes int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
