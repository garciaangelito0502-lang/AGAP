package ph.agap.app.captions;

/** What a {@link CaptionEngine} reports back. Called on the main thread. */
public interface CaptionListener {

    /** Why listening stopped on its own. */
    enum Failure {
        /** The microphone permission was taken away. */
        PERMISSION,
        /** The offline language pack is missing or was removed. */
        LANGUAGE_PACK,
        /** The recognizer kept failing; restarting did not help. */
        RECOGNIZER,
        /** The bundled speech model is missing, damaged, or would not load (Filipino). */
        MODEL
    }

    /** The microphone is open and the engine is waiting for speech. */
    void onListening();

    /** The words heard so far in the current sentence. They can still change. */
    void onPartial(String text);

    /** The final words of a sentence. They replace the partial text. */
    void onFinal(String text);

    /** A sentence ended without final words (silence or a restart): keep what was shown. */
    void onSegmentEnd();

    /** Listening stopped by itself and will not restart. */
    void onFailed(Failure failure);

    /**
     * Android silences the microphone AGAP opened (a phone call, or another app with priority), or
     * gives it back. Only engines that open the microphone themselves (Vosk) can tell.
     */
    default void onMicSilenced(boolean silenced) { }
}
