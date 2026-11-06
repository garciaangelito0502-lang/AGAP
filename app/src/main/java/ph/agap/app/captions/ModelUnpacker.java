package ph.agap.app.captions;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Unpacks a speech model zip into a folder, once. Plain Java so it can be tested on the JVM.
 *
 * It writes into "<target>.tmp" and renames that to the target folder only when every file is
 * written and a marker file is in place, so a model that is half unpacked (phone full, app
 * closed, battery dead) is never mistaken for a good one, and is cleaned up next time.
 */
public final class ModelUnpacker {

    /** Written last; holds the model's version so a newer model is unpacked again. */
    public static final String MARKER = ".agap-model-complete";

    /** Reports how many bytes of the zip file have been read so far. */
    public interface Progress {
        void onBytesRead(long bytesRead);
    }

    /** The unpack was cancelled by the caller. */
    public static final class CancelledException extends IOException {
        public CancelledException() {
            super("cancelled");
        }
    }

    private ModelUnpacker() { }

    /** True when {@code target} holds a complete model of this version. */
    public static boolean isInstalled(File target, String version) {
        File marker = new File(target, MARKER);
        if (!marker.isFile()) return false;
        try (InputStream in = new FileInputStream(marker)) {
            byte[] bytes = new byte[256];
            int n = in.read(bytes);
            return n > 0 && version.equals(new String(bytes, 0, n, StandardCharsets.UTF_8).trim());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @param zip       the model zip; not closed here
     * @param version   written into the marker
     * @param target    the folder the model ends up in (replaced if it exists)
     * @param progress  called about every 1 MB read; may be null
     * @param cancelled polled while unpacking; may be null
     */
    public static void unpack(InputStream zip, String version, File target,
                              @Nullable Progress progress, @Nullable BooleanSupplier cancelled)
            throws IOException {
        File tmp = new File(target.getPath() + ".tmp");
        deleteRecursively(tmp);
        if (!tmp.mkdirs()) {
            throw new IOException("Cannot create " + tmp);
        }
        boolean done = false;
        try {
            extract(zip, tmp, progress, cancelled);
            try (OutputStream marker = new FileOutputStream(new File(tmp, MARKER))) {
                marker.write(version.getBytes(StandardCharsets.UTF_8));
            }
            deleteRecursively(target);
            if (!tmp.renameTo(target)) {
                throw new IOException("Cannot move the model into place");
            }
            done = true;
        } finally {
            if (!done) deleteRecursively(tmp);
        }
    }

    private static void extract(InputStream zip, File into, @Nullable Progress progress,
                                @Nullable BooleanSupplier cancelled) throws IOException {
        CountingStream counting = new CountingStream(zip, progress);
        String canonicalRoot = into.getCanonicalPath() + File.separator;
        String rootFolder = null;
        byte[] buffer = new byte[1 << 16];
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(counting, 1 << 16))) {
            ZipEntry entry;
            boolean first = true;
            while ((entry = in.getNextEntry()) != null) {
                if (cancelled != null && cancelled.getAsBoolean()) throw new CancelledException();
                String name = entry.getName().replace('\\', '/');
                if (first) {
                    first = false;
                    int slash = name.indexOf('/');
                    // A zip with one top folder: drop that folder name, so the files land in `into`.
                    if (slash > 0) rootFolder = name.substring(0, slash + 1);
                }
                if (rootFolder != null && name.startsWith(rootFolder)) {
                    name = name.substring(rootFolder.length());
                }
                if (name.isEmpty()) continue;

                File out = new File(into, name);
                // A zip entry must not be able to write outside the target folder ("zip slip").
                if (!out.getCanonicalPath().startsWith(canonicalRoot)) {
                    throw new IOException("Unsafe path in the model zip");
                }
                if (entry.isDirectory()) {
                    if (!out.isDirectory() && !out.mkdirs()) throw new IOException("Cannot create " + out);
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Cannot create " + parent);
                }
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out), 1 << 16)) {
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        os.write(buffer, 0, n);
                        if (cancelled != null && cancelled.getAsBoolean()) throw new CancelledException();
                    }
                }
            }
        }
        counting.finish();
    }

    /** True when the failure was the phone running out of storage. */
    public static boolean isOutOfSpace(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && (message.contains("ENOSPC") || message.contains("No space left"))) {
                return true;
            }
        }
        return false;
    }

    public static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    /** Counts the bytes read and reports them about every megabyte. */
    private static final class CountingStream extends FilterInputStream {
        private static final long STEP = 1 << 20;
        @Nullable private final Progress progress;
        private long read;
        private long reported;

        CountingStream(InputStream in, @Nullable Progress progress) {
            super(in);
            this.progress = progress;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) add(1);
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) add(n);
            return n;
        }

        private void add(int n) {
            read += n;
            if (progress != null && read - reported >= STEP) {
                reported = read;
                progress.onBytesRead(read);
            }
        }

        void finish() {
            if (progress != null) progress.onBytesRead(read);
        }
    }
}
