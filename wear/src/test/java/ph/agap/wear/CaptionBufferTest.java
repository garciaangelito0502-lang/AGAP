package ph.agap.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CaptionBufferTest {

    @Test
    public void partialIsReplacedThenFinished() {
        CaptionBuffer buffer = new CaptionBuffer();
        assertTrue(buffer.isEmpty());
        buffer.add("Magandang", false);
        buffer.add("Magandang umaga", false);
        assertEquals("Magandang umaga", buffer.text());
        buffer.add("Magandang umaga po", true);
        buffer.add("Kumusta", false);
        assertEquals("Magandang umaga po\nKumusta", buffer.text());
        assertFalse(buffer.isEmpty());
    }

    @Test
    public void keepsOnlyTheNewestLines() {
        CaptionBuffer buffer = new CaptionBuffer();
        for (int i = 1; i <= CaptionBuffer.MAX_LINES + 5; i++) buffer.add("line " + i, true);
        String[] lines = buffer.text().split("\n");
        assertEquals(CaptionBuffer.MAX_LINES, lines.length);
        assertEquals("line 6", lines[0]);
        assertEquals("line " + (CaptionBuffer.MAX_LINES + 5), lines[lines.length - 1]);
    }

    @Test
    public void emptyFinalClearsThePartialWithoutABlankLine() {
        CaptionBuffer buffer = new CaptionBuffer();
        buffer.add("Hello", true);
        buffer.add("wor", false);
        buffer.add("", true);
        assertEquals("Hello", buffer.text());
    }

    @Test
    public void clearEmptiesEverything() {
        CaptionBuffer buffer = new CaptionBuffer();
        buffer.add("Hello", true);
        buffer.add("there", false);
        buffer.clear();
        assertTrue(buffer.isEmpty());
        assertEquals("", buffer.text());
    }
}
