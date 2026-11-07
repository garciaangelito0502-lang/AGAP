package ph.agap.app.watch;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.captions.CaptionRelay;
import ph.agap.app.demo.DemoMode;
import ph.agap.app.settings.SettingsStore;

/**
 * Sends alerts, caption text and what the phone is doing to the AGAP watch app over the Wearable
 * Data Layer (contract 1.11, D-042). Bluetooth through Google Play services; no internet. It only
 * sends when a watch with the AGAP app is connected, never waits for an answer, and does nothing at
 * all on a phone without Google Play services, so it can never slow down or crash the phone app.
 * Main thread only.
 */
public final class WatchSender implements CaptionRelay.Sink, CapabilityClient.OnCapabilityChangedListener,
        MessageClient.OnMessageReceivedListener {

    private static final String TAG = "AGAP/Watch";
    @Nullable private static WatchSender instance;

    private final Context context;
    private final boolean available;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CaptionThrottle throttle = new CaptionThrottle();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private Set<Node> watches = Collections.emptySet();
    private boolean started;
    private String captionLanguage = "";
    private WatchMessages.Mode mode = WatchMessages.Mode.IDLE;
    private String modeLanguage = "";
    @Nullable private String pendingCaption;
    private final Runnable sendPending = this::sendPendingCaption;

    private WatchSender(Context context) {
        this.context = context.getApplicationContext();
        this.available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this.context)
                == ConnectionResult.SUCCESS;
    }

    public static WatchSender get(Context context) {
        if (instance == null) instance = new WatchSender(context);
        return instance;
    }

    /** Starts listening for watches and for caption text. Safe to call more than once. */
    public void start() {
        if (started) return;
        started = true;
        if (!available) {
            Log.i(TAG, "no Google Play services: watch link off");
            return;
        }
        CaptionRelay.setSink(this);
        Wearable.getCapabilityClient(context).addListener(this, WatchMessages.CAPABILITY);
        Wearable.getMessageClient(context).addListener(this);
        refresh();
    }

    /** Looks for connected watches with the AGAP app again (Home calls this when it opens). */
    public void refresh() {
        if (!available) return;
        Wearable.getCapabilityClient(context)
                .getCapability(WatchMessages.CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(info -> setWatches(info.getNodes()))
                .addOnFailureListener(e -> Log.w(TAG, "could not look for watches: " + e));
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo info) {
        setWatches(info.getNodes());
    }

    private void setWatches(Set<Node> nodes) {
        Set<String> known = new HashSet<>();
        for (Node node : watches) known.add(node.getId());
        watches = nodes;
        // A watch that has just connected learns what the phone is doing.
        for (Node node : nodes) {
            if (!known.contains(node.getId())) sendTo(node.getId(), WatchMessages.PATH_STATE, currentState());
        }
        Log.i(TAG, nodes.size() + " AGAP watch(es) connected");
        for (Runnable listener : listeners) listener.run();
    }

    /** The watch screen just opened and asks what the phone is doing. */
    @Override
    public void onMessageReceived(@NonNull MessageEvent event) {
        if (!WatchMessages.PATH_HELLO.equals(event.getPath())) return;
        String node = event.getSourceNodeId();
        main.post(() -> {
            sendTo(node, WatchMessages.PATH_STATE, currentState());
            refresh();
        });
    }

    /** True when Google Play services is on this phone (needed for the watch link). */
    public boolean isAvailable() {
        return available;
    }

    /** Names of the connected watches that have the AGAP app; empty when none. */
    public List<String> connectedWatchNames() {
        List<String> names = new ArrayList<>();
        for (Node node : watches) names.add(node.getDisplayName());
        return names;
    }

    /** Called on the main thread when the set of connected watches changes. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    // ---- What goes to the watch ----

    /** An alert, real or a Demonstration Mode sample; sent as soon as the phone shows it. */
    public void sendAlert(AlertEvent event, boolean demo) {
        send(WatchMessages.PATH_ALERT, WatchMessages.alert(event.label(), event.category(),
                Math.round(event.confidence * 100f), event.detectedAtMillis, demo,
                SettingsStore.from(context).isVibrationOn()));
    }

    /** What the phone is doing now, for the watch's status line. */
    public void sendState(WatchMessages.Mode mode, String languageTag) {
        this.mode = mode;
        this.modeLanguage = languageTag;
        if (mode == WatchMessages.Mode.CAPTIONS) captionLanguage = languageTag;
        if (mode != WatchMessages.Mode.CAPTIONS) {
            pendingCaption = null;
            main.removeCallbacks(sendPending);
        }
        send(WatchMessages.PATH_STATE, currentState());
    }

    private byte[] currentState() {
        return WatchMessages.state(mode, modeLanguage, DemoMode.isOn());
    }

    @Override
    public void onCaption(String text, boolean isFinal) {
        if (watches.isEmpty()) return;
        long now = SystemClock.elapsedRealtime();
        long wait = throttle.delayFor(now, isFinal);
        if (wait == 0) {
            main.removeCallbacks(sendPending);
            pendingCaption = null;
            throttle.sent(now);
            send(WatchMessages.PATH_CAPTION, WatchMessages.caption(text, isFinal, captionLanguage));
        } else {
            boolean scheduled = pendingCaption != null;
            pendingCaption = text;
            if (!scheduled) main.postDelayed(sendPending, wait);
        }
    }

    @Override
    public void onCaptionsStopped() {
        pendingCaption = null;
        main.removeCallbacks(sendPending);
    }

    private void sendPendingCaption() {
        String text = pendingCaption;
        pendingCaption = null;
        if (text == null) return;
        throttle.sent(SystemClock.elapsedRealtime());
        send(WatchMessages.PATH_CAPTION, WatchMessages.caption(text, false, captionLanguage));
    }

    /** To every connected AGAP watch; failures are logged, never thrown. Words are never logged. */
    private void send(String path, byte[] data) {
        if (!available || watches.isEmpty()) return;
        for (Node node : watches) sendTo(node.getId(), path, data);
    }

    private void sendTo(String nodeId, String path, byte[] data) {
        if (!available) return;
        Wearable.getMessageClient(context).sendMessage(nodeId, path, data)
                .addOnFailureListener(e -> Log.w(TAG, "send " + path + " failed: " + e));
    }
}
