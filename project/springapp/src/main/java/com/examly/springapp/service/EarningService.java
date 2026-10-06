package com.examly.springapp.service;

import com.examly.springapp.dto.PayDtos.*;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.CourseEarning;
import com.examly.springapp.model.CourseOwner;
import com.examly.springapp.model.Payment;
import com.examly.springapp.model.Payout;
import com.examly.springapp.model.User;
import com.examly.springapp.pay.Money;
import com.examly.springapp.repository.CourseEarningRepo;
import com.examly.springapp.repository.CourseOwnerRepo;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.PayoutRepo;
import com.examly.springapp.repository.UserRepo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who earned what from a paid order.
 * <p>
 * All customer money lands in the platform's one Razorpay account. This service keeps a LEDGER that says how much of
 * each paid course belongs to the instructor who made it (the rest, the commission, stays with the platform).
 * The admin then pays the instructors outside the app and records a payout here. Courses made by an admin have
 * no owner, so the platform keeps all of that money and no ledger row is written.
 */
@Service
public class EarningService {
    private static final Logger log = LoggerFactory.getLogger(EarningService.class);

    private final CourseEarningRepo earnings;
    private final PayoutRepo payouts;
    private final CourseOwnerRepo owners;
    private final CourseRepo courses;
    private final UserRepo users;
    private final int commissionPercent;

    public EarningService(CourseEarningRepo earnings, PayoutRepo payouts, CourseOwnerRepo owners, CourseRepo courses,
                          UserRepo users, @Value("${commission.percent:20}") int commissionPercent) {
        this.earnings = earnings;
        this.payouts = payouts;
        this.owners = owners;
        this.courses = courses;
        this.users = users;
        this.commissionPercent = Math.max(0, Math.min(100, commissionPercent));
    }

    // ------------------------------------------------------------------ writing the ledger

    /** Called once a payment is verified. Safe to call again for the same payment: it writes nothing the second time. */
    public int record(Payment payment, Long orderId) {
        if (payment == null || payment.getPaymentId() == null || payment.getCourseIds() == null) return 0;
        if (DatabaseOperationException.guard("checking earnings", () -> earnings.existsByPaymentId(payment.getPaymentId()))) {
            return 0;
        }
        List<Long> ids = new ArrayList<>();
        for (String s : payment.getCourseIds().split(",")) {
            if (!s.isBlank()) ids.add(Long.valueOf(s.trim()));
        }
        List<Course> list = new ArrayList<>();
        List<Long> weights = new ArrayList<>();
        for (Long id : ids) {
            Course c = DatabaseOperationException.guard("loading a course", () -> courses.findById(id)).orElse(null);
            list.add(c);
            weights.add(c == null || c.getCoursePrice() == null ? 0L : Math.round(c.getCoursePrice() * 100));
        }
        // split what the customer really paid over the courses, so the lines add up to the payment exactly
        List<Long> gross = Money.allocate(payment.getAmountPaise(), weights);
        int written = 0;
        for (int i = 0; i < ids.size(); i++) {
            Long courseId = ids.get(i);
            Optional<CourseOwner> owner = DatabaseOperationException.guard("loading a course owner", () -> owners.findByCourseId(courseId));
            if (owner.isEmpty()) continue;                    // admin course: the platform keeps everything
            long g = gross.get(i);
            long fee = Money.fee(g, commissionPercent);
            CourseEarning e = new CourseEarning();
            e.setPaymentId(payment.getPaymentId());
            e.setOrderId(orderId);
            e.setCourseId(courseId);
            e.setCourseTitle(list.get(i) == null ? "Course " + courseId : list.get(i).getCourseType());
            e.setInstructorUserId(owner.get().getInstructorUserId());
            e.setGrossPaise(g);
            e.setFeePaise(fee);
            e.setOwnerPaise(g - fee);
            e.setStatus(CourseEarning.PENDING);
            e.setCreatedAt(Instant.now());
            try {
                earnings.save(e);
                written++;
            } catch (DataIntegrityViolationException dup) {
                log.info("Earning for payment {} course {} already exists", payment.getPaymentId(), courseId);
            }
        }
        log.info("Payment {}: {} earning row(s) written", payment.getPaymentId(), written);
        return written;
    }

    // ------------------------------------------------------------------ instructor view

    public EarningsSummary forInstructor(User instructor) {
        List<CourseEarning> rows = DatabaseOperationException.guard("loading earnings",
                () -> earnings.findByInstructorUserIdOrderByIdDesc(instructor.getUserId()));
        EarningsSummary s = new EarningsSummary();
        s.commissionPercent = commissionPercent;
        for (CourseEarning e : rows) {
            s.totalPaise += e.getOwnerPaise();
            if (CourseEarning.PAID_OUT.equals(e.getStatus())) s.paidOutPaise += e.getOwnerPaise();
            else s.pendingPaise += e.getOwnerPaise();
            s.earnings.add(row(e));
        }
        for (Payout p : DatabaseOperationException.guard("loading payouts", () -> payouts.findByInstructorUserIdOrderByIdDesc(instructor.getUserId()))) {
            s.payouts.add(view(p, instructor.getUsername()));
        }
        return s;
    }

    // ------------------------------------------------------------------ admin view and payout

    /** Instructors who are owed money, with the amount. */
    public List<PayableRow> payable() {
        Map<Long, PayableRow> byInstructor = new LinkedHashMap<>();
        for (CourseEarning e : DatabaseOperationException.guard("loading earnings", () -> earnings.findByStatus(CourseEarning.PENDING))) {
            PayableRow r = byInstructor.computeIfAbsent(e.getInstructorUserId(), id -> {
                PayableRow n = new PayableRow();
                n.instructorUserId = id;
                User u = users.findById(id).orElse(null);
                n.name = u == null ? "User " + id : u.getUsername();
                n.email = u == null ? "" : u.getEmail();
                n.mobile = u == null ? "" : u.getMobileNumber();
                return n;
            });
            r.pendingPaise += e.getOwnerPaise();
            r.pendingCount++;
        }
        return new ArrayList<>(byInstructor.values());
    }

    public List<PayoutView> allPayouts() {
        List<PayoutView> out = new ArrayList<>();
        for (Payout p : DatabaseOperationException.guard("loading payouts", () -> payouts.findAllByOrderByIdDesc())) {
            User u = users.findById(p.getInstructorUserId()).orElse(null);
            out.add(view(p, u == null ? "User " + p.getInstructorUserId() : u.getUsername()));
        }
        return out;
    }

    /** Marks everything pending for this instructor as paid out, and keeps the bank / UPI reference the admin typed. */
    @Transactional
    public PayoutView pay(String adminEmail, PayoutRequest req) {
        if (req == null || req.instructorUserId == null) throw new InvalidRequestException("Choose an instructor");
        String ref = req.reference == null ? "" : req.reference.trim();
        if (ref.length() < 3 || ref.length() > 100) {
            throw new InvalidRequestException("Enter the bank or UPI transaction reference (3 to 100 characters)");
        }
        User instructor = users.findById(req.instructorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
        if (!"INSTRUCTOR".equals(instructor.getRole())) throw new ResourceNotFoundException("Instructor not found");
        List<CourseEarning> pending = DatabaseOperationException.guard("loading earnings",
                () -> earnings.findByInstructorUserIdAndStatus(instructor.getUserId(), CourseEarning.PENDING));
        if (pending.isEmpty()) throw new InvalidRequestException("Nothing is pending for this instructor");
        long sum = 0;
        for (CourseEarning e : pending) sum += e.getOwnerPaise();
        Payout p = new Payout();
        p.setInstructorUserId(instructor.getUserId());
        p.setAmountPaise(sum);
        p.setEarningsCount(pending.size());
        p.setReference(ref);
        p.setPaidByEmail(adminEmail);
        p.setPaidAt(Instant.now());
        Payout saved = DatabaseOperationException.guard("saving a payout", () -> payouts.save(p));
        for (CourseEarning e : pending) {
            e.setStatus(CourseEarning.PAID_OUT);
            e.setPayoutId(saved.getId());
        }
        DatabaseOperationException.guard("saving earnings", () -> earnings.saveAll(pending));
        log.info("Payout {}: {} paise to instructor {} ({} earnings) by {}", saved.getId(), sum, instructor.getUserId(), pending.size(), adminEmail);
        return view(saved, instructor.getUsername());
    }

    // ------------------------------------------------------------------ mapping

    private static EarningRow row(CourseEarning e) {
        EarningRow r = new EarningRow();
        r.id = e.getId();
        r.courseId = e.getCourseId();
        r.courseTitle = e.getCourseTitle();
        r.paymentId = e.getPaymentId();
        r.grossPaise = e.getGrossPaise();
        r.feePaise = e.getFeePaise();
        r.ownerPaise = e.getOwnerPaise();
        r.status = e.getStatus();
        r.createdAt = e.getCreatedAt() == null ? null : e.getCreatedAt().toString();
        return r;
    }

    private static PayoutView view(Payout p, String name) {
        PayoutView v = new PayoutView();
        v.id = p.getId();
        v.instructorUserId = p.getInstructorUserId();
        v.instructorName = name;
        v.amountPaise = p.getAmountPaise();
        v.earningsCount = p.getEarningsCount();
        v.reference = p.getReference();
        v.paidByEmail = p.getPaidByEmail();
        v.paidAt = p.getPaidAt() == null ? null : p.getPaidAt().toString();
        return v;
    }
}
