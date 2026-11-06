package ph.agap.app.history;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/** Event History queries. Called off the main thread (HistoryRepository). */
@Dao
public interface AlertRecordDao {

    /** Saves one alert and returns its new id. */
    @Insert
    long insert(AlertRecord record);

    /** All alerts, newest first. */
    @Query("SELECT * FROM alert_events ORDER BY detectedAtMillis DESC, id DESC")
    List<AlertRecord> newestFirst();

    @Nullable
    @Query("SELECT * FROM alert_events WHERE id = :id")
    AlertRecord byId(long id);

    @Query("SELECT COUNT(*) FROM alert_events")
    int count();

    /** Deletes every alert; returns how many were deleted. */
    @Query("DELETE FROM alert_events")
    int deleteAll();
}
