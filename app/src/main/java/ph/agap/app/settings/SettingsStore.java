package ph.agap.app.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The four user settings (contract 1.7), stored on the phone in SharedPreferences.
 * Values read back are always clamped to their valid range, so a damaged or old file
 * cannot put the app in a state the screens do not expect.
 */
public final class SettingsStore {

    static final String FILE_NAME = "agap_settings";
    static final String KEY_TEXT_SIZE = "text_size_level";
    static final String KEY_HIGH_CONTRAST = "high_contrast";
    static final String KEY_VIBRATION = "vibration_on";
    static final String KEY_THRESHOLD = "alert_threshold_percent";

    /** Text size steps, as multipliers of the phone's own font size. */
    private static final float[] TEXT_SCALES = {0.85f, 1.0f, 1.2f, 1.45f, 1.75f};
    public static final int TEXT_SIZE_DEFAULT = 1;

    /** Alert threshold in whole percent; Sound Alerts (Phase 3) uses it as a score from 0 to 1. */
    public static final int THRESHOLD_MIN = 10;
    public static final int THRESHOLD_MAX = 90;
    public static final int THRESHOLD_STEP = 5;
    public static final int THRESHOLD_DEFAULT = 50;

    private final SharedPreferences prefs;

    public SettingsStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static SettingsStore from(Context context) {
        return new SettingsStore(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE));
    }

    public static int textSizeLevelCount() {
        return TEXT_SCALES.length;
    }

    public static float textScaleFor(int level) {
        return TEXT_SCALES[clamp(level, 0, TEXT_SCALES.length - 1)];
    }

    public int getTextSizeLevel() {
        return clamp(prefs.getInt(KEY_TEXT_SIZE, TEXT_SIZE_DEFAULT), 0, TEXT_SCALES.length - 1);
    }

    public void setTextSizeLevel(int level) {
        prefs.edit().putInt(KEY_TEXT_SIZE, clamp(level, 0, TEXT_SCALES.length - 1)).apply();
    }

    public float getTextScale() {
        return TEXT_SCALES[getTextSizeLevel()];
    }

    public boolean isHighContrast() {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false);
    }

    public void setHighContrast(boolean on) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, on).apply();
    }

    public boolean isVibrationOn() {
        return prefs.getBoolean(KEY_VIBRATION, true);
    }

    public void setVibrationOn(boolean on) {
        prefs.edit().putBoolean(KEY_VIBRATION, on).apply();
    }

    public int getAlertThresholdPercent() {
        return normaliseThreshold(prefs.getInt(KEY_THRESHOLD, THRESHOLD_DEFAULT));
    }

    public void setAlertThresholdPercent(int percent) {
        prefs.edit().putInt(KEY_THRESHOLD, normaliseThreshold(percent)).apply();
    }

    /** The threshold as a classifier score between 0 and 1. */
    public float getAlertThreshold() {
        return getAlertThresholdPercent() / 100f;
    }

    /** Changes whenever a setting that changes how screens look (text size, display mode) changes. */
    public String lookKey() {
        return getTextSizeLevel() + (isHighContrast() ? "-hc" : "-standard");
    }

    static int normaliseThreshold(int percent) {
        int rounded = Math.round(percent / (float) THRESHOLD_STEP) * THRESHOLD_STEP;
        return clamp(rounded, THRESHOLD_MIN, THRESHOLD_MAX);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
