package ph.agap.app.history;

/** Where shown alerts are saved. The real one is HistoryRepository; tests use a fake. */
public interface HistoryStore {

    /** Saves the alert. May return before it is written (the real store writes on a background thread). */
    void save(AlertRecord record);
}
