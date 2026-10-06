package com.examly.springapp.video;

/**
 * Reads the HTTP "Range" header a video player sends, for example "bytes=1000-" when the viewer
 * jumps ahead in the video. Only one range is supported, which is all browsers ask for.
 */
public final class RangeParser {
    private RangeParser() {}

    /** First and last byte to send (both inclusive). */
    public static final class Range {
        public final long start;
        public final long end;
        public Range(long start, long end) { this.start = start; this.end = end; }
        public long length() { return end - start + 1; }
    }

    /** Thrown when the asked bytes are outside the file; the answer is then HTTP 416. */
    public static class NotSatisfiableException extends RuntimeException {
        public NotSatisfiableException(String message) { super(message); }
    }

    /**
     * @return the range to send, or {@code null} when the whole file should be sent
     *         (no header, a header we do not understand, or more than one range)
     */
    public static Range parse(String header, long fileLength) {
        if (header == null || fileLength <= 0) return null;
        String h = header.trim();
        if (!h.regionMatches(true, 0, "bytes=", 0, 6)) return null;
        String spec = h.substring(6).trim();
        if (spec.isEmpty() || spec.contains(",")) return null;
        int dash = spec.indexOf('-');
        if (dash < 0) return null;
        String a = spec.substring(0, dash).trim();
        String b = spec.substring(dash + 1).trim();
        try {
            if (a.isEmpty()) {
                // "-500" means the last 500 bytes
                if (b.isEmpty()) return null;
                long n = Long.parseLong(b);
                if (n <= 0) throw new NotSatisfiableException("Range not satisfiable");
                long start = Math.max(0, fileLength - n);
                return new Range(start, fileLength - 1);
            }
            long start = Long.parseLong(a);
            long end = b.isEmpty() ? fileLength - 1 : Long.parseLong(b);
            if (start < 0 || start >= fileLength || end < start) throw new NotSatisfiableException("Range not satisfiable");
            return new Range(start, Math.min(end, fileLength - 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
