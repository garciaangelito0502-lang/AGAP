package ph.agap.app.watch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CaptionThrottleTest {

    @Test
    public void firstPartialGoesAtOnce() {
        assertEquals(0, new CaptionThrottle().delayFor(10_000, false));
    }

    @Test
    public void partialsWaitForTheNextSlot() {
        CaptionThrottle t = new CaptionThrottle();
        t.sent(10_000);
        assertEquals(CaptionThrottle.MIN_GAP_MS, t.delayFor(10_000, false));
        assertEquals(150, t.delayFor(10_100, false));
        assertEquals(0, t.delayFor(10_250, false));
        assertEquals(0, t.delayFor(11_000, false));
    }

    @Test
    public void finishedLinesNeverWait() {
        CaptionThrottle t = new CaptionThrottle();
        t.sent(10_000);
        assertEquals(0, t.delayFor(10_001, true));
    }

    @Test
    public void atMostFourPartialsASecond() {
        CaptionThrottle t = new CaptionThrottle();
        int sent = 0;
        for (long now = 0; now < 1_000; now += 20) {  // a new partial every 20 ms
            if (t.delayFor(now, false) == 0) {
                t.sent(now);
                sent++;
            }
        }
        assertEquals(4, sent);
    }
}
