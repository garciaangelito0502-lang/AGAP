package ph.agap.app.captions;

import static org.junit.Assert.assertEquals;

import android.speech.SpeechRecognizer;

import org.junit.Test;

public class RecognizerErrorsTest {

    @Test
    public void silenceRestartsQuietly() {
        assertEquals(RecognizerErrors.Action.SILENCE, RecognizerErrors.actionFor(SpeechRecognizer.ERROR_NO_MATCH));
        assertEquals(RecognizerErrors.Action.SILENCE, RecognizerErrors.actionFor(SpeechRecognizer.ERROR_SPEECH_TIMEOUT));
    }

    @Test
    public void missingPermissionOrPackStops() {
        assertEquals(RecognizerErrors.Action.STOP_PERMISSION,
                RecognizerErrors.actionFor(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS));
        assertEquals(RecognizerErrors.Action.STOP_LANGUAGE,
                RecognizerErrors.actionFor(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE));
        assertEquals(RecognizerErrors.Action.STOP_LANGUAGE,
                RecognizerErrors.actionFor(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED));
    }

    @Test
    public void otherErrorsRetry() {
        int[] errors = {SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                SpeechRecognizer.ERROR_NETWORK, 999};
        for (int error : errors) {
            assertEquals(RecognizerErrors.Action.RETRY, RecognizerErrors.actionFor(error));
        }
    }

    @Test
    public void namesForLogs() {
        assertEquals("NO_MATCH", RecognizerErrors.name(SpeechRecognizer.ERROR_NO_MATCH));
        assertEquals("ERROR_999", RecognizerErrors.name(999));
    }
}
