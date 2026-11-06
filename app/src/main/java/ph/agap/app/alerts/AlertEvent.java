package ph.agap.app.alerts;

/**
 * One detected Annex A sound. detectedAtMillis is the phone's clock (milliseconds since 1970) at the
 * moment the score crossed the threshold; the research team subtracts it from the time the alert
 * was shown (Phase 5) to get the response time.
 */
public final class AlertEvent {

    public final AlertType type;
    /** The YAMNet class with the highest score for this alert, e.g. "Ding-dong" for Doorbell. */
    public final String yamnetClass;
    /** 0 to 1. */
    public final float confidence;
    public final long detectedAtMillis;

    public AlertEvent(AlertType type, String yamnetClass, float confidence, long detectedAtMillis) {
        this.type = type;
        this.yamnetClass = yamnetClass;
        this.confidence = confidence;
        this.detectedAtMillis = detectedAtMillis;
    }

    public String label() {
        return type.label;
    }

    public AlertCategory category() {
        return type.category;
    }
}
