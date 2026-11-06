package ph.agap.app.mic;

import androidx.annotation.Nullable;

/**
 * Makes sure only one feature listens at a time (contract 1.10): Live Captions and Sound Alerts can
 * never hold the microphone together. Taking the microphone stops the feature that held it.
 * Used on the main thread only.
 */
public final class MicOwner {

    /** A feature that listens. */
    public interface User {
        /** The feature's name for the screen, such as "Live Captions". */
        String micUserName();

        /** Another feature took the microphone: stop listening now. */
        void onMicTaken(String byWhom);
    }

    @Nullable private static User current;

    private MicOwner() { }

    /**
     * Gives the microphone to user, stopping whoever held it.
     *
     * @return the name of the feature that was stopped, or null if the microphone was free
     */
    @Nullable
    public static String take(User user) {
        User previous = current;
        current = user;
        if (previous == null || previous == user) return null;
        previous.onMicTaken(user.micUserName());
        return previous.micUserName();
    }

    /** Gives the microphone back. Does nothing if user does not hold it. */
    public static void release(User user) {
        if (current == user) current = null;
    }

    public static boolean isHeldBy(User user) {
        return current == user;
    }
}
