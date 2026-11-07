package ph.agap.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;

import com.google.android.material.slider.Slider;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Helpers for the Espresso tests: keeping the user's settings, slider actions, colour matcher. */
final class TestSupport {

    private TestSupport() { }

    /** Copies a SharedPreferences file and puts it back afterwards, so tests leave the phone's settings as they were. */
    static final class SavedPrefs {
        private final SharedPreferences prefs;
        private final Map<String, Object> saved;

        SavedPrefs(String name) {
            Context context = ApplicationProvider.getApplicationContext();
            prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            saved = new HashMap<>(prefs.getAll());
        }

        @SuppressWarnings("unchecked")
        void restore() {
            SharedPreferences.Editor editor = prefs.edit().clear();
            for (Map.Entry<String, Object> entry : saved.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
                else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
                else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
                else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
                else if (value instanceof String) editor.putString(entry.getKey(), (String) value);
                else if (value instanceof Set) editor.putStringSet(entry.getKey(), (Set<String>) value);
            }
            editor.commit();
        }
    }

    /**
     * Scrolls Home to one of its rows and taps it. Home's status card can change height while the
     * scroll runs (the English check answers from Google's speech service, which Espresso cannot
     * see), so a scroll that ends with the row moved is tried once more.
     */
    static void openHomeRow(int id) {
        try {
            onView(withId(id)).perform(scrollTo());
        } catch (PerformException moved) {
            SystemClock.sleep(500);
            onView(withId(id)).perform(scrollTo());
        }
        onView(withId(id)).perform(click());
    }

    /** Sets a Material Slider's value directly (as the app's listeners see a change). */
    static ViewAction setSliderValue(float value) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(Slider.class);
            }

            @Override
            public String getDescription() {
                return "set slider to " + value;
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((Slider) view).setValue(value);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /** A real finger tap at a point inside the view, given as fractions of its width and height. */
    static ViewAction tapAt(float xFraction, float yFraction) {
        return new GeneralClickAction(Tap.SINGLE, view -> {
            int[] xy = new int[2];
            view.getLocationOnScreen(xy);
            return new float[]{xy[0] + view.getWidth() * xFraction, xy[1] + view.getHeight() * yFraction};
        }, Press.FINGER, InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY);
    }

    /** Matches a view whose background is a plain colour. */
    static Matcher<View> hasBackgroundColor(int argb) {
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has background colour #" + Integer.toHexString(argb));
            }

            @Override
            protected boolean matchesSafely(View view) {
                return view.getBackground() instanceof ColorDrawable
                        && ((ColorDrawable) view.getBackground()).getColor() == argb;
            }
        };
    }

    /** Matches a TextView whose text size (px) is above the given value. */
    static Matcher<View> textSizeAbove(float px) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("text size above " + px + " px");
            }

            @Override
            protected boolean matchesSafely(View view) {
                return view instanceof android.widget.TextView
                        && ((android.widget.TextView) view).getTextSize() > px;
            }
        };
    }
}
