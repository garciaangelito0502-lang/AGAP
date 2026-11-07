package ph.agap.app.alerts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
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

import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier;
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult;
import com.google.mediapipe.tasks.audio.core.RunningMode;
import com.google.mediapipe.tasks.components.containers.AudioData;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.ClassificationResult;
import com.google.mediapipe.tasks.core.BaseOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens with the microphone and classifies the sound with YAMNet through MediaPipe's Audio
 * Classifier. YAMNet hears 0.975 s at a time (15,600 samples at 16 kHz); a new window starts every
 * half window, so each sound is heard about twice a second. Audio stays in memory and is never
 * saved or logged (contract 1.2). Listener calls arrive on the main thread.
 */
public final class SoundAlertEngine {

    public interface Listener {
        /** The microphone is open. */
        void onListening();

        /** One window was classified. top is the strongest of all 521 YAMNet classes. */
        void onWindow(AlertDetector.Result result, String topClass, float topScore);

        /** Listening failed and has stopped. */
        void onFailed(String reason);

        /**
         * Android silences AGAP's microphone (a phone call, or another app that has priority) or
         * gives it back. While silenced the engine keeps running but hears nothing.
         */
        void onMicSilenced(boolean silenced);
    }

    public static final String MODEL_ASSET = "yamnet.tflite";
    private static final String TAG = "AGAP/Alerts";
    private static final int SAMPLE_RATE = 16_000;
    private static final int WINDOW = 15_600;
    private static final int HOP = WINDOW / 2;

    private final Context context;
    private final AnnexA annex;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable private Thread worker;
    /**
     * Held while the worker opens the microphone and while stop() looks at it, so stop() knows for
     * sure whether the microphone is open (Phase 8, ANR audit). If it is not, the worker will never
     * open it, and stop() does not wait.
     */
    private final Object micLock = new Object();
    @Nullable private AudioRecord record; // guarded by micLock
    private volatile boolean running;

    public SoundAlertEngine(Context context, AnnexA annex) {
        this.context = context.getApplicationContext();
        this.annex = annex;
    }

    /** Starts listening. threshold is the user's alert threshold, 0-1. */
    public void start(float threshold, Listener listener) {
        if (running) return;
        running = true;
        AlertDetector detector = new AlertDetector(annex);
        worker = new Thread(() -> listen(detector, threshold, listener), "AGAP-SoundAlerts");
        worker.start();
        Log.i(TAG, "start, threshold " + threshold);
    }

    /**
     * Stops listening. If the microphone is open, waits until the worker has released it (a read
     * and one classification, tens of milliseconds), so the other feature can open it next. If the
     * worker is still setting up the classifier, it returns at once: the worker ends by itself
     * without opening the microphone.
     */
    public void stop() {
        if (!running) return;
        long begun = SystemClock.elapsedRealtime();
        AudioRecord r;
        synchronized (micLock) {
            running = false;
            r = record;
        }
        Thread w = worker;
        worker = null;
        if (r != null) {
            try {
                r.stop(); // wakes up a blocked read
            } catch (IllegalStateException ignored) {
                // already stopped
            }
            if (w != null) {
                try {
                    w.join(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        main.removeCallbacksAndMessages(null);
        Log.i(TAG, "stop in " + (SystemClock.elapsedRealtime() - begun) + " ms"
                + (r == null ? " (microphone not open yet)" : ""));
    }

    @SuppressLint("MissingPermission") // the screen checks RECORD_AUDIO before it starts the engine
    private void listen(AlertDetector detector, float threshold, Listener listener) {
        AudioClassifier classifier = null;
        AudioRecord rec = null;
        AudioManager.AudioRecordingCallback silencedWatch = null;
        try {
            long begun = SystemClock.elapsedRealtime();
            classifier = AudioClassifier.createFromOptions(context,
                    AudioClassifier.AudioClassifierOptions.builder()
                            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
                            .setRunningMode(RunningMode.AUDIO_CLIPS)
                            // All 521 classes: the Annex A ones are rarely in the top few.
                            .setMaxResults(521)
                            .build());
            Log.i(TAG, "classifier ready in " + (SystemClock.elapsedRealtime() - begun) + " ms");

            int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT);
            int source = audioSource();
            synchronized (micLock) {
                // Stopped while the classifier was set up: do not open the microphone at all.
                if (!running) return;
                rec = new AudioRecord(source, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
                        Math.max(minBuffer, HOP * 4 * 2));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                    fail(listener, "microphone could not be opened");
                    return;
                }
                record = rec;
            }
            rec.startRecording();
            if (!running) return;
            if (rec.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                fail(listener, "microphone is busy");
                return;
            }
            silencedWatch = watchSilenced(rec, listener);
            main.post(() -> { if (running) listener.onListening(); });

            AudioData.AudioDataFormat format = AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1).setSampleRate(SAMPLE_RATE).build();
            float[] window = new float[WINDOW];
            float[] hop = new float[HOP];
            int filled = 0;
            while (running) {
                int n = rec.read(hop, 0, HOP, AudioRecord.READ_BLOCKING);
                if (!running) break;
                if (n <= 0) {
                    fail(listener, "microphone read error " + n);
                    return;
                }
                System.arraycopy(window, n, window, 0, WINDOW - n);
                System.arraycopy(hop, 0, window, WINDOW - n, n);
                filled = Math.min(WINDOW, filled + n);
                if (filled < WINDOW) continue;

                AudioData data = AudioData.create(format, WINDOW);
                data.load(window);
                long started = SystemClock.elapsedRealtime();
                AudioClassifierResult result = classifier.classify(data);
                long now = System.currentTimeMillis();
                handle(result, detector, threshold, now, SystemClock.elapsedRealtime() - started, listener);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "sound alerts failed", e);
            fail(listener, e.getClass().getSimpleName());
        } finally {
            synchronized (micLock) {
                record = null;
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
            if (classifier != null) classifier.close();
        }
    }

    /**
     * Android 10+ gives the microphone to a phone call or a privileged app and sends AGAP silence
     * meanwhile, without an error. This tells the screen when that starts and ends.
     */
    private AudioManager.AudioRecordingCallback watchSilenced(AudioRecord rec, Listener listener) {
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
                        Log.i(TAG, "microphone silenced by Android: " + silenced);
                        if (running) listener.onMicSilenced(silenced);
                    }
                }
            }
        };
        rec.registerAudioRecordingCallback(main::post, callback);
        // Already silenced when listening began (a call in progress): the callback may not fire.
        // Checked here on the worker; the screen hears about it on the main thread like every callback.
        AudioRecordingConfiguration now = rec.getActiveRecordingConfiguration();
        if (now != null) {
            List<AudioRecordingConfiguration> current = java.util.Collections.singletonList(now);
            main.post(() -> callback.onRecordingConfigChanged(current));
        }
        return callback;
    }

    /**
     * VOICE_RECOGNITION: the source Android tunes for machine listening, without the automatic
     * gain changes of the plain MIC source. Debug builds can override it for microphone tests with
     * the int "audio_source" in shared preferences "agap_debug" (set over adb); release builds
     * ignore it.
     */
    private int audioSource() {
        int source = MediaRecorder.AudioSource.VOICE_RECOGNITION;
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            source = context.getSharedPreferences("agap_debug", Context.MODE_PRIVATE)
                    .getInt("audio_source", source);
        }
        AudioManager audio = context.getSystemService(AudioManager.class);
        Log.i(TAG, "audio source " + source + ", unprocessed supported: "
                + audio.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
        return source;
    }

    private void handle(AudioClassifierResult result, AlertDetector detector, float threshold,
                        long now, long classifyMs, Listener listener) {
        List<ClassificationResult> windows = result.classificationResults();
        if (windows.isEmpty() || windows.get(windows.size() - 1).classifications().isEmpty()) return;
        List<Category> categories = windows.get(windows.size() - 1).classifications().get(0).categories();

        Map<String, Float> mapped = new HashMap<>();
        String topClass = "";
        float topScore = 0f;
        for (Category c : categories) {
            if (c.score() > topScore) {
                topScore = c.score();
                topClass = c.categoryName();
            }
            if (annex.alertFor(c.categoryName()) != null) mapped.put(c.categoryName(), c.score());
        }
        AlertDetector.Result r = detector.evaluate(mapped, threshold, now);

        // Class names and scores only, for tuning (contract 1.12); never audio.
        if (r.event != null) {
            Log.i(TAG, String.format(java.util.Locale.ROOT, "ALERT %s %.2f (%s) at %d",
                    r.event.label(), r.event.confidence, r.event.yamnetClass, r.event.detectedAtMillis));
        }
        Log.d(TAG, String.format(java.util.Locale.ROOT, "win top=%s %.2f map=%s %dms",
                topClass, topScore, topAlerts(mapped, 3), classifyMs));

        String top = topClass;
        float topS = topScore;
        main.post(() -> { if (running) listener.onWindow(r, top, topS); });
    }

    /** The n strongest alerts in one window as "Doorbell:0.62,Knocking:0.10", for the log. */
    private String topAlerts(Map<String, Float> mapped, int n) {
        Map<String, Float> perAlert = new HashMap<>();
        for (Map.Entry<String, Float> e : mapped.entrySet()) {
            String label = annex.alertFor(e.getKey()).label;
            Float known = perAlert.get(label);
            if (known == null || e.getValue() > known) perAlert.put(label, e.getValue());
        }
        List<Map.Entry<String, Float>> sorted = new java.util.ArrayList<>(perAlert.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(n, sorted.size()); i++) {
            if (i > 0) out.append(',');
            out.append(sorted.get(i).getKey()).append(':')
                    .append(String.format(java.util.Locale.ROOT, "%.2f", sorted.get(i).getValue()));
        }
        return out.toString();
    }

    private void fail(Listener listener, String reason) {
        Log.w(TAG, "failed: " + reason);
        main.post(() -> {
            if (!running) return;
            running = false;
            listener.onFailed(reason);
        });
    }
}
