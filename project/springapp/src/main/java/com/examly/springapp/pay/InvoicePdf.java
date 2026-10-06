package com.examly.springapp.pay;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws an invoice as a real PDF file, with no library: a PDF is plain text with a table of byte positions at
 * the end, and for a one-page-per-24-lines invoice that is a few dozen lines of code.
 * Uses the built-in Helvetica font, so only plain Latin characters are printed (other characters become '?',
 * and the rupee amount is written as "Rs").
 */
public final class InvoicePdf {
    private InvoicePdf() {}

    /** Everything printed on the invoice. */
    public static final class Data {
        public String sellerName = "", sellerAddress = "", sellerGstin = "";
        public String number = "", date = "", paymentRef = "";
        public String customerName = "", customerEmail = "";
        /** Each line: {title, amount in paise as text}. */
        public List<String[]> lines = new ArrayList<>();
        public long taxable, tax, total;
        public int gstPercent;
    }

    private static final int LINES_PER_PAGE = 24;

    public static byte[] render(Data d) {
        List<String> pages = new ArrayList<>();
        int n = d.lines.size();
        int pageCount = Math.max(1, (n + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        for (int p = 0; p < pageCount; p++) {
            pages.add(pageContent(d, p, pageCount));
        }
        return assemble(pages);
    }

    // ---------------------------------------------------------------- one page of drawing commands

    private static String pageContent(Data d, int page, int pageCount) {
        StringBuilder c = new StringBuilder();
        text(c, 50, 790, 20, "F2", d.gstPercent > 0 ? "TAX INVOICE" : "INVOICE");
        text(c, 50, 768, 11, "F2", d.sellerName);
        int y = 754;
        for (String part : d.sellerAddress.split("\\n")) {
            if (!part.isBlank()) { text(c, 50, y, 9, "F1", part.trim()); y -= 12; }
        }
        if (!d.sellerGstin.isBlank()) { text(c, 50, y, 9, "F1", "GSTIN: " + d.sellerGstin); }

        text(c, 360, 790, 10, "F2", "Invoice no.");
        text(c, 440, 790, 10, "F1", d.number);
        text(c, 360, 774, 10, "F2", "Date");
        text(c, 440, 774, 10, "F1", d.date);
        text(c, 360, 758, 10, "F2", "Payment ref.");
        text(c, 440, 758, 10, "F1", d.paymentRef);
        if (pageCount > 1) text(c, 360, 742, 9, "F1", "Page " + (page + 1) + " of " + pageCount);

        text(c, 50, 690, 10, "F2", "Billed to");
        text(c, 50, 676, 11, "F1", d.customerName);
        text(c, 50, 662, 10, "F1", d.customerEmail);

        // table header
        line(c, 50, 636, 545, 636);
        text(c, 55, 622, 10, "F2", "Course");
        text(c, 470, 622, 10, "F2", "Amount (Rs)");
        line(c, 50, 614, 545, 614);

        int from = page * LINES_PER_PAGE;
        int to = Math.min(d.lines.size(), from + LINES_PER_PAGE);
        int ty = 598;
        for (int i = from; i < to; i++) {
            String[] row = d.lines.get(i);
            text(c, 55, ty, 10, "F1", clip(row[0], 70));
            textRight(c, 540, ty, 10, "F1", row[1]);
            ty -= 16;
        }
        line(c, 50, ty + 8, 545, ty + 8);

        if (page == pageCount - 1) {
            int sy = ty - 10;
            if (d.gstPercent > 0) {
                text(c, 330, sy, 10, "F1", "Amount before tax");
                textRight(c, 540, sy, 10, "F1", Money.rupees(d.taxable));
                sy -= 16;
                text(c, 330, sy, 10, "F1", "GST " + d.gstPercent + "% (included in price)");
                textRight(c, 540, sy, 10, "F1", Money.rupees(d.tax));
                sy -= 16;
            }
            text(c, 330, sy, 12, "F2", "Total paid");
            textRight(c, 540, sy, 12, "F2", "Rs " + Money.rupees(d.total));
            text(c, 50, 60, 9, "F1", "This is a computer generated invoice for a payment made online. No signature is required.");
        } else {
            text(c, 50, 60, 9, "F1", "Continued on the next page");
        }
        return c.toString();
    }

    private static String clip(String s, int max) {
        String t = ascii(s);
        return t.length() <= max ? t : t.substring(0, max - 3) + "...";
    }

    private static void text(StringBuilder c, int x, int y, int size, String font, String s) {
        c.append("BT /").append(font).append(' ').append(size).append(" Tf ")
                .append(x).append(' ').append(y).append(" Td (").append(escape(s)).append(") Tj ET\n");
    }

    /** Right aligned: Helvetica digits are about 0.556 em wide, which is accurate enough for numbers. */
    private static void textRight(StringBuilder c, int xRight, int y, int size, String font, String s) {
        int w = (int) Math.round(ascii(s).length() * size * 0.556);
        text(c, xRight - w, y, size, font, s);
    }

    private static void line(StringBuilder c, int x1, int y1, int x2, int y2) {
        c.append("0.6 w ").append(x1).append(' ').append(y1).append(" m ").append(x2).append(' ').append(y2).append(" l S\n");
    }

    static String ascii(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            sb.append(ch >= 32 && ch < 127 ? ch : '?');
        }
        return sb.toString();
    }

    static String escape(String s) {
        return ascii(s).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    // ---------------------------------------------------------------- file structure

    private static byte[] assemble(List<String> contents) {
        List<byte[]> objs = new ArrayList<>();      // object number = index + 1
        int pages = contents.size();
        // 1 catalog, 2 page tree, 3 Helvetica, 4 Helvetica-Bold, then (page, content) pairs
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages; i++) kids.append(5 + i * 2).append(" 0 R ");
        objs.add(b("<< /Type /Catalog /Pages 2 0 R >>"));
        objs.add(b("<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pages + " >>"));
        objs.add(b("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objs.add(b("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        for (int i = 0; i < pages; i++) {
            int contentObj = 6 + i * 2;
            objs.add(b("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObj + " 0 R >>"));
            byte[] stream = contents.get(i).getBytes(StandardCharsets.ISO_8859_1);
            objs.add(concat(b("<< /Length " + stream.length + " >>\nstream\n"), stream, b("endstream")));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, b("%PDF-1.4\n"));
        int[] offsets = new int[objs.size()];
        for (int i = 0; i < objs.size(); i++) {
            offsets[i] = out.size();
            write(out, b((i + 1) + " 0 obj\n"));
            write(out, objs.get(i));
            write(out, b("\nendobj\n"));
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (objs.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) x.append(String.format("%010d 00000 n \n", off));
        x.append("trailer\n<< /Size ").append(objs.size() + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        write(out, b(x.toString()));
        return out.toByteArray();
    }

    private static byte[] b(String s) { return s.getBytes(StandardCharsets.ISO_8859_1); }
    private static void write(ByteArrayOutputStream o, byte[] bytes) { o.write(bytes, 0, bytes.length); }
    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) write(o, p);
        return o.toByteArray();
    }
}
