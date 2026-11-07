package ph.agap.app.ui;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

/**
 * A slow pulse for one small view (the listening dot, the alert badge), drawn in steps about ten
 * times a second instead of on every display frame. A smooth ObjectAnimator that never ends makes
 * Android redraw the screen up to 120 times a second for as long as listening lasts: on my phone that
 * cost about 44% of one processor core (Phase 8, K-028). Ten steps a second look the same on a small
 * view and cost a fraction. With the phone's animations turned off, the view stays still.
 * Main thread only.
 */
public final class SlowPulse {

    private static final long STEP_MS = 100;

    /** Sets the view for a point in the pulse: 0 is the resting look, 1 the far end. */
    private interface Look {
        void apply(View view, float amount);
    }

    private final View view;
    private final long halfPeriodMs;
    private final Look look;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable step = this::step;
    private boolean started;
    private long startedAt;

    private SlowPulse(View view, long halfPeriodMs, Look look) {
        this.view = view;
        this.halfPeriodMs = halfPeriodMs;
        this.look = look;
    }

    /** Fades from fully visible to {@code lowest} alpha and back; one way takes {@code halfPeriodMs}. */
    public static SlowPulse alpha(View view, float lowest, long halfPeriodMs) {
        return new SlowPulse(view, halfPeriodMs, (v, a) -> v.setAlpha(1f - (1f - lowest) * a));
    }

    /** Grows to {@code largest} times its size and back; one way takes {@code halfPeriodMs}. */
    public static SlowPulse scale(View view, float largest, long halfPeriodMs) {
        return new SlowPulse(view, halfPeriodMs, (v, a) -> {
            float s = 1f + (largest - 1f) * a;
            v.setScaleX(s);
            v.setScaleY(s);
        });
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        if (started) return;
        started = true;
        startedAt = SystemClock.uptimeMillis();
        step();
    }

    /** Stops and puts the view back to its resting look. */
    public void stop() {
        started = false;
        main.removeCallbacks(step);
        look.apply(view, 0f);
    }

    private void step() {
        if (!started) return;
        if (!ValueAnimator.areAnimatorsEnabled()) {
            // Animations are off on this phone (a user setting, also used by UI tests): keep still.
            look.apply(view, 0f);
            return;
        }
        look.apply(view, amountAt(SystemClock.uptimeMillis() - startedAt, halfPeriodMs));
        main.postDelayed(step, STEP_MS);
    }

    /** Where the pulse is after {@code elapsedMs}: 0 at rest, rising to 1 after one half period, back to 0. */
    static float amountAt(long elapsedMs, long halfPeriodMs) {
        long t = elapsedMs % (2 * halfPeriodMs);
        return t < halfPeriodMs ? t / (float) halfPeriodMs : 2f - t / (float) halfPeriodMs;
    }
}
