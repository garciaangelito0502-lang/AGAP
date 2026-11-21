package ph.agap.app.listen;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ph.agap.app.R;
import ph.agap.app.captions.CaptionEngine;
import ph.agap.app.captions.CaptionListener;
import ph.agap.app.captions.CaptionRelay;
import ph.agap.app.captions.Transcript;
import ph.agap.app.mic.MicOwner;
import ph.agap.app.watch.WatchMessages;
import ph.agap.app.watch.WatchSender;

/**
 * One Live Captions session for the whole app (Phase 11). The Live Captions screen picks the
 * language and builds the engine; this session runs it and keeps the transcript, so with
 * background listening on, captions go on after the user leaves AGAP and the whole session is still
 * there on return. The transcript is in memory only, never saved or logged (contract 1.2). It also
 * feeds the watch, the floating caption window and the listening notification. Main thread only.
 */
public final class CaptionsSession implements CaptionListener, MicOwner.User {

    private static final String TAG = "AGAP/Captions";
    @Nullable private static CaptionsSession instance;

    private final Context app;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Transcript transcript = new Transcript();
    @Nullable private CaptionEngine engine;

    private boolean demo;
    private boolean filipino;
    private String languageName = "";
    private boolean heardReady;
    private boolean micSilenced;
    @Nullable private Failure lastFailure;
    @Nullable private String micNote;

    private CaptionsSession(Context app) {
        this.app = app;
    }

    public static CaptionsSession get(Context context) {
        if (instance == null) instance = new CaptionsSession(context.getApplicationContext());
        return instance;
    }

    // ---- What the screen reads ----

    public boolean isRunning() { return engine != null; }
    /** Listening with the microphone (not Demonstration Mode's sample script). */
    public boolean isListeningForReal() { return engine != null && !demo; }
    public boolean isDemo() { return demo; }
    /** The language of the running session (or the last one). */
    public boolean isFilipino() { return filipino; }
    public String languageName() { return languageName; }
    public Transcript transcript() { return transcript; }
    public boolean heardReady() { return heardReady; }
    public boolean micSilenced() { return micSilenced; }
    @Nullable public Failure lastFailure() { return lastFailure; }
    @Nullable public String micNote() { return micNote; }

    /** Called on every change: new words, start, stop. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    // ---- Starting and stopping ----

    /**
     * Starts a session with an engine the screen built for the chosen language. A new session starts
     * with an empty page.
     *
     * @param demo true for Demonstration Mode's sample captions: no microphone, never in the background
     */
    public void start(CaptionEngine newEngine, boolean demo, boolean filipino, String languageName) {
        if (engine != null) return;
        transcript.clear();
        lastFailure = null;
        micSilenced = false;
        heardReady = false;
        this.demo = demo;
        this.filipino = filipino;
        this.languageName = languageName;
        if (demo) {
            micNote = null;
        } else {
            String stopped = MicOwner.take(this);
            micNote = stopped == null ? null : app.getString(R.string.mic_other_stopped, stopped);
        }
        engine = newEngine;
        engine.start(this);
        WatchSender.get(app).sendState(WatchMessages.Mode.CAPTIONS, engine.languageTag());
        ListeningService.update(app);
        FloatingCaptions.update(app);
        changed();
    }

    public void stop() {
        if (engine == null) return;
        engine.stop();
        engine = null;
        ended();
    }

    /** Empties the page between sessions (the Clear button). */
    public void clear() {
        if (engine != null) return;
        transcript.clear();
        changed();
    }

    public void clearFailure() {
        lastFailure = null;
    }

    public void clearMicNote() {
        micNote = null;
    }

    private void ended() {
        micSilenced = false;
        MicOwner.release(this);
        WatchSender.get(app).sendState(WatchMessages.Mode.IDLE, "");
        transcript.endSegment();
        CaptionRelay.publishStopped();
        ListeningService.update(app);
        FloatingCaptions.update(app);
        changed();
    }

    private void changed() {
        for (Runnable listener : listeners) listener.run();
        AgapWidget.refresh(app);
    }

    /** New words: the screen, the floating window and the notification; not the widget (too often). */
    private void wordsChanged() {
        for (Runnable listener : listeners) listener.run();
        FloatingCaptions.wordsChanged(app);
        ListeningService.captionsChanged(app);
    }

    private static int wordCount(String line) {
        String t = line.trim();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    // ---- MicOwner.User ----

    @Override
    public String micUserName() {
        return app.getString(R.string.captions_title);
    }

    @Override
    public void onMicTaken(String byWhom) {
        stop();
        micNote = app.getString(R.string.mic_taken, byWhom);
        changed();
    }

    // ---- CaptionListener ----

    @Override
    public void onListening() {
        if (!heardReady) {
            heardReady = true;
            changed();
        }
    }

    @Override
    public void onPartial(String text) {
        if (engine == null) return;
        transcript.setPartial(text);
        CaptionRelay.publish(transcript.latest(), false);
        wordsChanged();
    }

    @Override
    public void onFinal(String text) {
        if (engine == null) return;
        transcript.addFinal(text);
        CaptionRelay.publish(transcript.latest(), true);
        // Counts only, never the words (contract 1.2): shows that captions go on in the background.
        Log.d(TAG, "line " + transcript.lines().size() + ", " + wordCount(transcript.latest()) + " words"
                + (AppVisibility.isAppVisible() ? "" : " (AGAP not in view)"));
        wordsChanged();
    }

    @Override
    public void onSegmentEnd() {
        boolean hadPartial = !transcript.partial().isEmpty();
        transcript.endSegment();
        if (hadPartial) {
            CaptionRelay.publish(transcript.latest(), true);
            wordsChanged();
        }
    }

    @Override
    public void onMicSilenced(boolean silenced) {
        micSilenced = silenced;
        changed();
    }

    @Override
    public void onFailed(Failure failure) {
        engine = null;
        lastFailure = failure;
        ended();
    }
}
