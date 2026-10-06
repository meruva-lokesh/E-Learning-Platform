package com.examly.springapp.dto;

import java.util.ArrayList;
import java.util.List;

/** Shapes of the earnings, payout and invoice APIs. Money is always a number of paise (100 paise = 1 rupee). */
public final class PayDtos {
    private PayDtos() {}

    public static class EarningRow {
        public Long id;
        public Long courseId;
        public String courseTitle;
        public Long paymentId;
        public long grossPaise;
        public long feePaise;
        public long ownerPaise;
        public String status;
        public String createdAt;
    }

    public static class PayoutView {
        public Long id;
        public Long instructorUserId;
        public String instructorName;
        public long amountPaise;
        public int earningsCount;
        public String reference;
        public String paidByEmail;
        public String paidAt;
    }

    /** What an instructor sees on the earnings page. */
    public static class EarningsSummary {
        public long totalPaise;
        public long pendingPaise;
        public long paidOutPaise;
        public int commissionPercent;
        public List<EarningRow> earnings = new ArrayList<>();
        public List<PayoutView> payouts = new ArrayList<>();
    }

    /** One line of the admin's "who is owed money" table. */
    public static class PayableRow {
        public Long instructorUserId;
        public String name;
        public String email;
        public String mobile;
        public long pendingPaise;
        public int pendingCount;
    }

    public static class PayoutRequest {
        public Long instructorUserId;
        public String reference;
    }

    public static class InvoiceLineView {
        public Long courseId;
        public String title;
        public long paise;
    }

    public static class InvoiceView {
        public Long id;
        public String invoiceNumber;
        public Long paymentId;
        public Long orderId;
        public String razorpayPaymentId;
        public String customerName;
        public String customerEmail;
        public String issuedAt;
        public List<InvoiceLineView> lines = new ArrayList<>();
        public long taxablePaise;
        public long taxPaise;
        public long totalPaise;
        public int gstPercent;
        public String sellerName;
        public String sellerAddress;
        public String sellerGstin;
    }
}
