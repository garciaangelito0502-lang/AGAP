package ph.agap.app.history;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Saves the Event History as a CSV file in the phone's Download/AGAP folder (D-035). Works with no
 * internet and needs no permission on Android 10 and newer (the app adds its own file to
 * Downloads). The file stays on the phone; the user can open it, copy it to a computer by USB, or
 * share it.
 */
public final class HistoryExporter {

    private static final String TAG = "AGAP/History";
    /** Where the file goes, as the user sees it. */
    public static final String FOLDER = Environment.DIRECTORY_DOWNLOADS + "/AGAP";

    /** A finished export. */
    public static final class Result {
        public final Uri uri;
        public final String path;
        public final int count;

        Result(Uri uri, String path, int count) {
            this.uri = uri;
            this.path = path;
            this.count = count;
        }
    }

    private static final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "agap-export"));

    private HistoryExporter() { }

    /** Writes the file off the main thread; calls back on the main thread. */
    public static void export(Context context, List<AlertRecord> records,
                              Consumer<Result> done, Consumer<Exception> failed) {
        ContentResolver resolver = context.getApplicationContext().getContentResolver();
        Handler main = new Handler(Looper.getMainLooper());
        io.execute(() -> {
            String name = HistoryCsv.fileName(System.currentTimeMillis(), ZoneId.systemDefault());
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = null;
            try {
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Downloads did not accept the file");
                try (OutputStream os = resolver.openOutputStream(uri)) {
                    if (os == null) throw new IOException("Cannot open the file");
                    Writer writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                    HistoryCsv.write(records, ZoneId.systemDefault(), writer);
                }
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
                Log.i(TAG, "exported " + records.size() + " alerts to " + FOLDER + "/" + name);
                Result result = new Result(uri, FOLDER + "/" + name, records.size());
                main.post(() -> done.accept(result));
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "export failed", e);
                if (uri != null) resolver.delete(uri, null, null);
                main.post(() -> failed.accept(e));
            }
        });
    }
}
