package ph.agap.app.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

/**
 * 1.0 to 1.1 (Phase 11): the group's Event History from 1.0 must survive the update. Builds a
 * database exactly as 1.0 made it (schema 1, from app/schemas), opens it with 1.1, and checks that
 * Room accepts the migrated schema and that the old alerts are all there, read as APP_SCREEN.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String NAME = "agap-migration-test.db";
    private Context context;

    @Before
    public void start() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(NAME);
    }

    @After
    public void end() {
        context.deleteDatabase(NAME);
    }

    @Test
    public void historyFrom10IsKeptAndReadsAsAppScreen() {
        File file = context.getDatabasePath(NAME);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        try (SQLiteDatabase v1 = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            // The table and index of schema 1 (app/schemas/.../1.json).
            v1.execSQL("CREATE TABLE IF NOT EXISTS `alert_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`label` TEXT NOT NULL, `category` TEXT NOT NULL, `confidence` REAL NOT NULL, "
                    + "`yamnetClass` TEXT NOT NULL, `thresholdPercent` INTEGER NOT NULL, "
                    + "`detectedAtMillis` INTEGER NOT NULL, `shownAtMillis` INTEGER NOT NULL)");
            v1.execSQL("CREATE INDEX IF NOT EXISTS `index_alert_events_detectedAtMillis` ON `alert_events` (`detectedAtMillis`)");
            v1.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
            v1.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f248b9433159ffbc8fe5fd6ae5639e7a')");
            v1.execSQL("INSERT INTO alert_events (label, category, confidence, yamnetClass, thresholdPercent, "
                    + "detectedAtMillis, shownAtMillis) VALUES ('Fire Alarm', 'Emergency', 0.91, 'Fire alarm', 50, 1000, 1080)");
            v1.execSQL("INSERT INTO alert_events (label, category, confidence, yamnetClass, thresholdPercent, "
                    + "detectedAtMillis, shownAtMillis) VALUES ('Doorbell', 'Household/Entry', 0.62, 'Ding-dong', 50, 2000, 2075)");
            v1.setVersion(1);
        }

        AgapDatabase db = Room.databaseBuilder(context, AgapDatabase.class, NAME)
                .addMigrations(AgapDatabase.MIGRATION_1_2)
                .allowMainThreadQueries()
                .build();
        try {
            List<AlertRecord> all = db.alerts().newestFirst();
            assertEquals(2, all.size());
            AlertRecord doorbell = all.get(0);
            assertEquals("Doorbell", doorbell.label);
            assertEquals(75L, doorbell.responseTimeMillis());
            assertEquals(ShownVia.APP_SCREEN, doorbell.via());
            assertEquals(ShownVia.APP_SCREEN, all.get(1).via());

            // A 1.1 alert next to them keeps its own way of being shown.
            AlertRecord lock = new AlertRecord();
            lock.label = "Knocking";
            lock.category = "Household/Entry";
            lock.yamnetClass = "Knock";
            lock.detectedAtMillis = 3000;
            lock.shownAtMillis = 3110;
            lock.shownVia = ShownVia.LOCK_SCREEN.name();
            db.alerts().insert(lock);
            AlertRecord newest = db.alerts().newest();
            assertNotNull(newest);
            assertEquals(ShownVia.LOCK_SCREEN, newest.via());
            assertEquals(3, db.alerts().count());
        } finally {
            db.close();
        }
    }
}
