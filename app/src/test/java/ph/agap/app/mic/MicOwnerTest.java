package ph.agap.app.mic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class MicOwnerTest {

    private static final class FakeUser implements MicOwner.User {
        final String name;
        String takenBy;

        FakeUser(String name) {
            this.name = name;
        }

        @Override public String micUserName() { return name; }
        @Override public void onMicTaken(String byWhom) { takenBy = byWhom; }
    }

    private final FakeUser captions = new FakeUser("Live Captions");
    private final FakeUser alerts = new FakeUser("Sound Alerts");

    @After
    public void tearDown() {
        MicOwner.release(captions);
        MicOwner.release(alerts);
    }

    @Test
    public void freeMicrophoneIsTakenQuietly() {
        assertNull(MicOwner.take(captions));
        assertTrue(MicOwner.isHeldBy(captions));
    }

    @Test
    public void takingStopsTheOtherFeature() {
        MicOwner.take(captions);
        assertEquals("Live Captions", MicOwner.take(alerts));
        assertEquals("Sound Alerts", captions.takenBy);
        assertTrue(MicOwner.isHeldBy(alerts));
        assertFalse(MicOwner.isHeldBy(captions));
    }

    @Test
    public void releaseByAFormerHolderDoesNothing() {
        MicOwner.take(captions);
        MicOwner.take(alerts);
        MicOwner.release(captions);
        assertTrue(MicOwner.isHeldBy(alerts));
    }

    @Test
    public void takingTwiceIsHarmless() {
        MicOwner.take(alerts);
        assertNull(MicOwner.take(alerts));
        assertNull(alerts.takenBy);
    }
}
