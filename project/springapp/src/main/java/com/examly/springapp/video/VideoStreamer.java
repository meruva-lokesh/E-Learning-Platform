package com.examly.springapp.video;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/** Writes a video file to the response, whole or just the part the player asked for. */
public final class VideoStreamer {
    private static final int BUFFER = 64 * 1024;

    private VideoStreamer() {}

    /**
     * @param rangeHeader the Range header of the request, may be null
     * @throws RangeParser.NotSatisfiableException when the range is outside the file (the caller answers 416)
     */
    public static void write(Path file, String contentType, String rangeHeader, HttpServletResponse response) throws IOException {
        long length = file.toFile().length();
        RangeParser.Range range = RangeParser.parse(rangeHeader, length);
        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "private, max-age=0, no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        long start = 0;
        long count = length;
        if (range == null) {
            response.setStatus(200);
        } else {
            start = range.start;
            count = range.length();
            response.setStatus(206);
            response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + length);
        }
        response.setContentLengthLong(count);
        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
            in.seek(start);
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[BUFFER];
            long left = count;
            while (left > 0) {
                int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                if (n < 0) break;
                out.write(buf, 0, n);
                left -= n;
            }
            out.flush();
        }
    }
}
