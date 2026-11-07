package ph.agap.app.watch;

/**
 * Decides when caption text goes to the watch. Finished lines go at once; words still being heard
 * change several times a second, so they go at most every {@link #MIN_GAP_MS} and the newest
 * text waits for the next slot. Plain Java, so it is tested on the computer.
 */
final class CaptionThrottle {

    static final long MIN_GAP_MS = 250;

    private long lastSentAt = Long.MIN_VALUE / 2;

    /**
     * @return 0 to send now, or how many milliseconds to wait before sending the newest text
     */
    long delayFor(long nowMillis, boolean isFinal) {
        if (isFinal) return 0;
        long wait = lastSentAt + MIN_GAP_MS - nowMillis;
        return Math.max(0, wait);
    }

    /** Call when a caption was actually sent. */
    void sent(long nowMillis) {
        lastSentAt = nowMillis;
    }
}
