package ph.agap.app.alerts;

import java.util.Collections;
import java.util.List;

/** One of the 13 Annex A alerts, such as "Doorbell", with the YAMNet classes that raise it. */
public final class AlertType {

    public final String label;
    public final AlertCategory category;
    /** YAMNet class names, exactly as in the model's label list. */
    public final List<String> yamnetClasses;

    AlertType(String label, AlertCategory category, List<String> yamnetClasses) {
        this.label = label;
        this.category = category;
        this.yamnetClasses = Collections.unmodifiableList(yamnetClasses);
    }

    @Override
    public String toString() {
        return label;
    }
}
