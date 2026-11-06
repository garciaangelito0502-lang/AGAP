package ph.agap.app.vibration;

import ph.agap.app.alerts.AlertCategory;

/**
 * One vibration pattern per alert category (contract 1.5), from the approved design note:
 * Emergency long-long-long, repeating until the alert is dismissed (for at most a minute);
 * Traffic short-short, pause, three times; Household/Entry long-short, pause, twice.
 * Each is built to be told apart by feel: three equal long buzzes, quick pairs, or long-short pairs.
 * The same data drives the phone's vibration, the bars on the alert screen and the Vibration
 * Patterns guide, and later the watch.
 *
 * Timings are Android waveform timings: off, on, off, on... in milliseconds, starting with an
 * off (delay) of 0.
 */
public final class VibrationPatterns {

    private static final int LONG = 600;
    private static final int SHORT = 150;

    /** Emergency: long, long, long, then a pause; the whole pattern repeats. */
    private static final long[] EMERGENCY = {0, LONG, 250, LONG, 250, LONG, 900};

    /** Traffic: short-short, pause; three times. */
    private static final long[] TRAFFIC = {
            0, SHORT, 120, SHORT, 600,
            SHORT, 120, SHORT, 600,
            SHORT, 120, SHORT};

    /** Household/Entry: long-short, pause; twice. */
    private static final long[] HOUSEHOLD = {
            0, 500, 150, SHORT, 700,
            500, 150, SHORT};

    /** The longest an Emergency vibration repeats when nobody dismisses the alert. */
    public static final long MAX_REPEAT_MILLIS = 60_000;

    private VibrationPatterns() { }

    /** A copy of the pattern's timings (off, on, off, on... ms). */
    public static long[] timings(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return EMERGENCY.clone();
            case TRAFFIC: return TRAFFIC.clone();
            default: return HOUSEHOLD.clone();
        }
    }

    /** True when the pattern repeats until the alert is dismissed (Emergency only). */
    public static boolean repeats(AlertCategory category) {
        return category == AlertCategory.EMERGENCY;
    }

    /** Android's waveform repeat index: 0 to repeat the whole pattern, -1 to play it once. */
    public static int repeatIndex(AlertCategory category) {
        return repeats(category) ? 0 : -1;
    }

    /** One play of the pattern, in milliseconds. */
    public static long durationMillis(AlertCategory category) {
        long total = 0;
        for (long t : timings(category)) total += t;
        return total;
    }
}
