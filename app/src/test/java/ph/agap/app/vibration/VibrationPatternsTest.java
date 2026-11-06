package ph.agap.app.vibration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ph.agap.app.alerts.AlertCategory;

public class VibrationPatternsTest {

    /** The "on" (buzz) lengths of one play of the pattern. */
    private static List<Long> buzzes(AlertCategory category) {
        long[] t = VibrationPatterns.timings(category);
        List<Long> on = new ArrayList<>();
        for (int i = 1; i < t.length; i += 2) on.add(t[i]);
        return on;
    }

    @Test
    public void everyPatternStartsAtOnceAndAlternatesOffAndOn() {
        for (AlertCategory category : AlertCategory.values()) {
            long[] t = VibrationPatterns.timings(category);
            assertEquals(category + " starts without delay", 0L, t[0]);
            for (long value : t) assertTrue(category + " has no negative timing", value >= 0);
        }
    }

    @Test
    public void emergencyIsThreeEqualLongBuzzesAndRepeats() {
        List<Long> on = buzzes(AlertCategory.EMERGENCY);
        assertEquals(3, on.size());
        assertEquals(on.get(0), on.get(1));
        assertEquals(on.get(1), on.get(2));
        assertTrue(on.get(0) >= 500);
        assertTrue(VibrationPatterns.repeats(AlertCategory.EMERGENCY));
        assertEquals(0, VibrationPatterns.repeatIndex(AlertCategory.EMERGENCY));
    }

    @Test
    public void trafficIsThreePairsOfShortBuzzes() {
        List<Long> on = buzzes(AlertCategory.TRAFFIC);
        assertEquals(6, on.size());
        for (long buzz : on) assertTrue("short buzz", buzz <= 200);
        assertFalse(VibrationPatterns.repeats(AlertCategory.TRAFFIC));
        assertEquals(-1, VibrationPatterns.repeatIndex(AlertCategory.TRAFFIC));
    }

    @Test
    public void householdIsTwoLongShortPairs() {
        List<Long> on = buzzes(AlertCategory.HOUSEHOLD);
        assertEquals(4, on.size());
        assertTrue(on.get(0) > 2 * on.get(1));
        assertTrue(on.get(2) > 2 * on.get(3));
        assertFalse(VibrationPatterns.repeats(AlertCategory.HOUSEHOLD));
    }

    @Test
    public void buzzesAreLongEnoughToFeel() {
        for (AlertCategory category : AlertCategory.values()) {
            for (long buzz : buzzes(category)) assertTrue(category + " buzz " + buzz, buzz >= 100);
        }
    }

    @Test
    public void onePlayIsShort() {
        for (AlertCategory category : AlertCategory.values()) {
            long ms = VibrationPatterns.durationMillis(category);
            assertTrue(category + " lasts " + ms + " ms", ms > 0 && ms <= 4_000);
        }
    }

    @Test
    public void thePatternsDiffer() {
        assertFalse(Arrays.equals(VibrationPatterns.timings(AlertCategory.EMERGENCY),
                VibrationPatterns.timings(AlertCategory.TRAFFIC)));
        assertFalse(Arrays.equals(VibrationPatterns.timings(AlertCategory.TRAFFIC),
                VibrationPatterns.timings(AlertCategory.HOUSEHOLD)));
        assertFalse(Arrays.equals(VibrationPatterns.timings(AlertCategory.EMERGENCY),
                VibrationPatterns.timings(AlertCategory.HOUSEHOLD)));
    }

    @Test
    public void callersGetACopy() {
        long[] first = VibrationPatterns.timings(AlertCategory.TRAFFIC);
        first[1] = 9_999;
        assertArrayEquals(new long[]{0, 150, 120, 150, 600, 150, 120, 150, 600, 150, 120, 150},
                VibrationPatterns.timings(AlertCategory.TRAFFIC));
    }
}
