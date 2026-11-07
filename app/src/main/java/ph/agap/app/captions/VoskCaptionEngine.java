package ph.agap.app.captions;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Captions with one of Vosk's bundled models: Filipino (vosk-model-tl-ph-generic), or the small
 * English model where Google's offline English is not available. Everything runs on the phone.
 * Unlike Google's recognizer, Vosk listens continuously: it sends partial words as they form and a
 * final result at each pause, so no restarts are needed.
 *
 * AGAP feeds the microphone to Vosk itself instead of using Vosk's SpeechService (D-025): a
 * microphone read error ends the session with a message instead of crashing the app, partial words
 * go to the screen only when they change, and the 2 s audio buffer leaves room for a slower phone
 * while Vosk finishes a sentence. Recognition is the same as with SpeechService.
 *
 * One worker thread per session loads the model (slow the first time), owns the microphone and
 * the recognizer, and frees them when it ends. The listener hears onListening() only once the
 * microphone is open. Logs hold event names, never the words.
 */
public final class VoskCaptionEngine implements CaptionEngine {

    private static final String TAG = "AGAP/Captions";
    private static final int SAMPLE_RATE = 16000;
    /** 0.1 s of audio per read. */
    private static final int CHUNK = SAMPLE_RATE / 10;
    /** The microphone's buffer: 2 s of 16-bit audio, so no sound is lost while Vosk catches up. */
    private static final int BUFFER_BYTES = SAMPLE_RATE * 2 * 2;

    private final File modelDir;
    private final String languageTag;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable private CaptionListener listener;
    @Nullable private Thread worker;
    /**
     * Held while the worker opens the microphone and while stop() looks at it, so stop() knows for
     * sure whether the microphone is open (Phase 8, ANR audit). If it is not, the worker never opens
     * it, and stop() does not wait while the model loads.
     */
    private final Object micLock = new Object();
    @Nullable private AudioRecord record; // guarded by micLock
    private volatile boolean running;
    /** Numbers the starts (written on the main thread only); a worker or post from an older one stops. */
    private volatile int session;

    /** @param languageTag the language the model at {@code modelDir} recognises, such as "fil-PH" */
    public VoskCaptionEngine(File modelDir, String languageTag) {
        this.modelDir = modelDir;
        this.languageTag = languageTag;
    }

    @Override
    public void start(CaptionListener listener) {
        if (running) return;
        this.listener = listener;
        running = true;
        int id = ++session;
        Log.i(TAG, "start " + engineName() + " " + languageTag);
        worker = new Thread(() -> run(id), "agap-vosk");
        worker.start();
    }

    @Override
    public void stop() {
        if (!running) return;
        long begun = SystemClock.elapsedRealtime();
        AudioRecord r;
        synchronized (micLock) {
            running = false;
            session++;
            r = record;
        }
        Thread w = worker;
        worker = null;
        if (r != null) {
            try {
                r.stop(); // ends the recording at once and wakes up a blocked read
            } catch (IllegalStateException ignored) {
                // not recording yet
            }
            if (w != null) {
                try {
                    // Short wait so the microphone is released before another feature opens it.
                    w.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // Without an open microphone the worker is loading the model: it ends by itself, no wait.
        Log.i(TAG, "stop in " + (SystemClock.elapsedRealtime() - begun) + " ms"
                + (r == null ? " (microphone not open yet)" : ""));
    }

    @Override
    public String languageTag() {
        return languageTag;
    }

    @Override
    public String engineName() {
        return "Vosk " + modelDir.getName() + " (offline)";
    }

    /** Worker thread: model, microphone, recognition loop; frees all three when it ends. */
    @SuppressLint("MissingPermission") // the screen checks RECORD_AUDIO before it starts the engine
    private void run(int id) {
        Model model = null;
        Recognizer recognizer = null;
        AudioRecord rec = null;
        AudioManager.AudioRecordingCallback silencedWatch = null;
        try {
            try {
                LibVosk.setLogLevel(LogLevel.WARNINGS);
                long begun = System.nanoTime();
                model = VoskModelHolder.acquire(modelDir.getPath(), () -> live(id));
                if (model == null) {
                    Log.i(TAG, "s" + id + " stopped before the model was loaded");
                    return;
                }
                Log.i(TAG, "model ready in " + (System.nanoTime() - begun) / 1_000_000 + " ms");
            } catch (Throwable e) {
                // IOException for a missing or damaged model; also OutOfMemoryError and link errors.
                Log.w(TAG, "model did not load: " + e);
                fail(id, CaptionListener.Failure.MODEL);
                return;
            }
            if (!live(id)) return;

            recognizer = new Recognizer(model, SAMPLE_RATE);
            int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            synchronized (micLock) {
                // Stopped while the model loaded: do not open the microphone at all.
                if (!live(id)) return;
                rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(minBuffer, BUFFER_BYTES));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "s" + id + " microphone could not be opened");
                    fail(id, CaptionListener.Failure.RECOGNIZER);
                    return;
                }
                record = rec;
            }
            rec.startRecording();
            if (!live(id)) return;
            if (rec.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                Log.w(TAG, "s" + id + " microphone is busy");
                fail(id, CaptionListener.Failure.RECOGNIZER);
                return;
            }
            silencedWatch = watchSilenced(id, rec);
            post(id, l -> l.onListening());
            Log.d(TAG, "s" + id + " listening");

            short[] buffer = new short[CHUNK];
            String shown = "";
            while (live(id)) {
                int n = rec.read(buffer, 0, buffer.length);
                if (!live(id)) break;
                if (n <= 0) {
                    Log.w(TAG, "s" + id + " microphone read error " + n);
                    fail(id, CaptionListener.Failure.RECOGNIZER);
                    return;
                }
                if (recognizer.acceptWaveForm(buffer, n)) {
                    // A pause ended the sentence.
                    String words = VoskJson.text(recognizer.getResult());
                    shown = "";
                    post(id, l -> {
                        if (words.isEmpty()) l.onSegmentEnd(); else l.onFinal(words);
                    });
                } else {
                    String words = VoskJson.partial(recognizer.getPartialResult());
                    if (!words.isEmpty() && !words.equals(shown)) {
                        shown = words;
                        post(id, l -> l.onPartial(words));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "s" + id + " vosk error: " + e);
            fail(id, CaptionListener.Failure.RECOGNIZER);
        } finally {
            synchronized (micLock) {
                if (record == rec) record = null;
            }
            if (rec != null) {
                if (silencedWatch != null) rec.unregisterAudioRecordingCallback(silencedWatch);
                try {
                    rec.stop();
                } catch (IllegalStateException ignored) {
                    // not recording
                }
                rec.release();
            }
            if (recognizer != null) recognizer.close();
            if (model != null) VoskModelHolder.release(model);
        }
    }

    /**
     * Android 10+ gives the microphone to a phone call or a privileged app and sends AGAP silence
     * meanwhile, without an error. This tells the screen when that starts and ends.
     */
    private AudioManager.AudioRecordingCallback watchSilenced(int id, AudioRecord rec) {
        int sessionId = rec.getAudioSessionId();
        boolean[] last = {false};
        AudioManager.AudioRecordingCallback callback = new AudioManager.AudioRecordingCallback() {
            @Override
            public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                for (AudioRecordingConfiguration config : configs) {
                    if (config.getClientAudioSessionId() != sessionId) continue;
                    boolean silenced = config.isClientSilenced();
                    if (silenced != last[0]) {
                        last[0] = silenced;
                        Log.i(TAG, "s" + id + " microphone silenced by Android: " + silenced);
                        CaptionListener l = listener;
                        if (live(id) && l != null) l.onMicSilenced(silenced);
                    }
                }
            }
        };
        rec.registerAudioRecordingCallback(main::post, callback);
        // Already silenced when listening began (a call in progress): the callback may not fire.
        // Checked here on the worker; the screen hears about it on the main thread.
        AudioRecordingConfiguration now = rec.getActiveRecordingConfiguration();
        if (now != null) {
            List<AudioRecordingConfiguration> current = Collections.singletonList(now);
            main.post(() -> callback.onRecordingConfigChanged(current));
        }
        return callback;
    }

    /** True while session {@code id} is the one running (any thread). */
    private boolean live(int id) {
        return running && session == id;
    }

    private interface Call {
        void on(CaptionListener listener);
    }

    /** Runs {@code call} on the main thread if session {@code id} is still the live one. */
    private void post(int id, Call call) {
        main.post(() -> {
            CaptionListener l = listener;
            if (running && id == session && l != null) call.on(l);
        });
    }

    /** Ends session {@code id} (if still live) and tells the listener why. */
    private void fail(int id, CaptionListener.Failure failure) {
        main.post(() -> {
            CaptionListener l = listener;
            if (!running || id != session || l == null) return;
            stop();
            l.onFailed(failure);
        });
    }
}
