package ph.agap.app.captions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

/**
 * English captions with Google's offline speech recognition: Android's on-device recognizer, or
 * Google's speech service limited to its offline engine (see GoogleOnDeviceSupport). Android's
 * recognizer hears one sentence per session, so this class starts a new session after each one
 * and after silence, until stop() is called.
 */
@RequiresApi(33)
public final class GoogleOnDeviceEngine implements CaptionEngine {

    private static final String TAG = "AGAP/Captions";
    /** Give up after this many failed sessions in a row with no words heard. */
    private static final int MAX_FAILURES = 5;
    /** A "silence" result this soon after starting counts as a failure, so a broken recognizer
     *  cannot spin in a tight loop. */
    private static final long MIN_SESSION_MS = 400;

    private final Context context;
    private final String languageTag;
    @Nullable private final ComponentName service;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable private SpeechRecognizer recognizer;
    @Nullable private CaptionListener listener;
    private boolean running;
    /** Numbers the sessions, so late callbacks from an ended session are ignored. */
    private int session;
    private int failures;
    private long sessionStart;

    /** @param service Google's speech service, or null for Android's own on-device recognizer */
    public GoogleOnDeviceEngine(Context context, String languageTag, @Nullable ComponentName service) {
        this.context = context.getApplicationContext();
        this.languageTag = languageTag;
        this.service = service;
    }

    @Override
    public void start(CaptionListener listener) {
        if (running) return;
        this.listener = listener;
        running = true;
        failures = 0;
        Log.i(TAG, "start " + engineName() + " " + languageTag);
        listen();
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        session++;
        main.removeCallbacksAndMessages(null);
        releaseRecognizer();
        Log.i(TAG, "stop");
    }

    @Override
    public String languageTag() {
        return languageTag;
    }

    @Override
    public String engineName() {
        return service == null ? "Google on-device (system)" : "Google speech service, offline only";
    }

    private void listen() {
        if (!running) return;
        if (recognizer == null) {
            recognizer = GoogleOnDeviceSupport.create(context, service);
        }
        int id = ++session;
        recognizer.setRecognitionListener(new Callbacks(id));
        sessionStart = SystemClock.elapsedRealtime();
        recognizer.startListening(recognizeIntent(languageTag));
    }

    /** The request for one session. Also used to check for and download the language pack. */
    static Intent recognizeIntent(String languageTag) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        return intent;
    }

    private void releaseRecognizer() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
    }

    private void restart(long delayMs, boolean freshRecognizer) {
        if (freshRecognizer) {
            releaseRecognizer();
        }
        main.postDelayed(this::listen, delayMs);
    }

    private void fail(CaptionListener.Failure failure) {
        CaptionListener l = listener;
        stop();
        if (l != null) l.onFailed(failure);
    }

    private static int wordCount(@Nullable String text) {
        return text == null || text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
    }

    @Nullable
    private static String firstResult(Bundle results) {
        if (results == null) return null;
        ArrayList<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return texts == null || texts.isEmpty() ? null : texts.get(0);
    }

    private final class Callbacks implements RecognitionListener {
        private final int id;
        private boolean ended;

        Callbacks(int id) {
            this.id = id;
        }

        private boolean stale(String event) {
            boolean stale = !running || ended || id != session || listener == null;
            if (stale) debug(event + " ignored (session ended)");
            return stale;
        }

        /** Session events for the developer log: event names and word counts, never the words. */
        private void debug(String event) {
            Log.d(TAG, "s" + id + " +" + (SystemClock.elapsedRealtime() - sessionStart) + "ms " + event);
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (stale("ready")) return;
            debug("ready");
            listener.onListening();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (stale("partial")) return;
            String text = firstResult(partialResults);
            debug("partial " + wordCount(text) + " words");
            if (text != null && !text.trim().isEmpty()) {
                failures = 0;
                listener.onPartial(text);
            }
        }

        @Override
        public void onResults(Bundle results) {
            if (stale("results")) return;
            ended = true;
            String text = firstResult(results);
            debug("results " + wordCount(text) + " words");
            if (text != null && !text.trim().isEmpty()) {
                failures = 0;
                listener.onFinal(text);
            } else {
                listener.onSegmentEnd();
            }
            restart(0, false);
        }

        @Override
        public void onError(int error) {
            if (stale("error " + RecognizerErrors.name(error))) return;
            ended = true;
            debug("error " + RecognizerErrors.name(error));
            listener.onSegmentEnd();
            RecognizerErrors.Action action = RecognizerErrors.actionFor(error);
            boolean tooQuick = SystemClock.elapsedRealtime() - sessionStart < MIN_SESSION_MS;
            if (action != RecognizerErrors.Action.SILENCE || tooQuick) {
                Log.w(TAG, "recognizer error " + RecognizerErrors.name(error)
                        + (tooQuick ? " (quick)" : "") + ", failures " + (failures + 1));
            }
            switch (action) {
                case STOP_PERMISSION:
                    fail(CaptionListener.Failure.PERMISSION);
                    return;
                case STOP_LANGUAGE:
                    fail(CaptionListener.Failure.LANGUAGE_PACK);
                    return;
                case SILENCE:
                    if (!tooQuick) {
                        restart(0, false);
                        return;
                    }
                    // A silence result right after starting: treat it as a failure below.
                default:
                    failures++;
                    if (failures >= MAX_FAILURES) {
                        fail(CaptionListener.Failure.RECOGNIZER);
                    } else {
                        restart(250L * failures, true);
                    }
            }
        }

        @Override public void onBeginningOfSpeech() { debug("speech begins"); }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() { debug("speech ends"); }
        @Override public void onEvent(int eventType, Bundle params) { }
    }
}
