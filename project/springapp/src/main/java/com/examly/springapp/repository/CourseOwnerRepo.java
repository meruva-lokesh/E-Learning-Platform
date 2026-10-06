package com.examly.springapp.repository;

import com.examly.springapp.model.CourseOwner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for course ownership. */
@Repository
public interface CourseOwnerRepo extends JpaRepository<CourseOwner, Long> {
    Optional<CourseOwner> findByCourseId(Long courseId);
    List<CourseOwner> findByInstructorUserId(Long instructorUserId);
    void deleteByCourseId(Long courseId);
}
