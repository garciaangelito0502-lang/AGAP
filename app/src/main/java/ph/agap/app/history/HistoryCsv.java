package ph.agap.app.history;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The Event History as a CSV file for a spreadsheet (Excel, Google Sheets): one row per alert,
 * oldest first so the rows follow the order of the trials, with both times to the millisecond and
 * the response time. Plain Java, so the format is tested on the computer.
 *
 * Format: RFC 4180 (comma-separated, fields with a comma or quote are quoted, CRLF line ends),
 * UTF-8 with a byte-order mark so Excel shows letters like "ñ" correctly.
 */
public final class HistoryCsv {

    static final String[] HEADER = {
            "No.", "Date", "Alert", "Category", "Confidence (%)", "Heard as (YAMNet)", "Threshold (%)",
            "Sound detected", "Alert shown", "Response time (ms)", "Detected (epoch ms)", "Shown (epoch ms)"};

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.ROOT);
    private static final DateTimeFormatter FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss", Locale.ROOT);

    private HistoryCsv() { }

    /** Writes every alert, oldest first, with times in the phone's time zone. */
    public static void write(List<AlertRecord> records, ZoneId zone, Writer out) throws IOException {
        out.write(0xFEFF); // byte-order mark
        line(out, HEADER);
        List<AlertRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparingLong((AlertRecord r) -> r.detectedAtMillis).thenComparingLong(r -> r.id));
        int number = 0;
        for (AlertRecord record : sorted) line(out, row(++number, record, zone));
        out.flush();
    }

    /** "AGAP-history-2026-11-07-043012.csv" */
    public static String fileName(long nowMillis, ZoneId zone) {
        return "AGAP-history-" + FILE.format(Instant.ofEpochMilli(nowMillis).atZone(zone)) + ".csv";
    }

    static String[] row(int number, AlertRecord r, ZoneId zone) {
        ZonedDateTime detected = Instant.ofEpochMilli(r.detectedAtMillis).atZone(zone);
        ZonedDateTime shown = Instant.ofEpochMilli(r.shownAtMillis).atZone(zone);
        return new String[]{
                Integer.toString(number),
                DATE.format(detected),
                r.label,
                r.category,
                String.format(Locale.ROOT, "%.1f", r.confidence * 100f),
                r.yamnetClass,
                Integer.toString(r.thresholdPercent),
                TIME.format(detected),
                TIME.format(shown),
                Long.toString(r.responseTimeMillis()),
                Long.toString(r.detectedAtMillis),
                Long.toString(r.shownAtMillis)};
    }

    /** Quotes a field that holds a comma, quote or line break; quotes inside are doubled. */
    static String escape(String field) {
        if (field.indexOf(',') < 0 && field.indexOf('"') < 0 && field.indexOf('\n') < 0 && field.indexOf('\r') < 0) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }

    private static void line(Writer out, String[] fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) out.write(',');
            out.write(escape(fields[i]));
        }
        out.write("\r\n");
    }
}
