package com.examly.springapp.repository;

import com.examly.springapp.model.CourseVideo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for video details. */
@Repository
public interface CourseVideoRepo extends JpaRepository<CourseVideo, Long> {
    List<CourseVideo> findByCourseIdOrderByIdAsc(Long courseId);
    List<CourseVideo> findByCourseIdIn(List<Long> courseIds);
    long countByCourseId(Long courseId);
}
