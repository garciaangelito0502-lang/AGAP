package ph.agap.app.demo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Demonstration Mode (contract 1.9): sample captions and sample alerts for presentations, with
 * the microphone off and nothing saved. Kept in memory only, on purpose: closing the app or
 * restarting the phone always brings back normal mode, so a test session can never start in
 * Demonstration Mode by accident. Main thread only.
 */
public final class DemoMode {

    private static boolean on;
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DemoMode() { }

    public static boolean isOn() {
        return on;
    }

    public static void setOn(boolean value) {
        if (on == value) return;
        on = value;
        for (Runnable listener : listeners) listener.run();
    }

    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }
}
