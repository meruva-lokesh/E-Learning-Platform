package com.examly.springapp.video;

import com.examly.springapp.exception.InvalidRequestException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Keeps the video files on disk. The type is decided from the first bytes of the file (not from its name),
 * the size is limited while copying, and the file gets a random name so nobody can guess or overwrite it.
 */
@Service
public class VideoStorageService {
    private static final Logger log = LoggerFactory.getLogger(VideoStorageService.class);
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(mp4|webm)$");

    /** The result of saving a file. */
    public static final class Stored {
        public final String storedName;
        public final String contentType;
        public final long sizeBytes;
        public Stored(String storedName, String contentType, long sizeBytes) {
            this.storedName = storedName;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
        }
    }

    private final Path root;
    private final long maxBytes;

    public VideoStorageService(@Value("${video.storage-dir:video-files}") String dir,
                               @Value("${video.max-size-mb:200}") long maxMb) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.maxBytes = Math.max(1, maxMb) * 1024L * 1024L;
    }

    public long maxBytes() { return maxBytes; }

    /** "mp4", "webm", or null when the first bytes are not a supported video. */
    public static String detectExtension(byte[] head, int n) {
        if (n >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') return "mp4";
        if (n >= 4 && (head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45 && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3) return "webm";
        return null;
    }

    public Stored store(InputStream raw, long declaredSize) {
        if (declaredSize > maxBytes) throw tooBig();
        try (InputStream in = new BufferedInputStream(raw)) {
            in.mark(16);
            byte[] head = new byte[16];
            int n = in.readNBytes(head, 0, head.length);
            String ext = detectExtension(head, n);
            if (ext == null) throw new InvalidRequestException("Only MP4 or WebM videos can be uploaded");
            in.reset();
            Files.createDirectories(root);
            String name = UUID.randomUUID() + "." + ext;
            Path target = root.resolve(name).normalize();
            long total = 0;
            try (OutputStream out = Files.newOutputStream(target)) {
                byte[] buf = new byte[64 * 1024];
                int r;
                while ((r = in.read(buf)) > 0) {
                    total += r;
                    if (total > maxBytes) {
                        out.close();
                        Files.deleteIfExists(target);
                        throw tooBig();
                    }
                    out.write(buf, 0, r);
                }
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(target);
                throw e;
            }
            return new Stored(name, "mp4".equals(ext) ? "video/mp4" : "video/webm", total);
        } catch (IOException e) {
            log.error("Could not save the video", e);
            throw new IllegalStateException("The video could not be saved. Please try again.");
        }
    }

    /** Full path of a stored file, or null if the name is not one we created (blocks "../" tricks). */
    public Path resolve(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) return null;
        Path p = root.resolve(storedName).normalize();
        return p.startsWith(root) && Files.isRegularFile(p) ? p : null;
    }

    /** Removes a file; a missing file is fine. Never throws, so a failure cannot block a database delete. */
    public void delete(String storedName) {
        try {
            if (storedName != null && STORED_NAME.matcher(storedName).matches()) Files.deleteIfExists(root.resolve(storedName).normalize());
        } catch (IOException e) {
            log.warn("Could not remove the video file {}", storedName);
        }
    }

    private InvalidRequestException tooBig() {
        return new InvalidRequestException("The video is larger than " + (maxBytes / 1024 / 1024) + " MB");
    }
}
