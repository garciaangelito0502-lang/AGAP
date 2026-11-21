package ph.agap.app.history;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * The app's one database, on the phone only (no backup, no cloud, contract 1.2). It holds the
 * Event History. The schema is exported to app/schemas so later versions can be checked against it.
 */
@Database(entities = {AlertRecord.class}, version = 2, exportSchema = true)
public abstract class AgapDatabase extends RoomDatabase {

    private static final String FILE_NAME = "agap.db";

    /**
     * 1.0 to 1.1 (Phase 11): each alert also saves how it was shown. The group's alerts from 1.0 are
     * kept; they were all shown on AGAP's own screen, so they read as APP_SCREEN.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `alert_events` ADD COLUMN `shownVia` TEXT NOT NULL DEFAULT 'APP_SCREEN'");
        }
    };
    private static volatile AgapDatabase instance;

    public abstract AlertRecordDao alerts();

    public static AgapDatabase get(Context context) {
        AgapDatabase db = instance;
        if (db == null) {
            synchronized (AgapDatabase.class) {
                db = instance;
                if (db == null) {
                    db = Room.databaseBuilder(context.getApplicationContext(), AgapDatabase.class, FILE_NAME)
                            .addMigrations(MIGRATION_1_2)
                            .build();
                    instance = db;
                }
            }
        }
        return db;
    }
}
