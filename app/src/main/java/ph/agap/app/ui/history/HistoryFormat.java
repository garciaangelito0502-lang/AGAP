package ph.agap.app.ui.history;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ph.agap.app.R;

/** Dates and times in the Event History. 12- or 24-hour clock as the phone is set. */
final class HistoryFormat {

    private HistoryFormat() { }

    /** "10:45:40 AM" */
    static String time(Context context, long millis) {
        return format(DateFormat.is24HourFormat(context) ? "HH:mm:ss" : "h:mm:ss a", millis);
    }

    /** "10:45:40.118 AM", to the millisecond, for the detail view. */
    static String timeMillis(Context context, long millis) {
        return format(DateFormat.is24HourFormat(context) ? "HH:mm:ss.SSS" : "h:mm:ss.SSS a", millis);
    }

    /** "Nov 7, 2026" */
    static String date(long millis) {
        return format("MMM d, yyyy", millis);
    }

    /** A day's heading: "Today, November 7", "Yesterday, November 6" or "Thursday, November 5, 2026". */
    static String dayHeading(Context context, long millis, long nowMillis) {
        int days = daysBetween(millis, nowMillis);
        if (days == 0) return context.getString(R.string.history_today, format("MMMM d", millis));
        if (days == 1) return context.getString(R.string.history_yesterday, format("MMMM d", millis));
        return format("EEEE, MMMM d, yyyy", millis);
    }

    /** Same calendar day in the phone's time zone? Used to group the list. */
    static boolean sameDay(long a, long b) {
        return daysBetween(a, b) == 0;
    }

    private static int daysBetween(long earlier, long later) {
        return (int) Math.round((startOfDay(later) - startOfDay(earlier)) / (24.0 * 60 * 60 * 1000));
    }

    private static long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 12); // noon: avoids daylight-saving edges when subtracting
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static String format(String pattern, long millis) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }
}
