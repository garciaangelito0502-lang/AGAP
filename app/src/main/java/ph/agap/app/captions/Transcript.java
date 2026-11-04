package ph.agap.app.captions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The caption text of one session, kept in memory only (never saved or logged, contract 1.2).
 * Finished lines never change. The partial line is replaced by its final words, or kept as it is
 * when the recognizer restarts without them, so a restart neither drops nor repeats words.
 */
public final class Transcript {

    private final List<String> lines = new ArrayList<>();
    private String partial = "";

    /** Shows the words heard so far in the current sentence. */
    public void setPartial(String text) {
        partial = clean(text);
    }

    /** Replaces the current partial words with the final words of the sentence. */
    public void addFinal(String text) {
        String words = clean(text);
        if (words.isEmpty()) {
            words = partial;
        }
        if (!words.isEmpty()) {
            lines.add(words);
        }
        partial = "";
    }

    /** Ends the sentence without final words: the partial words shown so far are kept. */
    public void endSegment() {
        addFinal("");
    }

    public void clear() {
        lines.clear();
        partial = "";
    }

    public boolean isEmpty() {
        return lines.isEmpty() && partial.isEmpty();
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    public String partial() {
        return partial;
    }

    /** The newest words on screen: the partial line, or else the last finished line. */
    public String latest() {
        if (!partial.isEmpty()) return partial;
        return lines.isEmpty() ? "" : lines.get(lines.size() - 1);
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }
}
