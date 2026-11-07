package ph.agap.app.history;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneId;
import java.util.Arrays;

public class HistoryCsvTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private static AlertRecord record(long id, String label, String category, String yamnetClass,
                                      float confidence, long detected, long shown) {
        AlertRecord r = new AlertRecord();
        r.id = id;
        r.label = label;
        r.category = category;
        r.yamnetClass = yamnetClass;
        r.confidence = confidence;
        r.thresholdPercent = 50;
        r.detectedAtMillis = detected;
        r.shownAtMillis = shown;
        return r;
    }

    private static String csv(AlertRecord... records) throws IOException {
        StringWriter out = new StringWriter();
        HistoryCsv.write(Arrays.asList(records), MANILA, out);
        return out.toString();
    }

    @Test
    public void startsWithByteOrderMarkAndHeader() throws IOException {
        String text = csv();
        assertEquals(0xFEFF, text.charAt(0));
        assertEquals("No.,Date,Alert,Category,Confidence (%),Heard as (YAMNet),Threshold (%),Sound detected,"
                + "Alert shown,Response time (ms),Detected (epoch ms),Shown (epoch ms)\r\n", text.substring(1));
    }

    @Test
    public void rowHasBothTimesToTheMillisecondAndTheResponseTime() {
        // 2026-11-07 04:11:58.191 in Manila (UTC+8) = 2026-11-06 20:11:58.191 UTC.
        long detected = 1_793_995_918_191L;
        String[] row = HistoryCsv.row(1, record(1, "Fire Alarm", "Emergency", "Fire alarm", 0.594f,
                detected, detected + 130), MANILA);
        assertArrayEquals(new String[]{"1", "2026-11-07", "Fire Alarm", "Emergency", "59.4", "Fire alarm", "50",
                "04:11:58.191", "04:11:58.321", "130", "1793995918191", "1793995918321"}, row);
    }

    @Test
    public void fieldsWithCommasOrQuotesAreQuoted() throws IOException {
        assertEquals("\"Vehicle horn, car horn, honking\"", HistoryCsv.escape("Vehicle horn, car horn, honking"));
        assertEquals("\"say \"\"hi\"\"\"", HistoryCsv.escape("say \"hi\""));
        assertEquals("Household/Entry", HistoryCsv.escape("Household/Entry"));
        String text = csv(record(1, "Vehicle Horn", "Traffic", "Vehicle horn, car horn, honking", 0.97f, 1000L, 1071L));
        assertTrue(text.contains(",Vehicle Horn,Traffic,97.0,\"Vehicle horn, car horn, honking\",50,"));
    }

    @Test
    public void rowsAreOldestFirstAndNumbered() throws IOException {
        String text = csv(
                record(3, "Doorbell", "Household/Entry", "Doorbell", 0.6f, 3_000L, 3_050L),
                record(1, "Fire Alarm", "Emergency", "Fire alarm", 0.9f, 1_000L, 1_080L),
                record(2, "Knocking", "Household/Entry", "Knock", 0.7f, 2_000L, 2_060L));
        String[] lines = text.substring(1).split("\r\n");
        assertEquals(4, lines.length);
        assertTrue(lines[1].startsWith("1,") && lines[1].contains(",Fire Alarm,"));
        assertTrue(lines[2].startsWith("2,") && lines[2].contains(",Knocking,"));
        assertTrue(lines[3].startsWith("3,") && lines[3].contains(",Doorbell,"));
    }

    @Test
    public void fileNameHasTheDateAndTime() {
        assertEquals("AGAP-history-2026-11-07-041158.csv", HistoryCsv.fileName(1_793_995_918_191L, MANILA));
    }
}
