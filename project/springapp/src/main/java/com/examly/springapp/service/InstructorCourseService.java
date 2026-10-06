package com.examly.springapp.service;

import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.CourseOwner;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.CourseOwnerRepo;
import com.examly.springapp.repository.CourseRepo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Course management for an APPROVED instructor: create, view, edit and delete, always only their own courses.
 * The real work is done by the existing CourseService, so every course rule stays in one place.
 */
@Service
public class InstructorCourseService {
    private final InstructorService instructors;
    private final CourseService courseService;
    private final CourseRepo courseRepo;
    private final CourseOwnerRepo owners;
    // looked up only when a course is deleted, so VideoService (which needs this class) can be one of them without a start-up loop
    private final ObjectProvider<CourseDeleteListener> listeners;

    @Autowired
    public InstructorCourseService(InstructorService instructors, CourseService courseService, CourseRepo courseRepo,
                                   CourseOwnerRepo owners,
                                   ObjectProvider<CourseDeleteListener> listeners) {
        this.instructors = instructors;
        this.courseService = courseService;
        this.courseRepo = courseRepo;
        this.owners = owners;
        this.listeners = listeners;
    }

    public Course create(String email, Course course) {
        User u = instructors.requireApproved(email);
        Course saved = courseService.addCourse(course);
        DatabaseOperationException.guardVoid("saving the course owner", () -> owners.save(new CourseOwner(saved.getCourseId(), u.getUserId())));
        return saved;
    }

    public List<Course> mine(String email) {
        User u = instructors.requireApproved(email);
        List<CourseOwner> rows = DatabaseOperationException.guard("loading your courses", () -> owners.findByInstructorUserId(u.getUserId()));
        List<Course> out = new ArrayList<>();
        for (CourseOwner o : rows) {
            Optional<Course> c = DatabaseOperationException.guard("loading a course", () -> courseRepo.findById(o.getCourseId()));
            c.ifPresent(out::add); // a course deleted by an admin simply disappears from the list
        }
        return out;
    }

    public Course update(String email, Long courseId, Course course) {
        User u = instructors.requireApproved(email);
        requireOwner(u, courseId);
        return courseService.updateCourse(courseId, course);
    }

    public void delete(String email, Long courseId) {
        User u = instructors.requireApproved(email);
        requireOwner(u, courseId);
        listeners.orderedStream().forEach(l -> l.onCourseDeleted(courseId));
        courseService.deleteCourse(courseId);
        DatabaseOperationException.guardVoid("removing the course owner", () -> owners.deleteByCourseId(courseId));
    }

    /** True when this approved instructor owns the course. */
    public boolean owns(User instructor, Long courseId) {
        Optional<CourseOwner> o = DatabaseOperationException.guard("checking the course owner", () -> owners.findByCourseId(courseId));
        return o.isPresent() && o.get().getInstructorUserId().equals(instructor.getUserId());
    }

    private void requireOwner(User u, Long courseId) {
        if (courseId == null || courseId <= 0) throw new ResourceNotFoundException("Course not found");
        if (!owns(u, courseId)) {
            // the same answer for "not yours" and "does not exist", so ids cannot be probed
            throw new AccessDeniedException("You can only manage your own courses");
        }
    }
}
