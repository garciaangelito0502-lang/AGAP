package ph.agap.app.captions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class TranscriptTest {

    @Test
    public void finalWordsReplaceThePartialLine() {
        Transcript t = new Transcript();
        t.setPartial("good");
        t.setPartial("good morning");
        t.addFinal("good morning everyone");
        assertEquals(Arrays.asList("good morning everyone"), t.lines());
        assertEquals("", t.partial());
    }

    @Test
    public void restartWithoutFinalWordsKeepsThePartialOnce() {
        Transcript t = new Transcript();
        t.setPartial("please open your books");
        t.endSegment();
        // The recognizer restarts and may report the end again: nothing is repeated.
        t.endSegment();
        t.setPartial("to page twelve");
        t.addFinal("to page twelve");
        assertEquals(Arrays.asList("please open your books", "to page twelve"), t.lines());
    }

    @Test
    public void emptyFinalKeepsThePartialWords() {
        Transcript t = new Transcript();
        t.setPartial("five minutes");
        t.addFinal("  ");
        assertEquals(Arrays.asList("five minutes"), t.lines());
    }

    @Test
    public void silenceAddsNothing() {
        Transcript t = new Transcript();
        t.endSegment();
        t.addFinal(null);
        assertTrue(t.isEmpty());
    }

    @Test
    public void repeatedSentencesAreKept() {
        // People do say the same thing twice; the transcript must show both.
        Transcript t = new Transcript();
        t.addFinal("thank you");
        t.addFinal("thank you");
        assertEquals(2, t.lines().size());
    }

    @Test
    public void spacesAreTidied() {
        Transcript t = new Transcript();
        t.addFinal("  we  will\tstart ");
        assertEquals("we will start", t.lines().get(0));
    }

    @Test
    public void latestIsThePartialThenTheLastLine() {
        Transcript t = new Transcript();
        assertEquals("", t.latest());
        t.addFinal("one");
        assertEquals("one", t.latest());
        t.setPartial("two");
        assertEquals("two", t.latest());
    }

    @Test
    public void clearEmptiesEverything() {
        Transcript t = new Transcript();
        t.addFinal("one");
        t.setPartial("two");
        t.clear();
        assertTrue(t.isEmpty());
    }
}
