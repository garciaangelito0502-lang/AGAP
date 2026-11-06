package ph.agap.app.alerts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Checks the Annex A list against the YAMNet model bundled in the app. The label list is read from
 * the model file itself (it carries yamnet_label_list.txt), so a typo in annex_a.tsv fails here.
 */
public class AnnexATest {

    private static final File ASSETS = new File("src/main/assets");
    private static List<String> labels;
    private static AnnexA annex;

    @BeforeClass
    public static void load() throws IOException {
        try (ZipFile model = new ZipFile(new File(ASSETS, SoundAlertEngine.MODEL_ASSET))) {
            ZipEntry entry = model.getEntry("yamnet_label_list.txt");
            assertNotNull("the model has no label list", entry);
            try (InputStream in = model.getInputStream(entry)) {
                labels = Arrays.asList(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\r?\n"));
            }
        }
        try (InputStream in = new FileInputStream(new File(ASSETS, AnnexA.ASSET))) {
            annex = AnnexA.parse(in);
        }
    }

    @Test
    public void modelHasAllYamnetClasses() {
        assertEquals(521, labels.size());
    }

    @Test
    public void everyAnnexAClassIsAModelLabel() {
        List<String> missing = new ArrayList<>();
        for (AlertType type : annex.alerts()) {
            for (String yamnetClass : type.yamnetClasses) {
                if (!labels.contains(yamnetClass)) missing.add(type.label + ": " + yamnetClass);
            }
        }
        if (!missing.isEmpty()) fail("Not in the YAMNet label list: " + missing);
    }

    @Test
    public void thirteenAlertsInThreeCategories() {
        assertEquals(13, annex.alerts().size());
        assertEquals(27, annex.yamnetClasses().size());
        int emergency = 0, traffic = 0, household = 0;
        for (AlertType type : annex.alerts()) {
            if (type.category == AlertCategory.EMERGENCY) emergency++;
            if (type.category == AlertCategory.TRAFFIC) traffic++;
            if (type.category == AlertCategory.HOUSEHOLD) household++;
        }
        assertEquals(4, emergency);
        assertEquals(3, traffic);
        assertEquals(6, household);
    }

    @Test
    public void classNamesWithCommasStayWhole() {
        AlertType smoke = annex.alertFor("Smoke detector, smoke alarm");
        assertNotNull(smoke);
        assertEquals("Smoke Alarm", smoke.label);
        assertEquals(AlertCategory.EMERGENCY, smoke.category);
        assertEquals(Arrays.asList("Vehicle horn, car horn, honking", "Air horn, truck horn"),
                annex.alertFor("Air horn, truck horn").yamnetClasses);
    }

    @Test
    public void aClassMappedTwiceIsRejected() {
        String twice = "Emergency\tFire Alarm\tFire alarm\nEmergency\tSmoke Alarm\tFire alarm\n";
        try {
            AnnexA.parse(new ByteArrayInputStream(twice.getBytes(StandardCharsets.UTF_8)));
            fail("expected an error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("two alerts"));
        }
    }

    @Test
    public void aLineWithoutTabsIsRejected() {
        String bad = "Emergency, Fire Alarm, Fire alarm\n";
        try {
            AnnexA.parse(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8)));
            fail("expected an error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("3 tab-separated columns"));
        }
    }
}
