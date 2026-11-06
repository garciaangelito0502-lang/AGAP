package ph.agap.app.history;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.TestAlerts;

public class AlertRecordTest {

    @Test
    public void responseTimeIsShownMinusDetected() {
        AlertRecord record = new AlertRecord();
        record.detectedAtMillis = 1_762_500_340_118L;
        record.shownAtMillis = 1_762_500_340_236L;
        assertEquals(118L, record.responseTimeMillis());
    }

    @Test
    public void responseTimeKeepsMillisecondsOverLongGaps() {
        AlertRecord record = new AlertRecord();
        record.detectedAtMillis = 1_762_500_000_001L;
        record.shownAtMillis = 1_762_500_003_000L;
        assertEquals(2_999L, record.responseTimeMillis());
    }

    @Test
    public void copiesEveryFieldFromTheEvent() {
        AlertRecord record = AlertRecord.of(
                TestAlerts.event("Fire Alarm", AlertCategory.EMERGENCY, "Fire alarm", 0.92f, 10_000L), 10_150L, 35);
        assertEquals("Fire Alarm", record.label);
        assertEquals("Emergency", record.category);
        assertEquals("Fire alarm", record.yamnetClass);
        assertEquals(0.92f, record.confidence, 0f);
        assertEquals(35, record.thresholdPercent);
        assertEquals(10_000L, record.detectedAtMillis);
        assertEquals(10_150L, record.shownAtMillis);
        assertEquals(0L, record.id);
    }
}
