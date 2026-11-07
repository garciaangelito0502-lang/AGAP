package ph.agap.app.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The pulse's rhythm: rest, far end after one half period, rest again after two (Phase 8, K-028). */
public class SlowPulseTest {

    private static final float EPS = 1e-6f;

    @Test
    public void startsAtRest() {
        assertEquals(0f, SlowPulse.amountAt(0, 1000), EPS);
    }

    @Test
    public void reachesTheFarEndAfterHalfAPeriod() {
        assertEquals(0.5f, SlowPulse.amountAt(500, 1000), EPS);
        assertEquals(1f, SlowPulse.amountAt(1000, 1000), EPS);
    }

    @Test
    public void comesBackAndRepeats() {
        assertEquals(0.5f, SlowPulse.amountAt(1500, 1000), EPS);
        assertEquals(0f, SlowPulse.amountAt(2000, 1000), EPS);
        assertEquals(0.25f, SlowPulse.amountAt(2250, 1000), EPS);
    }

    @Test
    public void alertBadgePulsesOncePerSecond() {
        // The alert badge uses a 500 ms half period: largest at 0.5 s, at rest at 1 s.
        assertEquals(1f, SlowPulse.amountAt(500, 500), EPS);
        assertEquals(0f, SlowPulse.amountAt(1000, 500), EPS);
    }
}
