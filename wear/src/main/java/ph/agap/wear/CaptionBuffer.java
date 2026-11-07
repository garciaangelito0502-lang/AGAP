package ph.agap.wear;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The caption text on the watch: the newest finished lines (at most {@link #MAX_LINES}) and the
 * line still being spoken, which the phone keeps replacing until it is final. Memory only.
 */
final class CaptionBuffer {

    /** Finished lines kept for scrolling back. */
    static final int MAX_LINES = 30;

    private final Deque<String> lines = new ArrayDeque<>();
    private String partial = "";

    void add(String text, boolean isFinal) {
        if (isFinal) {
            if (!text.isEmpty()) lines.addLast(text);
            while (lines.size() > MAX_LINES) lines.removeFirst();
            partial = "";
        } else {
            partial = text;
        }
    }

    void clear() {
        lines.clear();
        partial = "";
    }

    boolean isEmpty() {
        return lines.isEmpty() && partial.isEmpty();
    }

    /** Finished lines, then the partial line, one per line. */
    String text() {
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            if (text.length() > 0) text.append('\n');
            text.append(line);
        }
        if (!partial.isEmpty()) {
            if (text.length() > 0) text.append('\n');
            text.append(partial);
        }
        return text.toString();
    }
}
