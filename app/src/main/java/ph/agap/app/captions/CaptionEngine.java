package ph.agap.app.captions;

/**
 * One speech-to-text engine for one language. English and Filipino each get their own
 * implementation and are never mixed in one session (contract 1.3, Methods).
 * All methods are called on the main thread, and the listener is called back on it.
 */
public interface CaptionEngine {

    /** Starts listening. Keeps going, restarting the recognizer as needed, until {@link #stop()}. */
    void start(CaptionListener listener);

    /** Stops listening and releases the microphone at once. Safe to call more than once. */
    void stop();

    /** The language this engine recognises, as a BCP 47 tag such as "en-US". */
    String languageTag();

    /** A short name for logs and the phase reports, such as "Google on-device". */
    String engineName();
}
