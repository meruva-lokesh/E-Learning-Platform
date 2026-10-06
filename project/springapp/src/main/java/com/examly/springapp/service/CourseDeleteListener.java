package com.examly.springapp.service;

/**
 * Anything that owns data tied to a course (for example its videos) can implement this so that data is
 * cleaned up when an instructor deletes the course.
 */
public interface CourseDeleteListener {
    void onCourseDeleted(Long courseId);
}
