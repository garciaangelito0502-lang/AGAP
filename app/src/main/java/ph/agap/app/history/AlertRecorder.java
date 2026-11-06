package ph.agap.app.history;

import ph.agap.app.alerts.AlertEvent;

/**
 * Decides which shown alerts go into the Event History: every real alert, including false ones
 * (the research group counts those as false positives), and never a Demonstration Mode alert
 * (contract 1.9). Plain Java, so the rule is tested on the computer.
 */
public final class AlertRecorder {

    private final HistoryStore store;

    public AlertRecorder(HistoryStore store) {
        this.store = store;
    }

    /**
     * Called once per alert, when its screen has been shown.
     *
     * @return the record that was saved, or null for a demonstration alert
     */
    public AlertRecord shown(AlertEvent event, long shownAtMillis, int thresholdPercent, boolean demo) {
        if (demo) return null;
        AlertRecord record = AlertRecord.of(event, shownAtMillis, thresholdPercent);
        store.save(record);
        return record;
    }
}
