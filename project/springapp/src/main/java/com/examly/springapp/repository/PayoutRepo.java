package com.examly.springapp.repository;

import com.examly.springapp.model.Payout;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for payouts. */
@Repository
public interface PayoutRepo extends JpaRepository<Payout, Long> {
    List<Payout> findByInstructorUserIdOrderByIdDesc(Long instructorUserId);
    List<Payout> findAllByOrderByIdDesc();
}
