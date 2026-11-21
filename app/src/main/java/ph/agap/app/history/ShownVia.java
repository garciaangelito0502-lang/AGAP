package ph.agap.app.history;

/**
 * Where an alert was shown, saved with each alert (Phase 11). The research group compares response
 * times only between alerts shown the same way.
 *
 * APP_SCREEN, LOCK_SCREEN and POPUP are screens AGAP draws itself, so their "shown" time is the
 * moment the frame with the alert reached the display (as in 1.0). NOTIFICATION is drawn by Android,
 * which does not tell an app when a notification appears, so its "shown" time is the moment AGAP
 * handed the notification to Android.
 */
public enum ShownVia {
    /** AGAP's full-screen alert, with AGAP open (all of 1.0's alerts). */
    APP_SCREEN("App screen"),
    /** AGAP's full-screen alert over the lock screen, with the screen off or locked. */
    LOCK_SCREEN("Lock screen"),
    /** AGAP's alert card over another app. */
    POPUP("Popup over another app"),
    /** An Android notification only. */
    NOTIFICATION("Notification");

    public final String displayName;

    ShownVia(String displayName) {
        this.displayName = displayName;
    }

    /** The value saved in the database, or APP_SCREEN for anything unknown (rows from 1.0). */
    public static ShownVia fromStored(String stored) {
        for (ShownVia via : values()) {
            if (via.name().equals(stored)) return via;
        }
        return APP_SCREEN;
    }

    /** True when the shown time is a frame AGAP drew, false when it is a notification post time. */
    public boolean isDrawnByAgap() {
        return this != NOTIFICATION;
    }
}
