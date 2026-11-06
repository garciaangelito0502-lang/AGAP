package ph.agap.app.alerts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AlertDetectorTest {

    private static final float THRESHOLD = 0.5f;
    private AlertDetector detector;

    @Before
    public void setUp() throws IOException {
        String tsv = "Emergency\tWarning Siren\tCivil defense siren\n"
                + "Emergency\tWarning Siren\tSiren\n"
                + "Emergency\tEmergency Vehicle Siren\tAmbulance (siren)\n"
                + "Household/Entry\tDoorbell\tDoorbell\n"
                + "Household/Entry\tDoorbell\tDing-dong\n"
                + "Household/Entry\tKnocking\tKnock\n";
        detector = new AlertDetector(AnnexA.parse(new ByteArrayInputStream(tsv.getBytes(StandardCharsets.UTF_8))));
    }

    private static Map<String, Float> scores(Object... pairs) {
        Map<String, Float> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], (Float) pairs[i + 1]);
        return map;
    }

    @Test
    public void belowThresholdNoEventButBestIsReported() {
        AlertDetector.Result r = detector.evaluate(scores("Doorbell", 0.3f, "Speech", 0.9f), THRESHOLD, 1000);
        assertNull(r.event);
        assertNotNull(r.best);
        assertEquals("Doorbell", r.best.type.label);
        assertEquals(0.3f, r.best.score, 1e-6);
    }

    @Test
    public void atThresholdFiresWithTimeAndHighestClass() {
        AlertDetector.Result r = detector.evaluate(scores("Doorbell", 0.5f, "Ding-dong", 0.7f), THRESHOLD, 1234);
        assertNotNull(r.event);
        assertEquals("Doorbell", r.event.label());
        assertEquals("Ding-dong", r.event.yamnetClass);
        assertEquals(0.7f, r.event.confidence, 1e-6);
        assertEquals(1234, r.event.detectedAtMillis);
        assertEquals(AlertCategory.HOUSEHOLD, r.event.category());
    }

    @Test
    public void onlyTheStrongestAlertFiresInOneWindow() {
        AlertDetector.Result r = detector.evaluate(scores("Knock", 0.6f, "Doorbell", 0.8f), THRESHOLD, 1000);
        assertEquals("Doorbell", r.event.label());
    }

    @Test
    public void aContinuingSoundFiresOnce() {
        assertNotNull(detector.evaluate(scores("Siren", 0.9f), THRESHOLD, 0).event);
        for (long t = 500; t <= 20_000; t += 500) {
            assertNull("fired again at " + t, detector.evaluate(scores("Siren", 0.9f), THRESHOLD, t).event);
        }
    }

    @Test
    public void relatedAlertsDoNotTakeTurns() {
        // A siren scores as both alerts; after the first fires, the other must not fire while it goes on.
        assertEquals("Warning Siren",
                detector.evaluate(scores("Siren", 0.9f, "Ambulance (siren)", 0.6f), THRESHOLD, 0).event.label());
        assertNull(detector.evaluate(scores("Siren", 0.5f, "Ambulance (siren)", 0.8f), THRESHOLD, 500).event);
    }

    @Test
    public void firesAgainAfterQuiet() {
        assertNotNull(detector.evaluate(scores("Knock", 0.9f), THRESHOLD, 0).event);
        detector.evaluate(scores("Knock", 0.1f), THRESHOLD, 500);
        assertNull(detector.evaluate(scores("Knock", 0.9f), THRESHOLD, AlertDetector.QUIET_MS).event);
        // The last time above the threshold was just now; after a quiet gap it may fire again.
        long later = AlertDetector.QUIET_MS + AlertDetector.QUIET_MS + 1;
        assertNotNull(detector.evaluate(scores("Knock", 0.9f), THRESHOLD, later).event);
    }

    @Test
    public void aSoundThatNeverStopsIsRepeated() {
        assertNotNull(detector.evaluate(scores("Siren", 0.9f), THRESHOLD, 0).event);
        AlertDetector.Result r = null;
        for (long t = 500; t <= AlertDetector.REPEAT_MS; t += 500) {
            r = detector.evaluate(scores("Siren", 0.9f), THRESHOLD, t);
            if (r.event != null) break;
        }
        assertNotNull(r.event);
        assertEquals(AlertDetector.REPEAT_MS, r.event.detectedAtMillis);
    }

    @Test
    public void otherAlertsAreNotBlocked() {
        assertNotNull(detector.evaluate(scores("Siren", 0.9f), THRESHOLD, 0).event);
        assertEquals("Doorbell",
                detector.evaluate(scores("Siren", 0.9f, "Doorbell", 0.6f), THRESHOLD, 500).event.label());
    }

    @Test
    public void unmappedClassesAreIgnored() {
        AlertDetector.Result r = detector.evaluate(scores("Music", 1.0f), THRESHOLD, 0);
        assertNull(r.best);
        assertNull(r.event);
    }

    @Test
    public void resetForgetsEarlierSounds() {
        assertNotNull(detector.evaluate(scores("Knock", 0.9f), THRESHOLD, 0).event);
        detector.reset();
        assertNotNull(detector.evaluate(scores("Knock", 0.9f), THRESHOLD, 500).event);
    }
}
