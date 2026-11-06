package ph.agap.app.captions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ModelUnpackerTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private static byte[] zip(String... namesAndText) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < namesAndText.length; i += 2) {
                zip.putNextEntry(new ZipEntry(namesAndText[i]));
                if (!namesAndText[i].endsWith("/")) {
                    zip.write(namesAndText[i + 1].getBytes(StandardCharsets.UTF_8));
                }
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void unpacksFilesAndDropsTheTopFolder() throws IOException {
        byte[] data = zip("model-0.6/", "", "model-0.6/am/final.mdl", "AM", "model-0.6/graph/HCLG.fst", "GRAPH");
        File target = new File(temp.getRoot(), "vosk/model");
        ModelUnpacker.unpack(new ByteArrayInputStream(data), "v1", target, null, null);
        assertEquals("AM", read(new File(target, "am/final.mdl")));
        assertEquals("GRAPH", read(new File(target, "graph/HCLG.fst")));
        assertFalse(new File(target, "model-0.6").exists());
    }

    @Test
    public void markerMakesItInstalledOnlyForTheSameVersion() throws IOException {
        File target = new File(temp.getRoot(), "model");
        assertFalse(ModelUnpacker.isInstalled(target, "v1"));
        ModelUnpacker.unpack(new ByteArrayInputStream(zip("m/a.txt", "x")), "v1", target, null, null);
        assertTrue(ModelUnpacker.isInstalled(target, "v1"));
        assertFalse(ModelUnpacker.isInstalled(target, "v2"));
    }

    @Test
    public void leavesNoTemporaryFolderAfterSuccess() throws IOException {
        File target = new File(temp.getRoot(), "model");
        ModelUnpacker.unpack(new ByteArrayInputStream(zip("m/a.txt", "x")), "v1", target, null, null);
        assertFalse(new File(temp.getRoot(), "model.tmp").exists());
    }

    @Test
    public void rejectsEntriesThatEscapeTheTargetFolder() throws IOException {
        byte[] data = zip("m/ok.txt", "ok", "m/../../evil.txt", "bad");
        File target = new File(temp.getRoot(), "model");
        try {
            ModelUnpacker.unpack(new ByteArrayInputStream(data), "v1", target, null, null);
            fail("expected the unsafe path to be refused");
        } catch (IOException expected) {
            // fine
        }
        assertFalse(new File(temp.getRoot(), "evil.txt").exists());
        assertFalse("a half-done unpack is not a model", ModelUnpacker.isInstalled(target, "v1"));
        assertFalse(new File(temp.getRoot(), "model.tmp").exists());
    }

    @Test
    public void aBrokenZipLeavesNoModelBehind() throws IOException {
        byte[] data = zip("m/a.txt", "some text that will be cut off");
        byte[] cut = new byte[data.length / 2];
        System.arraycopy(data, 0, cut, 0, cut.length);
        File target = new File(temp.getRoot(), "model");
        try {
            ModelUnpacker.unpack(new ByteArrayInputStream(cut), "v1", target, null, null);
        } catch (IOException expected) {
            // fine either way: what matters is that nothing half-done is left as a model
        }
        assertFalse(ModelUnpacker.isInstalled(target, "v1"));
        assertFalse(new File(temp.getRoot(), "model.tmp").exists());
    }

    @Test
    public void cancellingStopsAndCleansUp() throws IOException {
        byte[] data = zip("m/a.txt", "a", "m/b.txt", "b");
        File target = new File(temp.getRoot(), "model");
        try {
            ModelUnpacker.unpack(new ByteArrayInputStream(data), "v1", target, null, () -> true);
            fail("expected a cancel");
        } catch (ModelUnpacker.CancelledException expected) {
            // fine
        }
        assertFalse(target.exists());
        assertFalse(new File(temp.getRoot(), "model.tmp").exists());
    }

    @Test
    public void aNewUnpackReplacesAnOlderOne() throws IOException {
        File target = new File(temp.getRoot(), "model");
        ModelUnpacker.unpack(new ByteArrayInputStream(zip("m/old.txt", "old")), "v1", target, null, null);
        ModelUnpacker.unpack(new ByteArrayInputStream(zip("m/new.txt", "new")), "v2", target, null, null);
        assertFalse(new File(target, "old.txt").exists());
        assertEquals("new", read(new File(target, "new.txt")));
        assertTrue(ModelUnpacker.isInstalled(target, "v2"));
    }

    @Test
    public void reportsProgressUpToTheWholeFile() throws IOException {
        byte[] big = new byte[3 << 20];
        new java.util.Random(1).nextBytes(big); // does not compress, so the zip is about 3 MB
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(bytes)) {
            z.putNextEntry(new ZipEntry("m/big.bin"));
            z.write(big);
            z.closeEntry();
        }
        byte[] data = bytes.toByteArray();
        List<Long> seen = new ArrayList<>();
        ModelUnpacker.unpack(new ByteArrayInputStream(data), "v1", new File(temp.getRoot(), "model"),
                seen::add, null);
        assertTrue("several updates for a 3 MB file", seen.size() >= 3);
        for (int i = 1; i < seen.size(); i++) {
            assertTrue("never goes backwards", seen.get(i) >= seen.get(i - 1));
        }
        assertEquals(data.length, (long) seen.get(seen.size() - 1));
    }

    @Test
    public void recognisesOutOfSpaceErrors() {
        assertTrue(ModelUnpacker.isOutOfSpace(new IOException("write failed: ENOSPC (No space left on device)")));
        assertTrue(ModelUnpacker.isOutOfSpace(new IOException("x", new IOException("No space left on device"))));
        assertFalse(ModelUnpacker.isOutOfSpace(new IOException("Permission denied")));
    }
}
