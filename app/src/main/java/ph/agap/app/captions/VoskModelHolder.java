package ph.agap.app.captions;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import org.vosk.Model;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * Keeps the loaded Vosk model in memory between sessions. Loading takes seconds and the Filipino
 * model needs roughly 1 GB of RAM, so it is loaded once on first Start and freed when it is not in
 * use and the app leaves the screen or the user picks English.
 *
 * Only one model is kept. If a different model is asked for while the kept one is still in use
 * (a session that is ending while the next one, in the other language, starts loading), the new one
 * is handed out without being kept, and closed when its session ends. A caller therefore always gets
 * the model it asked for.
 *
 * A model is never closed while LOCK is held, and never on the main thread (Phase 8, ANR audit):
 * freeing a gigabyte takes time, and the main thread asks for the lock in releaseIfIdle().
 */
final class VoskModelHolder {

    private static final String TAG = "AGAP/Captions";
    private static final Object LOCK = new Object();
    /** Held for a whole acquire, so only one model load runs at a time. Worker threads only. */
    private static final Object LOAD = new Object();
    private static final ExecutorService CLOSER =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "agap-model-free"));

    @Nullable private static Model model;
    @Nullable private static String loadedPath;
    private static int users;

    private VoskModelHolder() { }

    /**
     * Returns the model at {@code path} (loading it if needed) and counts one use, or null if the
     * session no longer wants it. Call off the main thread.
     *
     * One acquire runs at a time (LOAD). Without that, quick Start/Stop taps while the model loads
     * started a load per tap: 11 loads at once took AGAP to 5.3 GB on my phone (Phase 8, K-029). Now
     * a later Start waits, then shares the model the first one loaded; a session stopped meanwhile
     * ({@code wanted} false) does not load at all. The main thread never takes LOAD.
     */
    @Nullable
    static Model acquire(String path, BooleanSupplier wanted) throws IOException {
        synchronized (LOAD) {
            if (!wanted.getAsBoolean()) return null;
            return acquireLocked(path);
        }
    }

    private static Model acquireLocked(String path) throws IOException {
        Model idle;
        synchronized (LOCK) {
            if (model != null && path.equals(loadedPath)) {
                users++;
                return model;
            }
            idle = detachIfIdle();
        }
        // Free the other language's model first, so both are never in memory together.
        if (idle != null) idle.close();
        // The slow part, outside the lock so the main thread is never held up behind it.
        Model fresh = new Model(path);
        Model unused;
        Model result;
        synchronized (LOCK) {
            if (model != null && path.equals(loadedPath)) {
                // Another start loaded the same model meanwhile: use that one.
                unused = fresh;
                users++;
                result = model;
            } else {
                unused = detachIfIdle();
                if (model == null) {
                    model = fresh;
                    loadedPath = path;
                    users = 1;
                }
                // Otherwise the kept model is still in use: `fresh` is returned unkept (see release).
                result = fresh;
            }
        }
        if (unused != null) unused.close();
        return result;
    }

    /** Ends one use of a model from {@link #acquire}. A model that was not kept is closed here. */
    static void release(Model used) {
        synchronized (LOCK) {
            if (used == model) {
                if (users > 0) users--;
                return;
            }
        }
        // Vosk counts references natively, so closing is safe even if a recognizer is still being freed.
        used.close();
    }

    /** Frees the kept model's memory unless a session is using it. Main thread: the freeing runs in the background. */
    static void releaseIfIdle() {
        Model idle;
        synchronized (LOCK) {
            idle = detachIfIdle();
        }
        if (idle == null) return;
        CLOSER.execute(() -> {
            long begun = SystemClock.elapsedRealtime();
            idle.close();
            Log.i(TAG, "model freed in " + (SystemClock.elapsedRealtime() - begun) + " ms");
        });
    }

    /** Takes the kept model out of the holder if no session uses it; the caller closes it. Under LOCK. */
    @Nullable
    private static Model detachIfIdle() {
        if (model == null || users != 0) return null;
        Model idle = model;
        model = null;
        loadedPath = null;
        return idle;
    }
}
