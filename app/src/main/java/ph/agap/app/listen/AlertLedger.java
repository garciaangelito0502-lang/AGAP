package ph.agap.app.listen;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.history.AlertRecorder;
import ph.agap.app.history.HistoryRepository;
import ph.agap.app.history.ShownVia;

/**
 * The alerts raised while AGAP was not in view (Phase 11), so each one is saved to the Event History
 * exactly once, by whichever way of showing it came first: the lock-screen alert, the popup, or the
 * notification. A later way (for example the user tapping the notification) shows the same alert
 * again without saving it twice. Memory only; the newest 50 are kept. Main thread only.
 */
public final class AlertLedger {

    private static final String TAG = "AGAP/Alerts";
    private static final int KEEP = 50;

    /** One alert raised in the background. */
    public static final class Entry {
        public final AlertEvent event;
        public final int thresholdPercent;
        boolean recorded;

        Entry(AlertEvent event, int thresholdPercent) {
            this.event = event;
            this.thresholdPercent = thresholdPercent;
        }
    }

    private static long nextId = 1;
    private static final Map<Long, AlertLedger.Entry> entries = new LinkedHashMap<Long, AlertLedger.Entry>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, AlertLedger.Entry> eldest) {
            return size() > KEEP;
        }
    };

    private AlertLedger() { }

    static long add(AlertEvent event, int thresholdPercent) {
        long id = nextId++;
        entries.put(id, new Entry(event, thresholdPercent));
        return id;
    }

    /** The alert with this id, or null if it is unknown (AGAP was restarted since). */
    @Nullable
    public static Entry get(long id) {
        return entries.get(id);
    }

    /**
     * Saves the alert the first time it is shown, with that time and way.
     *
     * @return true if this call saved it, false if it was saved before or is unknown
     */
    public static boolean record(Context context, long id, long shownAtMillis, ShownVia via) {
        Entry entry = entries.get(id);
        if (entry == null || entry.recorded) return false;
        entry.recorded = true;
        new AlertRecorder(HistoryRepository.get(context)).shown(entry.event, shownAtMillis,
                entry.thresholdPercent, false, via);
        Log.i(TAG, "alert " + entry.event.label() + " shown " + (shownAtMillis - entry.event.detectedAtMillis)
                + " ms after detection (" + via.name() + ", AGAP not in view)");
        return true;
    }
}
