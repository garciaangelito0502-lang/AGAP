package ph.agap.app.watch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import ph.agap.app.alerts.AlertCategory;

public class WatchMessagesTest {

    @Test
    public void alertRoundTrips() {
        byte[] data = WatchMessages.alert("Emergency Vehicle Siren", AlertCategory.EMERGENCY, 92, 1_793_995_918_191L, false, true);
        WatchMessages.Alert a = WatchMessages.readAlert(data);
        assertNotNull(a);
        assertEquals("Emergency Vehicle Siren", a.label);
        assertEquals(AlertCategory.EMERGENCY, a.category);
        assertEquals(92, a.confidencePercent);
        assertEquals(1_793_995_918_191L, a.detectedAtMillis);
        assertFalse(a.demo);
        assertTrue(a.vibrate);
    }

    @Test
    public void householdCategoryWithSlashRoundTrips() {
        WatchMessages.Alert a = WatchMessages.readAlert(
                WatchMessages.alert("Doorbell", AlertCategory.HOUSEHOLD, 61, 5L, true, false));
        assertNotNull(a);
        assertEquals(AlertCategory.HOUSEHOLD, a.category);
        assertTrue(a.demo);
        assertFalse(a.vibrate);
    }

    @Test
    public void messagesAreSmall() {
        assertTrue(WatchMessages.alert("Emergency Vehicle Siren", AlertCategory.EMERGENCY, 100,
                Long.MAX_VALUE, true, true).length < 130);
        assertTrue(WatchMessages.state(WatchMessages.Mode.CAPTIONS, "fil-PH", false).length < 60);
    }

    @Test
    public void captionRoundTripsWithFilipinoText() {
        WatchMessages.Caption c = WatchMessages.readCaption(
                WatchMessages.caption("Magandang umaga po, ñ", true, "fil-PH"));
        assertNotNull(c);
        assertEquals("Magandang umaga po, ñ", c.text);
        assertTrue(c.isFinal);
        assertEquals("fil-PH", c.languageTag);
    }

    @Test
    public void longCaptionKeepsTheNewestWords() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 100; i++) text.append("word").append(i).append(' ');
        text.append("LAST");
        WatchMessages.Caption c = WatchMessages.readCaption(WatchMessages.caption(text.toString(), false, "en-US"));
        assertNotNull(c);
        assertEquals(WatchMessages.MAX_CAPTION_CHARS, c.text.length());
        assertTrue(c.text.endsWith("LAST"));
        assertTrue(WatchMessages.caption(text.toString(), false, "en-US").length < 500);
    }

    @Test
    public void stateRoundTrips() {
        WatchMessages.State s = WatchMessages.readState(WatchMessages.state(WatchMessages.Mode.ALERTS, "", true));
        assertNotNull(s);
        assertEquals(WatchMessages.Mode.ALERTS, s.mode);
        assertTrue(s.demo);
    }

    @Test
    public void badInputIsIgnoredNotACrash() {
        assertNull(WatchMessages.readAlert(null));
        assertNull(WatchMessages.readAlert(new byte[0]));
        assertNull(WatchMessages.readAlert("not json".getBytes(StandardCharsets.UTF_8)));
        assertNull(WatchMessages.readAlert("{\"l\":\"X\",\"c\":\"Weather\",\"p\":1,\"t\":2}".getBytes(StandardCharsets.UTF_8)));
        assertNull(WatchMessages.readState("{\"m\":\"DANCING\"}".getBytes(StandardCharsets.UTF_8)));
        assertNull(WatchMessages.readCaption("{\"f\":true}".getBytes(StandardCharsets.UTF_8)));
    }
}
