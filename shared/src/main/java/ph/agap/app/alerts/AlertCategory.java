package ph.agap.app.alerts;

/** The three alert categories of contract Annex A. Each has its own colour and vibration (Phase 5). */
public enum AlertCategory {
    EMERGENCY("Emergency"),
    TRAFFIC("Traffic"),
    HOUSEHOLD("Household/Entry");

    /** The name used in Annex A, on screen and in the Event History. */
    public final String displayName;

    AlertCategory(String displayName) {
        this.displayName = displayName;
    }

    public static AlertCategory fromDisplayName(String name) {
        for (AlertCategory category : values()) {
            if (category.displayName.equals(name)) return category;
        }
        throw new IllegalArgumentException("Unknown alert category: " + name);
    }
}
