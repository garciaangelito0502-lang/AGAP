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

    /** Starts the category's pattern, replacing any pattern still playing. */
    public void play(AlertCategory category) {
        stop();
        if (!willVibrate()) return;
        VibrationEffect effect = VibrationEffect.createWaveform(
                VibrationPatterns.timings(category), VibrationPatterns.repeatIndex(category));
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        } else {
            vibrator.vibrate(effect, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
        if (VibrationPatterns.repeats(category)) {
            main.postDelayed(stopRepeating, VibrationPatterns.MAX_REPEAT_MILLIS);
        }
        Log.d(TAG, "vibrate " + category.displayName);
    }

    public void stop() {
        main.removeCallbacks(stopRepeating);
        if (vibrator != null) vibrator.cancel();
    }
}
