package ph.agap.app.captions;

import android.speech.SpeechRecognizer;

/** What to do when Android's SpeechRecognizer reports an error code. */
final class RecognizerErrors {

    enum Action {
        /** Nobody spoke, or the sentence ended: start the next session. Normal in captioning. */
        SILENCE,
        /** Something went wrong: make a new recognizer and try again after a short wait. */
        RETRY,
        /** The microphone permission is missing: stop. */
        STOP_PERMISSION,
        /** The offline language pack is missing: stop. */
        STOP_LANGUAGE
    }

    private RecognizerErrors() { }

    static Action actionFor(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return Action.SILENCE;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return Action.STOP_PERMISSION;
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                return Action.STOP_LANGUAGE;
            default:
                // ERROR_AUDIO, ERROR_CLIENT, ERROR_RECOGNIZER_BUSY, ERROR_SERVER_DISCONNECTED and the
                // network errors (which an on-device recognizer should never raise).
                return Action.RETRY;
        }
    }

    /** The constant's name, for logs. Logs carry error codes only, never caption text. */
    static String name(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "NETWORK_TIMEOUT";
            case SpeechRecognizer.ERROR_NETWORK: return "NETWORK";
            case SpeechRecognizer.ERROR_AUDIO: return "AUDIO";
            case SpeechRecognizer.ERROR_SERVER: return "SERVER";
            case SpeechRecognizer.ERROR_CLIENT: return "CLIENT";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "SPEECH_TIMEOUT";
            case SpeechRecognizer.ERROR_NO_MATCH: return "NO_MATCH";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "RECOGNIZER_BUSY";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "INSUFFICIENT_PERMISSIONS";
            case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS: return "TOO_MANY_REQUESTS";
            case SpeechRecognizer.ERROR_SERVER_DISCONNECTED: return "SERVER_DISCONNECTED";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED: return "LANGUAGE_NOT_SUPPORTED";
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE: return "LANGUAGE_UNAVAILABLE";
            case SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT: return "CANNOT_CHECK_SUPPORT";
            default: return "ERROR_" + error;
        }
    }
}
