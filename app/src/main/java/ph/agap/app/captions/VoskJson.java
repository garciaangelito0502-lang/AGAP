package ph.agap.app.captions;

import org.json.JSONException;
import org.json.JSONObject;

/** Reads the small JSON texts Vosk sends: {"partial": "..."} and {"text": "..."}. */
final class VoskJson {

    private VoskJson() { }

    /** The words in a partial result; empty when there are none or the JSON is not understood. */
    static String partial(String json) {
        return words(field(json, "partial"));
    }

    /** The words in a final result; empty when there are none or the JSON is not understood. */
    static String text(String json) {
        return words(field(json, "text"));
    }

    private static String field(String json, String key) {
        if (json == null) return "";
        try {
            return new JSONObject(json).optString(key, "");
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Drops "words" with no letter or digit in them. The Filipino model's vocabulary has 44 such
     * entries ("." "--" "'" and others, left from its training text), and Vosk sometimes returns
     * one; they are not speech and would count as extra words in the group's error counts.
     */
    static String words(String text) {
        StringBuilder out = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (!hasLetterOrDigit(word)) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word);
        }
        return out.toString();
    }

    private static boolean hasLetterOrDigit(String word) {
        for (int i = 0; i < word.length(); ) {
            int c = word.codePointAt(i);
            if (Character.isLetterOrDigit(c)) return true;
            i += Character.charCount(c);
        }
        return false;
    }
}
