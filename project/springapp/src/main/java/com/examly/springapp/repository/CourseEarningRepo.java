package com.examly.springapp.repository;

import com.examly.springapp.model.CourseEarning;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for the instructor earnings ledger. */
@Repository
public interface CourseEarningRepo extends JpaRepository<CourseEarning, Long> {
    boolean existsByPaymentId(Long paymentId);
    List<CourseEarning> findByInstructorUserIdOrderByIdDesc(Long instructorUserId);
    List<CourseEarning> findByInstructorUserIdAndStatus(Long instructorUserId, String status);
    List<CourseEarning> findByStatus(String status);
}
