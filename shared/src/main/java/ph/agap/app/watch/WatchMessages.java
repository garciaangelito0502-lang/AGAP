package ph.agap.app.watch;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import ph.agap.app.alerts.AlertCategory;

/**
 * The small messages the phone sends to the watch over the Wearable Data Layer (contract 1.11,
 * D-042). Compact JSON in UTF-8, a few dozen bytes each. The watch only shows what it receives;
 * it never listens itself. Reading returns null for anything it does not understand, so a newer
 * phone can never crash an older watch.
 */
public final class WatchMessages {

    /** The capability the watch app advertises (wear/src/main/res/values/wear.xml). */
    public static final String CAPABILITY = "agap_watch_app";

    public static final String PATH_ALERT = "/agap/alert";
    public static final String PATH_CAPTION = "/agap/caption";
    public static final String PATH_STATE = "/agap/state";
    /** Watch to phone, empty: "my screen just opened, tell me what you are doing". */
    public static final String PATH_HELLO = "/agap/hello";

    /** Only the newest words go to the watch; a long line is cut from the front. */
    static final int MAX_CAPTION_CHARS = 400;

    /** What the phone is doing, for the watch's status line. */
    public enum Mode { IDLE, CAPTIONS, ALERTS }

    public static final class Alert {
        public final String label;
        public final AlertCategory category;
        public final int confidencePercent;
        public final long detectedAtMillis;
        public final boolean demo;
        /** The phone's Vibration setting (contract 1.7); the watch follows it. */
        public final boolean vibrate;

        Alert(String label, AlertCategory category, int confidencePercent, long detectedAtMillis, boolean demo,
              boolean vibrate) {
            this.label = label;
            this.category = category;
            this.confidencePercent = confidencePercent;
            this.detectedAtMillis = detectedAtMillis;
            this.demo = demo;
            this.vibrate = vibrate;
        }
    }

    public static final class Caption {
        public final String text;
        public final boolean isFinal;
        public final String languageTag;

        Caption(String text, boolean isFinal, String languageTag) {
            this.text = text;
            this.isFinal = isFinal;
            this.languageTag = languageTag;
        }
    }

    public static final class State {
        public final Mode mode;
        public final String languageTag;
        public final boolean demo;

        State(Mode mode, String languageTag, boolean demo) {
            this.mode = mode;
            this.languageTag = languageTag;
            this.demo = demo;
        }
    }

    private WatchMessages() { }

    // ---- Writing (phone) ----

    public static byte[] alert(String label, AlertCategory category, int confidencePercent,
                               long detectedAtMillis, boolean demo, boolean vibrate) {
        try {
            return bytes(new JSONObject()
                    .put("l", label)
                    .put("c", category.displayName)
                    .put("p", confidencePercent)
                    .put("t", detectedAtMillis)
                    .put("d", demo)
                    .put("v", vibrate));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] caption(String text, boolean isFinal, String languageTag) {
        String newest = text.length() > MAX_CAPTION_CHARS ? text.substring(text.length() - MAX_CAPTION_CHARS) : text;
        try {
            return bytes(new JSONObject().put("x", newest).put("f", isFinal).put("g", languageTag));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] state(Mode mode, String languageTag, boolean demo) {
        try {
            return bytes(new JSONObject().put("m", mode.name()).put("g", languageTag).put("d", demo));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // ---- Reading (watch) ----

    @Nullable
    public static Alert readAlert(@Nullable byte[] data) {
        JSONObject o = parse(data);
        if (o == null) return null;
        try {
            String label = o.getString("l");
            AlertCategory category = AlertCategory.fromDisplayName(o.getString("c"));
            int percent = Math.max(0, Math.min(100, o.getInt("p")));
            return new Alert(label, category, percent, o.getLong("t"), o.optBoolean("d", false),
                    o.optBoolean("v", true));
        } catch (JSONException | IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static Caption readCaption(@Nullable byte[] data) {
        JSONObject o = parse(data);
        if (o == null) return null;
        try {
            return new Caption(o.getString("x"), o.optBoolean("f", false), o.optString("g", ""));
        } catch (JSONException e) {
            return null;
        }
    }

    @Nullable
    public static State readState(@Nullable byte[] data) {
        JSONObject o = parse(data);
        if (o == null) return null;
        try {
            return new State(Mode.valueOf(o.getString("m")), o.optString("g", ""), o.optBoolean("d", false));
        } catch (JSONException | IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] bytes(JSONObject o) {
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Nullable
    private static JSONObject parse(@Nullable byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            return new JSONObject(new String(data, StandardCharsets.UTF_8));
        } catch (JSONException e) {
            return null;
        }
    }
}
