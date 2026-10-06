package com.examly.springapp.service;

import com.examly.springapp.dto.PayDtos.*;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.Invoice;
import com.examly.springapp.model.Payment;
import com.examly.springapp.pay.InvoicePdf;
import com.examly.springapp.pay.Money;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.InvoiceRepo;
import com.examly.springapp.repository.PaymentRepo;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Makes the invoice of a verified payment and hands it out as data or as a PDF file.
 * A customer can see only his own invoices; an admin can see all of them.
 */
@Service
public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final InvoiceRepo invoices;
    private final PaymentRepo payments;
    private final CustomerRepo customers;
    private final CourseRepo courses;
    private final AccessService access;
    private final String prefix, sellerName, sellerAddress, sellerGstin;
    private final int gstPercent;

    public InvoiceService(InvoiceRepo invoices, PaymentRepo payments, CustomerRepo customers, CourseRepo courses,
                          AccessService access,
                          @Value("${invoice.prefix:INV}") String prefix,
                          @Value("${invoice.seller-name:Online E-Learning Platform}") String sellerName,
                          @Value("${invoice.seller-address:}") String sellerAddress,
                          @Value("${invoice.seller-gstin:}") String sellerGstin,
                          @Value("${invoice.gst-percent:0}") int gstPercent) {
        this.invoices = invoices;
        this.payments = payments;
        this.customers = customers;
        this.courses = courses;
        this.access = access;
        this.prefix = prefix;
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerGstin = sellerGstin;
        this.gstPercent = Math.max(0, Math.min(40, gstPercent));
    }

    // ------------------------------------------------------------------ making the invoice

    /** Makes the invoice for a PAID payment, or returns the one that already exists. Safe to call many times. */
    public Invoice ensureForPayment(Payment payment) {
        if (payment == null || !Payment.PAID.equals(payment.getStatus())) return null;
        Optional<Invoice> existing = DatabaseOperationException.guard("loading an invoice", () -> invoices.findByPaymentId(payment.getPaymentId()));
        if (existing.isPresent()) return existing.get();

        Customer customer = DatabaseOperationException.guard("loading a customer", () -> customers.findById(payment.getCustomerId())).orElse(null);
        List<Long> ids = new ArrayList<>();
        for (String s : payment.getCourseIds().split(",")) {
            if (!s.isBlank()) ids.add(Long.valueOf(s.trim()));
        }
        List<String> titles = new ArrayList<>();
        List<Long> weights = new ArrayList<>();
        for (Long id : ids) {
            Course c = DatabaseOperationException.guard("loading a course", () -> courses.findById(id)).orElse(null);
            titles.add(c == null ? "Course " + id : c.getCourseType());
            weights.add(c == null || c.getCoursePrice() == null ? 0L : Math.round(c.getCoursePrice() * 100));
        }
        List<Long> amounts = Money.allocate(payment.getAmountPaise(), weights);
        StringBuilder lines = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) lines.append('\n');
            lines.append(ids.get(i)).append('|').append(titles.get(i).replace('|', '/').replace('\n', ' ')).append('|').append(amounts.get(i));
        }

        Invoice inv = new Invoice();
        inv.setInvoiceNumber("TMP-" + UUID.randomUUID());      // unique placeholder until the database gives us the id
        inv.setPaymentId(payment.getPaymentId());
        inv.setOrderId(payment.getOrderId());
        inv.setCustomerId(payment.getCustomerId());
        inv.setCustomerName(customer == null ? "Customer" : customer.getCustomerName());
        inv.setCustomerEmail(customer == null || customer.getUser() == null ? "" : customer.getUser().getEmail());
        inv.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        inv.setLines(lines.toString());
        long total = payment.getAmountPaise();
        long tax = Money.taxInside(total, gstPercent);
        inv.setTotalPaise(total);
        inv.setTaxPaise(tax);
        inv.setTaxablePaise(total - tax);
        inv.setGstPercent(gstPercent);
        Instant now = payment.getPaidAt() == null ? Instant.now() : payment.getPaidAt();
        inv.setIssuedAt(now);
        try {
            Invoice saved = invoices.save(inv);
            saved.setInvoiceNumber(prefix + "-" + now.atZone(IST).getYear() + "-" + String.format("%06d", saved.getId()));
            saved = invoices.save(saved);
            log.info("Invoice {} created for payment {}", saved.getInvoiceNumber(), payment.getPaymentId());
            return saved;
        } catch (DataIntegrityViolationException dup) {
            // two requests made it at the same moment: the other one won, use its invoice
            return invoices.findByPaymentId(payment.getPaymentId()).orElseThrow(() -> dup);
        }
    }

    // ------------------------------------------------------------------ reading

    public List<InvoiceView> listMine() {
        Customer me = access.currentCustomer();
        List<InvoiceView> out = new ArrayList<>();
        for (Invoice i : DatabaseOperationException.guard("loading invoices", () -> invoices.findByCustomerIdOrderByIdDesc(me.getCustomerId()))) {
            out.add(view(i));
        }
        return out;
    }

    public InvoiceView get(Long id) {
        return view(load(id));
    }

    /** The invoice of an order. If the payment was verified but the invoice is missing (it failed once), it is made now. */
    public InvoiceView getByOrder(Long orderId) {
        Payment p = DatabaseOperationException.guard("loading a payment", () -> payments.findByOrderId(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("There is no invoice for this order"));
        Invoice inv = ensureForPayment(p);
        if (inv == null) throw new ResourceNotFoundException("There is no invoice for this order");
        check(inv);
        return view(inv);
    }

    public byte[] pdf(Long id) {
        Invoice inv = load(id);
        InvoicePdf.Data d = new InvoicePdf.Data();
        d.sellerName = sellerName;
        d.sellerAddress = sellerAddress.replace("\\n", "\n");
        d.sellerGstin = sellerGstin;
        d.number = inv.getInvoiceNumber();
        d.date = DATE.format(inv.getIssuedAt().atZone(IST));
        d.paymentRef = inv.getRazorpayPaymentId() == null ? "" : inv.getRazorpayPaymentId();
        d.customerName = inv.getCustomerName();
        d.customerEmail = inv.getCustomerEmail();
        for (InvoiceLineView l : lines(inv)) {
            d.lines.add(new String[]{l.title, Money.rupees(l.paise)});
        }
        d.taxable = inv.getTaxablePaise();
        d.tax = inv.getTaxPaise();
        d.total = inv.getTotalPaise();
        d.gstPercent = inv.getGstPercent();
        return InvoicePdf.render(d);
    }

    public String fileName(Long id) {
        return "invoice-" + load(id).getInvoiceNumber() + ".pdf";
    }

    // ------------------------------------------------------------------ helpers

    private Invoice load(Long id) {
        Invoice inv = DatabaseOperationException.guard("loading an invoice", () -> invoices.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        check(inv);
        return inv;
    }

    /** An admin may see every invoice; a customer only his own. A stranger gets 404, so ids cannot be guessed. */
    private void check(Invoice inv) {
        if (access.isAdmin()) return;
        Customer me;
        try {
            me = access.currentCustomer();
        } catch (RuntimeException e) {
            throw new AccessDeniedException("Not allowed");
        }
        if (me == null || !me.getCustomerId().equals(inv.getCustomerId())) {
            throw new ResourceNotFoundException("Invoice not found");
        }
    }

    static List<InvoiceLineView> lines(Invoice inv) {
        List<InvoiceLineView> out = new ArrayList<>();
        if (inv.getLines() == null || inv.getLines().isBlank()) return out;
        for (String row : inv.getLines().split("\n")) {
            String[] p = row.split("\\|", 3);
            if (p.length < 3) continue;
            InvoiceLineView l = new InvoiceLineView();
            l.courseId = Long.valueOf(p[0]);
            l.title = p[1];
            l.paise = Long.parseLong(p[2]);
            out.add(l);
        }
        return out;
    }

    private InvoiceView view(Invoice i) {
        InvoiceView v = new InvoiceView();
        v.id = i.getId();
        v.invoiceNumber = i.getInvoiceNumber();
        v.paymentId = i.getPaymentId();
        v.orderId = i.getOrderId();
        v.razorpayPaymentId = i.getRazorpayPaymentId();
        v.customerName = i.getCustomerName();
        v.customerEmail = i.getCustomerEmail();
        v.issuedAt = i.getIssuedAt() == null ? null : i.getIssuedAt().toString();
        v.lines = lines(i);
        v.taxablePaise = i.getTaxablePaise();
        v.taxPaise = i.getTaxPaise();
        v.totalPaise = i.getTotalPaise();
        v.gstPercent = i.getGstPercent();
        v.sellerName = sellerName;
        v.sellerAddress = sellerAddress.replace("\\n", "\n");
        v.sellerGstin = sellerGstin;
        return v;
    }
}
