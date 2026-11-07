package ph.agap.app.ui.alerts;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;

/**
 * How an alert looks: its own icon, and its category's badge shape and colours (approved design
 * note). Colour is never the only cue: shape, icon, label and category name go with it.
 */
public final class AlertLooks {

    private AlertLooks() { }

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

    /** One plain sentence saying what the alert means (Sound Categories screen). */
    @StringRes
    public static int description(String label) {
        switch (label) {
            case "Fire Alarm": return R.string.sound_fire_alarm;
            case "Smoke Alarm": return R.string.sound_smoke_alarm;
            case "Emergency Vehicle Siren": return R.string.sound_vehicle_siren;
            case "Warning Siren": return R.string.sound_warning_siren;
            case "Vehicle Horn": return R.string.sound_vehicle_horn;
            case "Reversing Alarm": return R.string.sound_reversing_alarm;
            case "Vehicle Nearby": return R.string.sound_vehicle_nearby;
            case "Doorbell": return R.string.sound_doorbell;
            case "Knocking": return R.string.sound_knocking;
            case "Phone Ringing": return R.string.sound_phone_ringing;
            case "Alarm Clock": return R.string.sound_alarm_clock;
            case "Appliance Timer": return R.string.sound_appliance_timer;
            case "Glass Breaking": return R.string.sound_glass_breaking;
            default: return R.string.sound_unknown;
        }
    }

    /** The category's short meaning, from the design note: "Danger. Act now." and so on. */
    @StringRes
    public static int tagline(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.string.category_emergency_tagline;
            case TRAFFIC: return R.string.category_traffic_tagline;
            default: return R.string.category_household_tagline;
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

    /** The colour that fills the whole alert screen, in both themes. */
    @ColorRes
    public static int strong(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.color.emergency_strong;
            case TRAFFIC: return R.color.traffic_strong;
            default: return R.color.household_strong;
        }
    }

    /** Text, badge and button colour on the strong fill: white on red, near-black on orange and blue. */
    @ColorRes
    public static int onStrong(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.color.emergency_on_strong;
            case TRAFFIC: return R.color.traffic_on_strong;
            default: return R.color.household_on_strong;
        }
    }

    /** The info tiles on the alert screen: darker on red, lighter on orange and blue. */
    @ColorRes
    public static int tile(AlertCategory category) {
        return category == AlertCategory.EMERGENCY ? R.color.alert_tile_on_dark : R.color.alert_tile_on_light;
    }

    /** Row background (tint) of the category in the current theme. */
    @AttrRes
    public static int rowAttr(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.attr.agapEmergencyRow;
            case TRAFFIC: return R.attr.agapTrafficRow;
            default: return R.attr.agapHouseholdRow;
        }
    }

    /** Row text, icon and 1dp border (ink) of the category in the current theme. */
    @AttrRes
    public static int inkAttr(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.attr.agapEmergencyInk;
            case TRAFFIC: return R.attr.agapTrafficInk;
            default: return R.attr.agapHouseholdInk;
        }
    }
}
