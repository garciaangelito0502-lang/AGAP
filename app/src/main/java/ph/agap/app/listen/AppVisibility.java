package ph.agap.app.listen;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Knows whether any AGAP screen is in view, and which screen can show the full-screen alert over
 * itself (Phase 11). With AGAP in view, alerts appear as in 1.0; without it, AlertPresenter uses the
 * lock screen, a popup or a notification. Main thread only.
 */
public final class AppVisibility implements Application.ActivityLifecycleCallbacks {

    /** A screen that can show the full-screen alert over itself: MainActivity and AlertActivity. */
    public interface AlertHost { }

    private static final AppVisibility INSTANCE = new AppVisibility();

    private int started;
    /** Started alert hosts, the newest last. */
    private final List<FragmentActivity> hosts = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private AppVisibility() { }

    static void install(Application app) {
        app.registerActivityLifecycleCallbacks(INSTANCE);
    }

    /** True while at least one AGAP screen is started (in view, or about to be). */
    public static boolean isAppVisible() {
        return INSTANCE.started > 0;
    }

    /** The newest started screen that can show an alert now, or null. */
    @Nullable
    public static FragmentActivity alertHost() {
        List<FragmentActivity> hosts = INSTANCE.hosts;
        for (int i = hosts.size() - 1; i >= 0; i--) {
            FragmentActivity activity = hosts.get(i);
            if (!activity.isFinishing() && !activity.getSupportFragmentManager().isStateSaved()) {
                return activity;
            }
        }
        return null;
    }

    /** Called when AGAP comes into view or leaves it. */
    public static void addListener(Runnable listener) {
        INSTANCE.listeners.add(listener);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        started++;
        if (activity instanceof AlertHost && activity instanceof FragmentActivity) {
            hosts.remove(activity);
            hosts.add((FragmentActivity) activity);
        }
        if (started == 1) changed();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        started = Math.max(0, started - 1);
        hosts.remove(activity);
        if (started == 0) changed();
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        hosts.remove(activity);
    }

    private void changed() {
        for (Runnable listener : listeners) listener.run();
    }

    @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) { }
    @Override public void onActivityResumed(@NonNull Activity a) { }
    @Override public void onActivityPaused(@NonNull Activity a) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) { }
}
