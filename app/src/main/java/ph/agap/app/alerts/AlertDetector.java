package ph.agap.app.alerts;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns YAMNet scores, one set per audio window, into alert events.
 *
 * For each alert the highest score among its YAMNet classes counts. In one window at most one
 * alert fires: the highest-scoring alert at or above the threshold. A sound that goes on (a ringing
 * alarm) fires once; the same alert fires again only after it has been quiet for QUIET_MS, or after
 * REPEAT_MS if it never stops. Pure Java, so it is unit tested on the computer.
 */
public final class AlertDetector {

    /** How long an alert must stay below the threshold before it can fire again. */
    public static final long QUIET_MS = 3000;
    /** A sound that never stops is alerted again after this long. */
    public static final long REPEAT_MS = 30_000;

    /** The strongest Annex A match in one window, whether or not it reached the threshold. */
    public static final class Best {
        public final AlertType type;
        public final String yamnetClass;
        public final float score;

        Best(AlertType type, String yamnetClass, float score) {
            this.type = type;
            this.yamnetClass = yamnetClass;
            this.score = score;
        }
    }

    /** What one window produced. */
    public static final class Result {
        /** The strongest Annex A match, or null if no mapped class scored above zero. */
        @Nullable public final Best best;
        /** The alert raised by this window, or null. */
        @Nullable public final AlertEvent event;

        Result(@Nullable Best best, @Nullable AlertEvent event) {
            this.best = best;
            this.event = event;
        }
    }

    private final AnnexA annex;
    /** Per alert: when it was last at or above the threshold. */
    private final Map<AlertType, Long> lastAbove = new HashMap<>();
    private final Map<AlertType, Long> lastFired = new HashMap<>();

    public AlertDetector(AnnexA annex) {
        this.annex = annex;
    }

    /**
     * @param scores    YAMNet class name to score (0-1) for one window; other classes are ignored
     * @param threshold the user's alert threshold, 0-1
     * @param nowMillis the phone's clock when the scores came out
     */
    public Result evaluate(Map<String, Float> scores, float threshold, long nowMillis) {
        // Highest score and class for each alert.
        Map<AlertType, Best> perAlert = new HashMap<>();
        for (Map.Entry<String, Float> entry : scores.entrySet()) {
            AlertType type = annex.alertFor(entry.getKey());
            if (type == null || entry.getValue() == null) continue;
            Best known = perAlert.get(type);
            if (known == null || entry.getValue() > known.score) {
                perAlert.put(type, new Best(type, entry.getKey(), entry.getValue()));
            }
        }

        Best strongest = null;
        Best toFire = null;
        for (Best candidate : perAlert.values()) {
            if (candidate.score <= 0f) continue;
            if (strongest == null || candidate.score > strongest.score) strongest = candidate;
            if (candidate.score >= threshold && mayFire(candidate.type, nowMillis)
                    && (toFire == null || candidate.score > toFire.score)) {
                toFire = candidate;
            }
        }
        // Mark every alert at or above the threshold, so a sound that triggers two related alerts
        // (a siren as both Warning Siren and Emergency Vehicle Siren) does not alternate between them.
        for (Best candidate : perAlert.values()) {
            if (candidate.score >= threshold) lastAbove.put(candidate.type, nowMillis);
        }

        AlertEvent event = null;
        if (toFire != null) {
            lastFired.put(toFire.type, nowMillis);
            event = new AlertEvent(toFire.type, toFire.yamnetClass, toFire.score, nowMillis);
        }
        return new Result(strongest, event);
    }

    private boolean mayFire(AlertType type, long now) {
        Long above = lastAbove.get(type);
        if (above == null || now - above > QUIET_MS) return true;
        // The sound is still going on. An alert that never fired for it (another alert was stronger)
        // stays quiet; one that did fire repeats only after REPEAT_MS.
        Long fired = lastFired.get(type);
        return fired != null && now - fired >= REPEAT_MS;
    }

    /** Forgets earlier sounds, for a new listening session. */
    public void reset() {
        lastAbove.clear();
        lastFired.clear();
    }
}
