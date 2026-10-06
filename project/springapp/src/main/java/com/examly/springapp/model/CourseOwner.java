package com.examly.springapp.model;

import jakarta.persistence.*;

/**
 * Says which instructor created a course. It is a separate table so the Course table (and the course API
 * the tests rely on) stays exactly as it was. Courses without a row here were made by an admin.
 */
@Entity
@Table(name = "course_owners")
public class CourseOwner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long courseId;

    @Column(nullable = false)
    private Long instructorUserId;

    public CourseOwner() {}

    public CourseOwner(Long courseId, Long instructorUserId) {
        this.courseId = courseId;
        this.instructorUserId = instructorUserId;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getInstructorUserId() { return instructorUserId; }
}
