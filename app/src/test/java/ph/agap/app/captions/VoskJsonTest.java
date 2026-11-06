package ph.agap.app.captions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VoskJsonTest {

    @Test
    public void readsPartialAndFinalWords() {
        assertEquals("magandang umaga po", VoskJson.partial("{\n  \"partial\" : \"magandang umaga po\"\n}"));
        assertEquals("kumusta ka na", VoskJson.text("{\n  \"text\" : \"kumusta ka na\"\n}"));
    }

    @Test
    public void emptyOrMissingMeansNoWords() {
        assertEquals("", VoskJson.partial("{\"partial\" : \"\"}"));
        assertEquals("", VoskJson.text("{\"text\" : \"\"}"));
        assertEquals("", VoskJson.text("{\"partial\" : \"hello\"}"));
    }

    @Test
    public void badInputIsNotAnError() {
        assertEquals("", VoskJson.text(null));
        assertEquals("", VoskJson.text("not json"));
        assertEquals("", VoskJson.partial(""));
    }

    @Test
    public void handlesEscapesAndExtraSpaces() {
        assertEquals("sabi niya \"oo\"", VoskJson.text("{\"text\":\"  sabi niya \\\"oo\\\" \"}"));
        assertEquals("ñandú", VoskJson.text("{\"text\":\"\\u00f1and\\u00fa\"}"));
    }

    @Test
    public void dropsPunctuationOnlyWords() {
        assertEquals("sa kanila", VoskJson.text("{\"text\":\"sa kanila .\"}"));
        assertEquals("oo nga", VoskJson.partial("{\"partial\":\"-- oo ' nga ''.\"}"));
        assertEquals("", VoskJson.text("{\"text\":\".\"}"));
    }

    @Test
    public void keepsWordsWithLettersOrDigitsAndInnerMarks() {
        assertEquals("pang-polo ika-3 'yan", VoskJson.text("{\"text\":\"pang-polo ika-3 'yan\"}"));
    }
}
