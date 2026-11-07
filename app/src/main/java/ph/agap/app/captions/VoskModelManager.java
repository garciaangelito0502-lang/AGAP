package ph.agap.app.captions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One Vosk speech model on this phone (Filipino, or the English fallback). It is bundled in the APK
 * as a zip and unpacked once, on first use, into the app's private storage. Never on every launch:
 * after a good unpack a marker file says so. All methods are called on the main thread, and the
 * listener is called back on it.
 */
public final class VoskModelManager {

    private static final String TAG = "AGAP/Captions";

    public enum State {
        /** Not looked at yet. */
        UNKNOWN,
        /** Not unpacked yet and there is room to do it. */
        NOT_INSTALLED,
        /** Not unpacked yet and the phone has too little free storage. */
        LOW_STORAGE,
        /** Unpacking now; see {@link #percent()}. */
        UNPACKING,
        /** Ready to use. */
        INSTALLED,
        /** The last unpack failed for a reason other than storage. */
        FAILED
    }

    public interface Listener {
        void onModelStateChanged();
    }

    private final Context context;
    private final VoskModelSpec spec;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "agap-model-unpack"));

    private State state = State.UNKNOWN;
    private int percent;
    private long freeBytes;
    @Nullable private Listener listener;
    private volatile boolean cancelled;

    public VoskModelManager(Context context, VoskModelSpec spec) {
        this.context = context.getApplicationContext();
        this.spec = spec;
    }

    /** True when the model is unpacked and complete on this phone (quick file check; any thread). */
    public static boolean isInstalled(Context context, VoskModelSpec spec) {
        File dir = new File(new File(context.getFilesDir(), "vosk"), spec.name);
        return ModelUnpacker.isInstalled(dir, spec.name);
    }

    /** What to do given what is on the phone. Kept apart so it can be tested. */
    static State stateFor(boolean installed, long freeBytes, long requiredFreeBytes) {
        if (installed) return State.INSTALLED;
        return freeBytes >= requiredFreeBytes ? State.NOT_INSTALLED : State.LOW_STORAGE;
    }

    public VoskModelSpec spec() {
        return spec;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public State state() {
        return state;
    }

    /** 0 to 100 while unpacking. */
    public int percent() {
        return percent;
    }

    /** Free internal storage when the state was last checked, in bytes. */
    public long freeBytes() {
        return freeBytes;
    }

    /** The folder the model is unpacked into. */
    public File modelDir() {
        return new File(new File(context.getFilesDir(), "vosk"), spec.name);
    }

    /**
     * Looks at the phone again (installed? enough room?). Does nothing while unpacking.
     * Uses getUsableSpace, not StorageManager.getAllocatableBytes: that one may take seconds and must
     * not run on the main thread. It can only under-count (cache the system could clear), and the
     * message then asks the user to free space, which is safe.
     */
    @SuppressLint("UsableSpace")
    public void refresh() {
        if (state == State.UNPACKING) return;
        freeBytes = debugFreeBytes(context.getFilesDir().getUsableSpace());
        State next = stateFor(ModelUnpacker.isInstalled(modelDir(), spec.name), freeBytes, spec.requiredFreeBytes);
        // A failed unpack stays shown until the user looks again on purpose (retry).
        if (next != state) {
            state = next;
            changed();
        }
    }

    /** Starts unpacking in the background. Ignored unless the model is missing and there is room. */
    public void startUnpack() {
        if (state != State.NOT_INSTALLED && state != State.FAILED) return;
        refresh();
        if (state != State.NOT_INSTALLED) return;
        state = State.UNPACKING;
        percent = 0;
        cancelled = false;
        changed();
        io.execute(this::unpack);
    }

    /** Deletes the unpacked model, so the next setup starts clean (used if it will not load). */
    public void discard() {
        if (state == State.UNPACKING) return;
        VoskModelHolder.releaseIfIdle();
        ModelUnpacker.deleteRecursively(modelDir());
        state = State.UNKNOWN;
        refresh();
    }

    /** Frees the loaded model's memory (about 1 GB for Filipino) if no session is using it. */
    public static void releaseMemory() {
        VoskModelHolder.releaseIfIdle();
    }

    /** Stops an unpack in progress; called when the screen state is thrown away. */
    public void shutdown() {
        cancelled = true;
        io.shutdown();
    }

    private void unpack() {
        File dir = modelDir();
        File parent = dir.getParentFile();
        if (parent != null) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        State result;
        try (AssetFileDescriptor afd = context.getAssets().openFd(spec.asset());
             InputStream zip = afd.createInputStream()) {
            long total = Math.max(1, afd.getLength());
            long begun = System.nanoTime();
            int[] last = {-1};
            ModelUnpacker.unpack(zip, spec.name, dir, bytes -> {
                int p = (int) Math.min(99, bytes * 100 / total);
                if (p != last[0]) {
                    last[0] = p;
                    main.post(() -> {
                        percent = p;
                        changed();
                    });
                }
            }, () -> cancelled);
            Log.i(TAG, spec.name + " unpacked in " + (System.nanoTime() - begun) / 1_000_000 + " ms");
            result = State.INSTALLED;
        } catch (ModelUnpacker.CancelledException e) {
            Log.i(TAG, spec.name + " unpack cancelled");
            result = State.UNKNOWN;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, spec.name + " unpack failed: " + e);
            result = ModelUnpacker.isOutOfSpace(e) ? State.LOW_STORAGE : State.FAILED;
        }
        State finalResult = result;
        main.post(() -> {
            if (finalResult == State.INSTALLED || finalResult == State.FAILED) {
                state = finalResult;
                percent = 100;
                changed();
            } else {
                // Cancelled or out of space: look at the phone again to see which state fits now.
                state = State.UNKNOWN;
                refresh();
            }
        });
    }

    /**
     * Debug builds only: the long "fake_free_mb" in preferences "agap_debug" stands in for the free
     * storage, so the low-storage message can be checked on a phone with plenty of room.
     */
    private long debugFreeBytes(long real) {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return real;
        long fakeMb = context.getSharedPreferences("agap_debug", Context.MODE_PRIVATE)
                .getLong("fake_free_mb", -1);
        return fakeMb >= 0 ? fakeMb * 1_000_000L : real;
    }

    private void changed() {
        if (listener != null) listener.onModelStateChanged();
    }
}
