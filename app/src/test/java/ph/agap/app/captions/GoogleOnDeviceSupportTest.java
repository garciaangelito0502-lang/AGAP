package ph.agap.app.captions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ph.agap.app.captions.GoogleOnDeviceSupport.Result;
import ph.agap.app.captions.GoogleOnDeviceSupport.Status;

public class GoogleOnDeviceSupportTest {

    private static final List<String> NONE = Collections.emptyList();

    @Test
    public void installedPackIsReady() {
        Result r = GoogleOnDeviceSupport.evaluate("en-US", Arrays.asList("en-US", "de-DE"), NONE, NONE);
        assertEquals(Status.READY, r.status);
        assertEquals("en-US", r.languageTag);
    }

    @Test
    public void supportedButNotInstalledNeedsDownload() {
        Result r = GoogleOnDeviceSupport.evaluate("en-US", NONE, NONE, Arrays.asList("en-US"));
        assertEquals(Status.NEEDS_DOWNLOAD, r.status);
    }

    @Test
    public void pendingPackIsDownloading() {
        Result r = GoogleOnDeviceSupport.evaluate("en-US", NONE, Arrays.asList("en-US"), Arrays.asList("en-US"));
        assertEquals(Status.DOWNLOADING, r.status);
    }

    @Test
    public void anotherInstalledEnglishIsUsed() {
        Result r = GoogleOnDeviceSupport.evaluate("en-US", Arrays.asList("en-GB"), NONE, NONE);
        assertEquals(Status.READY, r.status);
        assertEquals("en-GB", r.languageTag);
    }

    @Test
    public void installedOtherVariantBeatsADownload() {
        Result r = GoogleOnDeviceSupport.evaluate("en-US", Arrays.asList("en-GB"), NONE, Arrays.asList("en-US"));
        assertEquals(Status.READY, r.status);
        assertEquals("en-GB", r.languageTag);
    }

    @Test
    public void otherVariantIsDownloadedWhenTheWantedOneIsNotOffered() {
        Result r = GoogleOnDeviceSupport.evaluate("en-US", NONE, NONE, Arrays.asList("en-AU"));
        assertEquals(Status.NEEDS_DOWNLOAD, r.status);
        assertEquals("en-AU", r.languageTag);
    }

    @Test
    public void unknownLanguageIsNotSupported() {
        Result r = GoogleOnDeviceSupport.evaluate("en-US", Arrays.asList("fr-FR"), null, null);
        assertEquals(Status.LANGUAGE_NOT_SUPPORTED, r.status);
    }
}
