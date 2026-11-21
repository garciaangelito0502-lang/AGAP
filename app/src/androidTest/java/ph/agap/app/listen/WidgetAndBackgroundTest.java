package ph.agap.app.listen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import ph.agap.app.R;
import ph.agap.app.demo.DemoMode;
import ph.agap.app.history.AlertRecord;
import ph.agap.app.history.ShownVia;
import ph.agap.app.settings.SettingsStore;

/**
 * Phase 11 on the phone: the home-screen widget's content, and the rules that keep background
 * listening off unless it is wanted (setting off by default, never for Demonstration Mode).
 */
@RunWith(AndroidJUnit4.class)
public class WidgetAndBackgroundTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    /** Builds the widget the way a launcher does and returns its view. */
    private View widget(boolean alerts, boolean captions, String language, AlertRecord newest) {
        View[] out = new View[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            RemoteViews views = AgapWidget.views(context, alerts, captions, language, newest);
            out[0] = views.apply(context, new FrameLayout(context));
        });
        return out[0];
    }

    private static String text(View root, int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }

    @Test
    public void widgetWhileNotListeningWithNoAlerts() {
        View w = widget(false, false, "", null);
        assertEquals(context.getString(R.string.widget_not_listening), text(w, R.id.widget_status));
        assertEquals(context.getString(R.string.widget_no_alerts), text(w, R.id.widget_last_label));
        // Stop only while listening.
        assertEquals(View.GONE, w.findViewById(R.id.widget_stop).getVisibility());
        assertEquals(View.VISIBLE, w.findViewById(R.id.widget_open).getVisibility());
    }

    @Test
    public void widgetWhileListeningShowsTheNewestAlertAndStop() {
        AlertRecord fire = new AlertRecord();
        fire.label = "Fire Alarm";
        fire.category = "Emergency";
        fire.detectedAtMillis = System.currentTimeMillis();
        fire.shownAtMillis = fire.detectedAtMillis + 90;
        fire.shownVia = ShownVia.LOCK_SCREEN.name();
        View w = widget(true, false, "", fire);
        assertEquals(context.getString(R.string.widget_listening_alerts), text(w, R.id.widget_status));
        assertEquals(context.getString(R.string.widget_last_alert, "Fire Alarm"), text(w, R.id.widget_last_label));
        assertTrue(text(w, R.id.widget_last_details).startsWith("Emergency · "));
        assertEquals(View.VISIBLE, w.findViewById(R.id.widget_stop).getVisibility());

        View captions = widget(false, true, "Filipino", null);
        assertEquals(context.getString(R.string.widget_listening_captions, "Filipino"),
                text(captions, R.id.widget_status));
    }

    @Test
    public void backgroundListeningIsOffByDefaultAndNeverForDemo() {
        SettingsStore settings = SettingsStore.from(context);
        boolean before = settings.isBackgroundListeningOn();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                settings.setBackgroundListeningOn(false);
                assertFalse(ListeningService.wanted(context));
                // Demonstration Mode: sample captions never open the microphone, so the service is never wanted.
                settings.setBackgroundListeningOn(true);
                DemoMode.setOn(true);
                CaptionsSession.get(context).start(new ph.agap.app.captions.DemoCaptionEngine(false),
                        true, false, "English");
                assertTrue(CaptionsSession.get(context).isRunning());
                assertFalse(CaptionsSession.get(context).isListeningForReal());
                assertFalse(ListeningService.wanted(context));
                assertFalse(ListeningService.isRunning());
                CaptionsSession.get(context).stop();
            } finally {
                DemoMode.setOn(false);
                settings.setBackgroundListeningOn(before);
            }
        });
    }
}
