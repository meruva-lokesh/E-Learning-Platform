package com.examly.springapp.pay;

import java.util.ArrayList;
import java.util.List;

/**
 * All the money arithmetic of the payment features, in one small class with no Spring in it, so it is easy to test.
 * Every amount is a whole number of paise (100 paise = 1 rupee). No decimals, so no rounding drift.
 */
public final class Money {
    private Money() {}

    /**
     * Splits {@code total} over the given weights in proportion, and guarantees the parts add up to exactly
     * {@code total} (the "largest remainder" method). Example: 100 over [1,1,1] gives [34,33,33].
     */
    public static List<Long> allocate(long total, List<Long> weights) {
        int n = weights.size();
        List<Long> out = new ArrayList<>();
        long sum = 0;
        for (Long w : weights) sum += (w == null ? 0 : w);
        if (n == 0) return out;
        if (sum <= 0) {
            // nothing to be proportional to: give it all to the first part
            for (int i = 0; i < n; i++) out.add(i == 0 ? total : 0L);
            return out;
        }
        long[] part = new long[n];
        long[] rem = new long[n];
        long used = 0;
        for (int i = 0; i < n; i++) {
            long w = weights.get(i) == null ? 0 : weights.get(i);
            // total * w can be large, but paise totals stay far below Long.MAX / weights
            part[i] = Math.floorDiv(total * w, sum);
            rem[i] = Math.floorMod(total * w, sum);
            used += part[i];
        }
        long left = total - used;
        while (left > 0) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (best == -1 || rem[i] > rem[best]) best = i;
            }
            part[best]++;
            rem[best] = -1;
            left--;
        }
        for (long p : part) out.add(p);
        return out;
    }

    /** The platform's share of {@code gross}: percent of it, rounded to the nearest paise (half up). */
    public static long fee(long gross, int percent) {
        if (percent <= 0) return 0;
        if (percent >= 100) return gross;
        return (gross * percent + 50) / 100;
    }

    /** Tax part of a price that ALREADY includes GST: total * rate / (100 + rate), rounded half up. */
    public static long taxInside(long totalInclusive, int gstPercent) {
        if (gstPercent <= 0) return 0;
        return (totalInclusive * gstPercent + (100 + gstPercent) / 2) / (100 + gstPercent);
    }

    /** "12,34,567.50" (Indian digit grouping) from paise. */
    public static String rupees(long paise) {
        boolean neg = paise < 0;
        long abs = Math.abs(paise);
        String whole = Long.toString(abs / 100);
        String frac = String.format("%02d", abs % 100);
        String grouped;
        if (whole.length() <= 3) {
            grouped = whole;
        } else {
            String last3 = whole.substring(whole.length() - 3);
            String rest = whole.substring(0, whole.length() - 3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                if (i > 0 && (rest.length() - i) % 2 == 0) sb.append(',');
                sb.append(rest.charAt(i));
            }
            grouped = sb + "," + last3;
        }
        return (neg ? "-" : "") + grouped + "." + frac;
    }
}
