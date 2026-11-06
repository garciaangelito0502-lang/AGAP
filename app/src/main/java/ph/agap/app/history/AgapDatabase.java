package ph.agap.app.history;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The app's one database, on the phone only (no backup, no cloud, contract 1.2). It holds the
 * Event History. The schema is exported to app/schemas so later versions can be checked against it.
 */
@Database(entities = {AlertRecord.class}, version = 1, exportSchema = true)
public abstract class AgapDatabase extends RoomDatabase {

    private static final String FILE_NAME = "agap.db";
    private static volatile AgapDatabase instance;

    public abstract AlertRecordDao alerts();

    public static AgapDatabase get(Context context) {
        AgapDatabase db = instance;
        if (db == null) {
            synchronized (AgapDatabase.class) {
                db = instance;
                if (db == null) {
                    db = Room.databaseBuilder(context.getApplicationContext(), AgapDatabase.class, FILE_NAME)
                            .build();
                    instance = db;
                }
            }
        }
        return db;
    }
}
