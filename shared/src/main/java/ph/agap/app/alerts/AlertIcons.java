package ph.agap.app.alerts;

import androidx.annotation.DrawableRes;

import ph.agap.shared.R;

/**
 * Each alert's own icon and each category's icon and badge shape, shared by the phone and the
 * watch so both always show the same picture. Colour is never the only cue: the shape, icon, label
 * and category name go with it.
 */
public final class AlertIcons {

    private AlertIcons() { }

    /** The icon for one of the 13 Annex A alerts; the category's icon for an unknown label. */
    @DrawableRes
    public static int icon(String label, AlertCategory category) {
        switch (label) {
            case "Fire Alarm": return R.drawable.ic_sound_fire_alarm;
            case "Smoke Alarm": return R.drawable.ic_sound_smoke_alarm;
            case "Emergency Vehicle Siren": return R.drawable.ic_sound_vehicle_siren;
            case "Warning Siren": return R.drawable.ic_sound_warning_siren;
            case "Vehicle Horn": return R.drawable.ic_sound_vehicle_horn;
            case "Reversing Alarm": return R.drawable.ic_sound_reversing_alarm;
            case "Vehicle Nearby": return R.drawable.ic_sound_vehicle_nearby;
            case "Doorbell": return R.drawable.ic_sound_doorbell;
            case "Knocking": return R.drawable.ic_sound_knocking;
            case "Phone Ringing": return R.drawable.ic_sound_phone_ringing;
            case "Alarm Clock": return R.drawable.ic_sound_alarm_clock;
            case "Appliance Timer": return R.drawable.ic_sound_appliance_timer;
            case "Glass Breaking": return R.drawable.ic_sound_glass_breaking;
            default: return categoryIcon(category);
        }
    }

    @DrawableRes
    public static int categoryIcon(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.drawable.ic_cat_emergency;
            case TRAFFIC: return R.drawable.ic_cat_traffic;
            default: return R.drawable.ic_cat_household;
        }
    }

    /** Octagon (Emergency), diamond (Traffic) or circle (Household/Entry). */
    @DrawableRes
    public static int badgeShape(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.drawable.badge_octagon;
            case TRAFFIC: return R.drawable.badge_diamond;
            default: return R.drawable.badge_circle;
        }
    }
}
