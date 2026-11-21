package ph.agap.app.listen;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertDetector;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.alerts.AnnexA;
import ph.agap.app.alerts.SoundAlertEngine;
import ph.agap.app.captions.VoskModelManager;
import ph.agap.app.mic.MicOwner;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.watch.WatchMessages;
import ph.agap.app.watch.WatchSender;

/**
 * One Sound Alerts listening session for the whole app (Phase 11). In 1.0 the Sound Alerts screen
 * owned the engine and stopped it when the screen left the display; now this session owns it, so
 * with background listening on it keeps going after the user leaves AGAP (ListeningService keeps
 * the app running). The screen shows this session's state. Each alert goes to AlertPresenter.
 * Main thread only.
 */
public final class AlertsSession implements SoundAlertEngine.Listener, MicOwner.User {

    @Nullable private static AlertsSession instance;

    private final Context app;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    @Nullable private AnnexA annex;
    @Nullable private SoundAlertEngine engine;

    private boolean heardReady;
    private float threshold;
    private int thresholdPercent;
    @Nullable private AlertEvent lastEvent;
    private int sessionCount;
    @Nullable private String micNote;
    @Nullable private String failure;
    private boolean micSilenced;
    @Nullable private AlertDetector.Result lastWindow;
    private String topClass = "";
    private float topScore;

    private AlertsSession(Context app) {
        this.app = app;
    }

    public static AlertsSession get(Context context) {
        if (instance == null) instance = new AlertsSession(context.getApplicationContext());
        return instance;
    }

    // ---- What the screen reads ----

    public boolean isRunning() { return engine != null; }
    public boolean heardReady() { return heardReady; }
    public float threshold() { return threshold; }
    public int thresholdPercent() { return thresholdPercent; }
    @Nullable public AlertEvent lastEvent() { return lastEvent; }
    public int sessionCount() { return sessionCount; }
    @Nullable public String micNote() { return micNote; }
    @Nullable public String failure() { return failure; }
    public boolean micSilenced() { return micSilenced; }
    /** The last audio window's result while listening, for the "closest now" line; null when idle. */
    @Nullable public AlertDetector.Result lastWindow() { return engine == null ? null : lastWindow; }
    public String topClass() { return topClass; }
    public float topScore() { return topScore; }

    /** Called on every change (start, stop, each audio window, each alert). */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /** The Annex A alert list from the app's assets, read once. */
    public AnnexA annex() {
        if (annex == null) {
            try (InputStream in = app.getAssets().open(AnnexA.ASSET)) {
                annex = AnnexA.parse(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + AnnexA.ASSET, e);
            }
        }
        return annex;
    }

    // ---- Starting and stopping ----

    /** Starts listening. The caller has checked the microphone permission and Demonstration Mode. */
    public void start() {
        if (engine != null) return;
        String stopped = MicOwner.take(this);
        // The Filipino captions model (about 1 GB) is not needed while Sound Alerts listens, often
        // for a long session: free it now (Phase 8). Filipino captions load it again on their next Start.
        VoskModelManager.releaseMemory();
        micNote = stopped == null ? null : app.getString(R.string.mic_other_stopped, stopped);
        failure = null;
        micSilenced = false;
        sessionCount = 0;
        heardReady = false;
        lastWindow = null;
        SettingsStore settings = SettingsStore.from(app);
        threshold = settings.getAlertThreshold();
        thresholdPercent = settings.getAlertThresholdPercent();
        engine = new SoundAlertEngine(app, annex());
        engine.start(threshold, this);
        WatchSender.get(app).sendState(WatchMessages.Mode.ALERTS, "");
        ListeningService.update(app);
        changed();
    }

    public void stop() {
        if (engine == null) return;
        engine.stop();
        engine = null;
        ended();
    }

    /** The user moved the threshold in Settings while listening: use it from the next window on. */
    public void thresholdChanged() {
        SettingsStore settings = SettingsStore.from(app);
        threshold = settings.getAlertThreshold();
        thresholdPercent = settings.getAlertThresholdPercent();
        if (engine != null) engine.setThreshold(threshold);
        changed();
    }

    /** Forgets the note about the other feature, once the screen has shown it. */
    public void clearMicNote() {
        micNote = null;
    }

    private void ended() {
        micSilenced = false;
        lastWindow = null;
        MicOwner.release(this);
        WatchSender.get(app).sendState(WatchMessages.Mode.IDLE, "");
        ListeningService.update(app);
        changed();
    }

    private void changed() {
        for (Runnable listener : listeners) listener.run();
        AgapWidget.refresh(app);
    }

    // ---- MicOwner.User ----

    @Override
    public String micUserName() {
        return app.getString(R.string.alerts_title);
    }

    @Override
    public void onMicTaken(String byWhom) {
        stop();
        micNote = app.getString(R.string.mic_taken, byWhom);
        changed();
    }

    // ---- SoundAlertEngine.Listener ----

    @Override
    public void onListening() {
        heardReady = true;
        changed();
    }

    @Override
    public void onWindow(AlertDetector.Result result, String topClass, float topScore) {
        if (engine == null) return;
        lastWindow = result;
        this.topClass = topClass;
        this.topScore = topScore;
        if (result.event != null) {
            lastEvent = result.event;
            sessionCount++;
            // The alert screen, popup or notification, with vibration and the watch; saved once shown.
            AlertPresenter.present(app, result.event, thresholdPercent);
        }
        for (Runnable listener : listeners) listener.run();
    }

    @Override
    public void onMicSilenced(boolean silenced) {
        micSilenced = silenced;
        changed();
    }

    @Override
    public void onFailed(String reason) {
        engine = null;
        failure = reason;
        ended();
    }
}
