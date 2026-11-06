package ph.agap.app.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Event History database on the phone, in memory (the app's own history is not touched): every
 * field survives a save and a read, to the millisecond, and the list comes back newest first.
 */
@RunWith(AndroidJUnit4.class)
public class AlertRecordDaoTest {

    private AgapDatabase db;
    private AlertRecordDao dao;

    @Before
    public void openDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AgapDatabase.class).build();
        dao = db.alerts();
    }

    @After
    public void closeDatabase() {
        db.close();
    }

    private static AlertRecord record(String label, String category, long detectedAt, long shownAt) {
        AlertRecord r = new AlertRecord();
        r.label = label;
        r.category = category;
        r.confidence = 0.734f;
        r.yamnetClass = "Doorbell";
        r.thresholdPercent = 45;
        r.detectedAtMillis = detectedAt;
        r.shownAtMillis = shownAt;
        return r;
    }

    @Test
    public void everyFieldRoundTrips() {
        long id = dao.insert(record("Doorbell", "Household/Entry", 1_762_500_340_118L, 1_762_500_340_236L));
        assertTrue(id > 0);
        AlertRecord back = dao.byId(id);
        assertNotNull(back);
        assertEquals(id, back.id);
        assertEquals("Doorbell", back.label);
        assertEquals("Household/Entry", back.category);
        assertEquals(0.734f, back.confidence, 0f);
        assertEquals("Doorbell", back.yamnetClass);
        assertEquals(45, back.thresholdPercent);
        assertEquals(1_762_500_340_118L, back.detectedAtMillis);
        assertEquals(1_762_500_340_236L, back.shownAtMillis);
        assertEquals(118L, back.responseTimeMillis());
    }

    @Test
    public void newestFirst() {
        dao.insert(record("Vehicle Horn", "Traffic", 2_000L, 2_100L));
        dao.insert(record("Fire Alarm", "Emergency", 1_000L, 1_100L));
        dao.insert(record("Doorbell", "Household/Entry", 3_000L, 3_100L));
        List<AlertRecord> all = dao.newestFirst();
        assertEquals(3, all.size());
        assertEquals("Doorbell", all.get(0).label);
        assertEquals("Vehicle Horn", all.get(1).label);
        assertEquals("Fire Alarm", all.get(2).label);
    }

    @Test
    public void sameDetectionTimeKeepsSaveOrderNewestFirst() {
        long first = dao.insert(record("Alarm Clock", "Household/Entry", 5_000L, 5_080L));
        long second = dao.insert(record("Appliance Timer", "Household/Entry", 5_000L, 5_090L));
        List<AlertRecord> all = dao.newestFirst();
        assertEquals(second, all.get(0).id);
        assertEquals(first, all.get(1).id);
    }

    @Test
    public void clearDeletesEverything() {
        dao.insert(record("Knocking", "Household/Entry", 1L, 2L));
        long id = dao.insert(record("Knocking", "Household/Entry", 3L, 4L));
        assertEquals(2, dao.count());
        assertEquals(2, dao.deleteAll());
        assertEquals(0, dao.count());
        assertNull(dao.byId(id));
        assertTrue(dao.newestFirst().isEmpty());
    }
}
