package ph.agap.app.captions;

import androidx.annotation.Nullable;

import org.vosk.Model;

import java.io.IOException;

/**
 * Keeps the loaded Vosk model in memory between sessions. Loading takes seconds and the Filipino
 * model needs roughly 1 GB of RAM, so it is loaded once on first Start and freed when it is not in
 * use and the app leaves the screen or the user picks English.
 *
 * Only one model is kept. If a different model is asked for while the kept one is still in use
 * (a session that is ending while the next one, in the other language, starts loading), the new one
 * is handed out without being kept, and closed when its session ends. A caller therefore always gets
 * the model it asked for.
 */
final class VoskModelHolder {

    private static final Object LOCK = new Object();
    @Nullable private static Model model;
    @Nullable private static String loadedPath;
    private static int users;

    private VoskModelHolder() { }

    /** Returns the model at {@code path} (loading it if needed) and counts one use. Call off the main thread. */
    static Model acquire(String path) throws IOException {
        synchronized (LOCK) {
            if (model != null && path.equals(loadedPath)) {
                users++;
                return model;
            }
            dropIfIdle();
        }
        // The slow part, outside the lock so the main thread is never held up behind it.
        Model fresh = new Model(path);
        synchronized (LOCK) {
            if (model != null && path.equals(loadedPath)) {
                // Another start loaded the same model meanwhile: use that one.
                fresh.close();
                users++;
                return model;
            }
            dropIfIdle();
            if (model == null) {
                model = fresh;
                loadedPath = path;
                users = 1;
            }
            // Otherwise the kept model is still in use: `fresh` is returned unkept (see release).
            return fresh;
        }
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

    /** Frees the kept model's memory unless a session is using it. */
    static void releaseIfIdle() {
        synchronized (LOCK) {
            dropIfIdle();
        }
    }

    private static void dropIfIdle() {
        if (model != null && users == 0) {
            model.close();
            model = null;
            loadedPath = null;
        }
    }
}
