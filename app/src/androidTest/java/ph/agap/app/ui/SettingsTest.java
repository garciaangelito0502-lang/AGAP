package ph.agap.app.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ph.agap.app.ui.TestSupport.hasBackgroundColor;
import static ph.agap.app.ui.TestSupport.setSliderValue;
import static ph.agap.app.ui.TestSupport.tapAt;
import static ph.agap.app.ui.TestSupport.textSizeAbove;

import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import ph.agap.app.R;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.settings.SettingsActivity;

/**
 * The four settings (contract 1.7) change what the app does, not just what Settings shows. The
 * phone's own settings are saved first and put back after each test.
 */
@RunWith(AndroidJUnit4.class)
public class SettingsTest {

    private TestSupport.SavedPrefs saved;

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Before
    public void saveSettings() {
        saved = new TestSupport.SavedPrefs("agap_settings");
    }

    @After
    public void restoreSettings() {
        saved.restore();
    }

    private static SettingsStore store() {
        return SettingsStore.from(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void thresholdIsSavedAndUsedBySoundAlerts() {
        try (ActivityScenario<SettingsActivity> ignored = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.slider_threshold)).perform(scrollTo(), setSliderValue(70f));
            onView(withId(R.id.threshold_title)).check(matches(withText(containsString("70%"))));
        }
        assertEquals(70, store().getAlertThresholdPercent());
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_alerts)).perform(click());
            onView(withId(R.id.session_count)).check(matches(withText(containsString("70%"))));
        }
    }

    @Test
    public void vibrationRowTurnsVibrationOff() {
        store().setVibrationOn(true);
        try (ActivityScenario<SettingsActivity> ignored = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.row_vibration)).perform(scrollTo(), click());
            onView(withId(R.id.switch_vibration)).check(matches(isNotChecked()));
            assertEquals(false, store().isVibrationOn());
            onView(withId(R.id.row_vibration)).perform(scrollTo(), click());
            onView(withId(R.id.switch_vibration)).check(matches(isChecked()));
            assertEquals(true, store().isVibrationOn());
        }
    }

    @Test
    public void highContrastRebuildsTheScreenInBlack() {
        store().setHighContrast(false);
        try (ActivityScenario<SettingsActivity> ignored = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.mode_high_contrast)).perform(scrollTo(), click());
            assertTrue(store().isHighContrast());
            onView(withId(R.id.top_bar_holder)).check(matches(hasBackgroundColor(0xFF000000)));
        }
    }

    @Test
    public void largestTextSizeMakesTextBigger() {
        store().setTextSizeLevel(SettingsStore.TEXT_SIZE_DEFAULT);
        float[] before = new float[1];
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            scenario.onActivity(a -> before[0] = ((TextView) a.findViewById(R.id.text_size_sample)).getTextSize());
            // A real tap at the right end of the slider: the largest size.
            onView(withId(R.id.slider_text_size)).perform(scrollTo(), tapAt(0.97f, 0.5f));
            assertEquals(SettingsStore.textSizeLevelCount() - 1, store().getTextSizeLevel());
            onView(withId(R.id.text_size_sample)).check(matches(textSizeAbove(before[0] * 1.5f)));
        }
    }
}
