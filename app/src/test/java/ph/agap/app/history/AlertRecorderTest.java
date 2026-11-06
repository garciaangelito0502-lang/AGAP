package ph.agap.app.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.alerts.TestAlerts;

public class AlertRecorderTest {

    private final List<AlertRecord> saved = new ArrayList<>();
    private final AlertRecorder recorder = new AlertRecorder(saved::add);

    private static AlertEvent doorbell(long detectedAt) {
        return TestAlerts.event("Doorbell", AlertCategory.HOUSEHOLD, "Ding-dong", 0.88f, detectedAt);
    }

    @Test
    public void demoAlertsAreNotSaved() {
        AlertRecord result = recorder.shown(doorbell(1_000L), 1_118L, 50, true);
        assertNull(result);
        assertTrue(saved.isEmpty());
    }

    @Test
    public void realAlertsAreSavedWithBothTimes() {
        AlertRecord result = recorder.shown(doorbell(1_762_500_340_118L), 1_762_500_340_236L, 50, false);
        assertNotNull(result);
        assertEquals(1, saved.size());
        AlertRecord record = saved.get(0);
        assertSame(result, record);
        assertEquals("Doorbell", record.label);
        assertEquals("Household/Entry", record.category);
        assertEquals("Ding-dong", record.yamnetClass);
        assertEquals(0.88f, record.confidence, 0f);
        assertEquals(50, record.thresholdPercent);
        assertEquals(1_762_500_340_118L, record.detectedAtMillis);
        assertEquals(1_762_500_340_236L, record.shownAtMillis);
        assertEquals(118L, record.responseTimeMillis());
    }

    @Test
    public void everyRealAlertIsSavedInOrder() {
        // False alerts are saved too: the group counts them as false positives.
        recorder.shown(doorbell(1_000L), 1_100L, 50, false);
        recorder.shown(TestAlerts.event("Phone Ringing", AlertCategory.HOUSEHOLD, "Telephone", 0.51f, 2_000L),
                2_090L, 50, false);
        recorder.shown(doorbell(3_000L), 3_050L, 50, true);
        assertEquals(2, saved.size());
        assertEquals("Doorbell", saved.get(0).label);
        assertEquals("Phone Ringing", saved.get(1).label);
    }
}
