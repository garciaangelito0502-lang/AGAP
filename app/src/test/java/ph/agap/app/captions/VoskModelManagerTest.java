package ph.agap.app.captions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VoskModelManagerTest {

    private static final long GB = 1_000_000_000L;
    private static final long NEED = VoskModelSpec.FILIPINO.requiredFreeBytes;

    @Test
    public void installedModelIsReadyWhateverTheFreeSpace() {
        assertEquals(VoskModelManager.State.INSTALLED, VoskModelManager.stateFor(true, 0, NEED));
    }

    @Test
    public void missingModelWithRoomCanBeSetUp() {
        assertEquals(VoskModelManager.State.NOT_INSTALLED, VoskModelManager.stateFor(false, 5 * GB, NEED));
        assertEquals(VoskModelManager.State.NOT_INSTALLED, VoskModelManager.stateFor(false, NEED, NEED));
    }

    @Test
    public void missingModelWithoutRoomIsLowStorage() {
        assertEquals(VoskModelManager.State.LOW_STORAGE, VoskModelManager.stateFor(false, NEED - 1, NEED));
        assertEquals(VoskModelManager.State.LOW_STORAGE, VoskModelManager.stateFor(false, 0, NEED));
    }

    @Test
    public void filipinoNeedsAboutOneGigabyteWhichCoversTheUnpackedModel() {
        assertTrue(VoskModelSpec.FILIPINO.requiredFreeBytes >= GB);
        assertTrue(VoskModelSpec.FILIPINO.requiredFreeBytes > VoskModelSpec.FILIPINO.unpackedBytes);
    }

    @Test
    public void englishFallbackLeavesHeadroomToo() {
        assertTrue(VoskModelSpec.ENGLISH.requiredFreeBytes > VoskModelSpec.ENGLISH.unpackedBytes);
    }

    @Test
    public void theModelsAreDifferentLanguagesAndAssets() {
        assertEquals("fil-PH", VoskModelSpec.FILIPINO.languageTag);
        assertEquals("en-US", VoskModelSpec.ENGLISH.languageTag);
        assertEquals("vosk-model-tl-ph-generic-0.6.zip", VoskModelSpec.FILIPINO.asset());
        assertEquals("vosk-model-small-en-us-0.15.zip", VoskModelSpec.ENGLISH.asset());
    }
}
