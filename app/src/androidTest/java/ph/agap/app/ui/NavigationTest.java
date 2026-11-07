package ph.agap.app.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ph.agap.app.ui.TestSupport.openHomeRow;

import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ph.agap.app.R;
import ph.agap.app.demo.DemoMode;

/**
 * Smoke tests for moving around the app. Google's accessibility checks run on the whole screen at
 * every action (touch targets of 48dp, labels, contrast...), so a failing check fails the test.
 * Nothing here opens the microphone or changes the Event History.
 */
@RunWith(AndroidJUnit4.class)
public class NavigationTest {

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Rule
    public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule<>(MainActivity.class);

    @After
    public void demoModeOff() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> DemoMode.setOn(false));
    }

    @Test
    public void homeShowsBothModesAndTheStatus() {
        onView(withId(R.id.greeting)).check(matches(isDisplayed()));
        onView(withId(R.id.card_captions)).check(matches(isDisplayed()));
        onView(withId(R.id.card_alerts)).check(matches(isDisplayed()));
        onView(withId(R.id.home_status)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText(R.string.home_status_alerts)).check(matches(isDisplayed()));
        onView(withText(R.string.home_status_offline)).check(matches(isDisplayed()));
    }

    @Test
    public void bottomTabsOpenEachScreen() {
        onView(withId(R.id.nav_captions)).perform(click());
        onView(withId(R.id.language_group)).check(matches(isDisplayed()));
        onView(withId(R.id.start_stop)).check(matches(isDisplayed()));

        onView(withId(R.id.nav_alerts)).perform(click());
        onView(withId(R.id.status_text)).check(matches(withText(R.string.alerts_status_idle)));
        onView(withId(R.id.session_count)).check(matches(withText(containsString("Alert threshold"))));

        onView(withId(R.id.nav_history)).perform(click());
        onView(withId(R.id.action_export_history)).check(matches(isDisplayed()));
        onView(withId(R.id.action_clear_history)).check(matches(isDisplayed()));

        onView(withId(R.id.nav_home)).perform(click());
        onView(withId(R.id.greeting)).check(matches(isDisplayed()));
        onView(withId(R.id.action_clear_history)).check(doesNotExist());
    }

    @Test
    public void homeRowsOpenTheOtherScreens() {
        openHomeRow(R.id.row_categories);
        onView(withText(R.string.category_emergency_tagline)).check(matches(isDisplayed()));
        onView(withText("Fire Alarm")).check(matches(isDisplayed()));
        onView(withText("Glass Breaking")).perform(scrollTo()).check(matches(isDisplayed()));
        pressBack();

        openHomeRow(R.id.row_vibration);
        onView(allOf(withId(R.id.vibration_try), withContentDescription("Try the Traffic vibration")))
                .perform(scrollTo(), click());
        pressBack();

        openHomeRow(R.id.row_settings);
        onView(withId(R.id.slider_threshold)).perform(scrollTo()).check(matches(isDisplayed()));
        pressBack();

        // Home keeps its scroll position (at the bottom, after Settings): scroll back up.
        onView(withId(R.id.greeting)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void demonstrationModeShowsItsBannerAndBlocksTheMicrophone() {
        openHomeRow(R.id.row_demo);
        onView(withId(R.id.demo_turn_on)).perform(click());
        onView(withId(R.id.demo_banner)).check(matches(isDisplayed()));
        onView(withId(R.id.demo_chips)).check(matches(isDisplayed()));
        pressBack();

        // Home, then Sound Alerts: banner shown, Start disabled.
        onView(withId(R.id.demo_banner)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_alerts)).perform(click());
        onView(withId(R.id.start_stop)).check(matches(not(isEnabled())));
        onView(withId(R.id.nav_home)).perform(click());

        openHomeRow(R.id.row_demo);
        onView(withId(R.id.demo_exit)).perform(scrollTo(), click());
        onView(withId(R.id.demo_banner)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.demo_turn_on)).check(matches(isDisplayed()));
    }
}
