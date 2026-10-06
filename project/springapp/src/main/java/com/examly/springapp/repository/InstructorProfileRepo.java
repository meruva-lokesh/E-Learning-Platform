package com.examly.springapp.repository;

import com.examly.springapp.model.InstructorProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for instructor applications. */
@Repository
public interface InstructorProfileRepo extends JpaRepository<InstructorProfile, Long> {
    Optional<InstructorProfile> findByUserId(Long userId);
    List<InstructorProfile> findByStatusOrderBySubmittedAtDesc(String status);
    List<InstructorProfile> findAllByOrderBySubmittedAtDesc();
}
