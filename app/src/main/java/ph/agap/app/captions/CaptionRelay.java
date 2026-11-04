package ph.agap.app.captions;

import androidx.annotation.Nullable;

/**
 * Passes the newest caption text to one listener besides the screen. The watch link (Phase 7)
 * registers here to send captions to the watch. Nothing is stored.
 */
public final class CaptionRelay {

    public interface Sink {
        /**
         * @param text    the newest caption words (the line being heard, or the last finished one)
         * @param isFinal true when the line is finished and will not change
         */
        void onCaption(String text, boolean isFinal);

        /** Captioning stopped. */
        void onCaptionsStopped();
    }

    @Nullable private static volatile Sink sink;

    private CaptionRelay() { }

    public static void setSink(@Nullable Sink newSink) {
        sink = newSink;
    }

    public static void publish(String text, boolean isFinal) {
        Sink s = sink;
        if (s != null) s.onCaption(text, isFinal);
    }

    public static void publishStopped() {
        Sink s = sink;
        if (s != null) s.onCaptionsStopped();
    }
}
