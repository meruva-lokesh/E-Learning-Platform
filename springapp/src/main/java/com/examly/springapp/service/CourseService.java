package com.examly.springapp.service;

import com.examly.springapp.dto.CourseSearchRequest;
import com.examly.springapp.dto.PageResponse;
import com.examly.springapp.model.Course;
import java.util.List;

/**
 * Business operations on courses: management (create, update, delete), viewing and search.
 *
 * @author Sivamuthu
 * @author Amogh (search)
 */
public interface CourseService {
    Course addCourse(Course course);
    List<Course> getAllCourses();
    Course getCourseById(Long courseId);
    Course updateCourse(Long courseId, Course course);
    void deleteCourse(Long courseId);

    /**
     * Keyword / price-range search with sorting and paging.
     *
     * @author Amogh
     */
    PageResponse<Course> searchCourses(CourseSearchRequest request);
}
