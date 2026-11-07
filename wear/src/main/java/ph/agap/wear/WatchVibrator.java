package ph.agap.wear;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build;
import android.media.AudioAttributes;

import androidx.annotation.Nullable;

import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.vibration.VibrationPatterns;

/**
 * The same category patterns as the phone (VibrationPatterns, shared), as an alarm so the watch
 * vibrates even on silent. Main thread only.
 */
final class WatchVibrator {

    @Nullable private final Vibrator vibrator;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable stopRepeating = this::stop;

    WatchVibrator(Context context) {
        Vibrator v = context.getSystemService(Vibrator.class);
        vibrator = v != null && v.hasVibrator() ? v : null;
    }

    @SuppressWarnings("deprecation") // the AudioAttributes form is only used before Wear OS 4 (API 33)
    void play(AlertCategory category) {
        stop();
        if (vibrator == null) return;
        VibrationEffect effect = VibrationEffect.createWaveform(
                VibrationPatterns.timings(category), VibrationPatterns.repeatIndex(category));
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        } else {
            vibrator.vibrate(effect, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        }
        if (VibrationPatterns.repeats(category)) {
            main.postDelayed(stopRepeating, VibrationPatterns.MAX_REPEAT_MILLIS);
        }
    }

    void stop() {
        main.removeCallbacks(stopRepeating);
        if (vibrator != null) vibrator.cancel();
    }
}
