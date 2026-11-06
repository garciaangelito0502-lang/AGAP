package ph.agap.app.alerts;

import java.util.Collections;

/** Builds alert objects for tests in other packages (AlertType's constructor is package-private). */
public final class TestAlerts {

    private TestAlerts() { }

    public static AlertEvent event(String label, AlertCategory category, String yamnetClass,
                                   float confidence, long detectedAtMillis) {
        AlertType type = new AlertType(label, category, Collections.singletonList(yamnetClass));
        return new AlertEvent(type, yamnetClass, confidence, detectedAtMillis);
    }
}
