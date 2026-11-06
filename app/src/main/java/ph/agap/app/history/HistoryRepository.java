package ph.agap.app.history;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Reads and writes the Event History on one background thread, so the database never blocks the
 * screen and writes keep their order. Results and change notices come back on the main thread.
 */
public final class HistoryRepository implements HistoryStore {

    private static final String TAG = "AGAP/History";
    private static volatile HistoryRepository instance;

    private final AlertRecordDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "agap-history"));
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private HistoryRepository(AlertRecordDao dao) {
        this.dao = dao;
    }

    public static HistoryRepository get(Context context) {
        HistoryRepository repo = instance;
        if (repo == null) {
            synchronized (HistoryRepository.class) {
                repo = instance;
                if (repo == null) {
                    repo = new HistoryRepository(AgapDatabase.get(context).alerts());
                    instance = repo;
                }
            }
        }
        return repo;
    }

    /** Called on the main thread after any change (an alert saved, the history cleared). */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    @Override
    public void save(AlertRecord record) {
        io.execute(() -> {
            try {
                record.id = dao.insert(record);
                // Names and times only; no audio exists to log.
                Log.i(TAG, "saved #" + record.id + " " + record.label + " response "
                        + record.responseTimeMillis() + " ms");
                changed();
            } catch (RuntimeException e) {
                Log.e(TAG, "could not save " + record.label, e);
            }
        });
    }

    public void loadAll(Consumer<List<AlertRecord>> result) {
        io.execute(() -> {
            List<AlertRecord> all = dao.newestFirst();
            main.post(() -> result.accept(all));
        });
    }

    public void load(long id, Consumer<AlertRecord> result) {
        io.execute(() -> {
            @Nullable AlertRecord record = dao.byId(id);
            main.post(() -> result.accept(record));
        });
    }

    /** Deletes every alert, then reports how many were deleted. */
    public void clear(@Nullable Consumer<Integer> done) {
        io.execute(() -> {
            int deleted = dao.deleteAll();
            Log.i(TAG, "cleared " + deleted + " alerts");
            changed();
            if (done != null) main.post(() -> done.accept(deleted));
        });
    }

    private void changed() {
        main.post(() -> {
            for (Runnable listener : listeners) listener.run();
        });
    }
}
