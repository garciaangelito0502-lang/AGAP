package ph.agap.app.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class SettingsStoreTest {

    private FakeSharedPreferences prefs;
    private SettingsStore store;

    @Before
    public void setUp() {
        prefs = new FakeSharedPreferences();
        store = new SettingsStore(prefs);
    }

    @Test
    public void defaultsOnFirstStart() {
        assertEquals(SettingsStore.TEXT_SIZE_DEFAULT, store.getTextSizeLevel());
        assertEquals(1.0f, store.getTextScale(), 0.0001f);
        assertFalse(store.isHighContrast());
        assertTrue(store.isVibrationOn());
        assertEquals(50, store.getAlertThresholdPercent());
        assertEquals(0.5f, store.getAlertThreshold(), 0.0001f);
    }

    @Test
    public void valuesSurviveANewStoreOnTheSameFile() {
        store.setTextSizeLevel(3);
        store.setHighContrast(true);
        store.setVibrationOn(false);
        store.setAlertThresholdPercent(70);

        // Same as the app being closed and opened again: a fresh store reads the saved values.
        SettingsStore reopened = new SettingsStore(prefs);
        assertEquals(3, reopened.getTextSizeLevel());
        assertTrue(reopened.isHighContrast());
        assertFalse(reopened.isVibrationOn());
        assertEquals(70, reopened.getAlertThresholdPercent());
    }

    @Test
    public void textSizeIsKeptInRange() {
        store.setTextSizeLevel(99);
        assertEquals(SettingsStore.textSizeLevelCount() - 1, store.getTextSizeLevel());
        store.setTextSizeLevel(-5);
        assertEquals(0, store.getTextSizeLevel());
    }

    @Test
    public void thresholdIsRoundedToStepsAndKeptInRange() {
        store.setAlertThresholdPercent(62);
        assertEquals(60, store.getAlertThresholdPercent());
        store.setAlertThresholdPercent(5);
        assertEquals(SettingsStore.THRESHOLD_MIN, store.getAlertThresholdPercent());
        store.setAlertThresholdPercent(100);
        assertEquals(SettingsStore.THRESHOLD_MAX, store.getAlertThresholdPercent());
    }

    @Test
    public void badValueInTheFileIsClampedOnRead() {
        prefs.edit().putInt(SettingsStore.KEY_THRESHOLD, 400).putInt(SettingsStore.KEY_TEXT_SIZE, 12).apply();
        assertEquals(SettingsStore.THRESHOLD_MAX, store.getAlertThresholdPercent());
        assertEquals(SettingsStore.textSizeLevelCount() - 1, store.getTextSizeLevel());
    }

    @Test
    public void lookKeyChangesOnlyForSettingsThatChangeTheScreens() {
        String before = store.lookKey();
        store.setVibrationOn(false);
        store.setAlertThresholdPercent(80);
        assertEquals(before, store.lookKey());

        store.setHighContrast(true);
        assertNotEquals(before, store.lookKey());
    }
}
