package ph.agap.app.vibration;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;

import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.settings.SettingsStore;

/**
 * Plays an alert's category pattern, if vibration is on in Settings (contract 1.7). Marked as an
 * alarm, so the phone vibrates even when it is set to silent: for a deaf or hard-of-hearing user
 * the vibration is the alert. Main thread only.
 */
public final class AlertVibrator {

    private static final String TAG = "AGAP/Alerts";

    private final Context context;
    @Nullable private final Vibrator vibrator;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable stopRepeating = this::stop;

    public AlertVibrator(Context context) {
        this.context = context.getApplicationContext();
        Vibrator v = this.context.getSystemService(Vibrator.class);
        this.vibrator = v != null && v.hasVibrator() ? v : null;
    }

    /** True when this alert will vibrate: the phone has a vibrator and vibration is on in Settings. */
    public boolean willVibrate() {
        return vibrator != null && SettingsStore.from(context).isVibrationOn();
    }

    /** True when the phone has a vibrator at all. */
    public boolean canVibrate() {
        return vibrator != null;
    }

    /** Starts the category's pattern, replacing any pattern still playing. */
    public void play(AlertCategory category) {
        stop();
        if (!willVibrate()) return;
        vibrate(category, VibrationPatterns.repeatIndex(category));
        if (VibrationPatterns.repeats(category)) {
            main.postDelayed(stopRepeating, VibrationPatterns.MAX_REPEAT_MILLIS);
        }
        Log.d(TAG, "vibrate " + category.displayName);
    }

    /**
     * One play of the pattern, for the Vibration Patterns guide's Try button. Plays even when
     * Vibration is off in Settings, because the user asked to feel it; Emergency does not repeat.
     */
    public void preview(AlertCategory category) {
        stop();
        if (vibrator == null) return;
        vibrate(category, -1);
        Log.d(TAG, "preview " + category.displayName);
    }

    private void vibrate(AlertCategory category, int repeatIndex) {
        VibrationEffect effect = VibrationEffect.createWaveform(VibrationPatterns.timings(category), repeatIndex);
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        } else {
            vibrator.vibrate(effect, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
    }

    public void stop() {
        main.removeCallbacks(stopRepeating);
        if (vibrator != null) vibrator.cancel();
    }
}
