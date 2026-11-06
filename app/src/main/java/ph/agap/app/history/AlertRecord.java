package ph.agap.app.history;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import ph.agap.app.alerts.AlertEvent;

/**
 * One alert in the Event History (contract 1.6), as saved on the phone. Both times are the
 * phone's clock (System.currentTimeMillis, milliseconds since 1970), so the response time is
 * simply shown minus detected. Demonstration Mode alerts are never saved.
 */
@Entity(tableName = "alert_events", indices = {@Index("detectedAtMillis")})
public class AlertRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** The Annex A alert, such as "Doorbell". */
    @NonNull public String label = "";

    /** The Annex A category name: "Emergency", "Traffic" or "Household/Entry". */
    @NonNull public String category = "";

    /** The score that raised the alert, 0 to 1. */
    public float confidence;

    /** The YAMNet class with the highest score for this alert, such as "Ding-dong". For tuning. */
    @NonNull public String yamnetClass = "";

    /** The alert threshold in Settings when the alert was raised, in percent. */
    public int thresholdPercent;

    /** When the score crossed the threshold. */
    public long detectedAtMillis;

    /** When the alert screen's first frame with this alert was handed to the display. */
    public long shownAtMillis;

    public AlertRecord() { }

    public static AlertRecord of(AlertEvent event, long shownAtMillis, int thresholdPercent) {
        AlertRecord record = new AlertRecord();
        record.label = event.label();
        record.category = event.category().displayName;
        record.confidence = event.confidence;
        record.yamnetClass = event.yamnetClass;
        record.thresholdPercent = thresholdPercent;
        record.detectedAtMillis = event.detectedAtMillis;
        record.shownAtMillis = shownAtMillis;
        return record;
    }

    /** Alert shown minus sound detected, in milliseconds. */
    public long responseTimeMillis() {
        return shownAtMillis - detectedAtMillis;
    }
}
