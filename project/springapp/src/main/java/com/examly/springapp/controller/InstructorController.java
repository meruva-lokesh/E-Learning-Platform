package com.examly.springapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.RateLimiter;
import com.examly.springapp.dto.InstructorDtos.ApplicationStatusRequest;
import com.examly.springapp.dto.InstructorDtos.ApplicationStatusView;
import com.examly.springapp.dto.InstructorDtos.InstructorDetails;
import com.examly.springapp.dto.InstructorDtos.InstructorRegisterRequest;
import com.examly.springapp.dto.InstructorDtos.InstructorView;
import com.examly.springapp.dto.InstructorDtos.ResubmitRequest;
import com.examly.springapp.exception.RateLimitExceededException;
import com.examly.springapp.model.Course;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.InstructorCourseService;
import com.examly.springapp.service.InstructorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Instructor registration (public), the instructor's own application, and their
 * course CRUD (approved
 * only).
 */
@RestController
@RequestMapping("/api/instructor")
@Tag(name = "Instructor", description = "Instructor registration, application status and own courses")
public class InstructorController {
    private final InstructorService instructors;
    private final InstructorCourseService courses;
    private final AccessService access;
    private final RateLimiter limiter;

    @Autowired
    public InstructorController(InstructorService instructors, InstructorCourseService courses,
            AccessService access, RateLimiter limiter) {
        this.instructors = instructors;
        this.courses = courses;
        this.access = access;
        this.limiter = limiter;
    }

    @Operation(summary = "Register as an instructor (public)", description = "Creates the account (role INSTRUCTOR) and a PENDING application. The admindecides later.")

    @PostMapping("/register")
    public ResponseEntity<InstructorView> register(@RequestBody InstructorRegisterRequest body,
            HttpServletRequest request) {
        String key = "instructor-register:" + request.getRemoteAddr();
        if (!limiter.tryAcquire(key, 10, 60 * 60_000L)) {
            throw new RateLimitExceededException("Too many registrations from this device. Try again later.",
                    limiter.retryAfterSeconds(key, 60 * 60_000L));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(instructors.register(body));
    }

    @Operation(summary = "Check an application by username or e-mail (public)", description = "Body: {identifier}. Answer: status, dates, the rejection reason and whether theinstructor can log in. No phone number or bio.")

    @PostMapping("/status")
    public ResponseEntity<ApplicationStatusView> status(@RequestBody ApplicationStatusRequest body,
            HttpServletRequest request) {
        String id = body == null || body.identifier() == null ? "" : body.identifier().trim().toLowerCase();
        guard("instructor-status:ip:" + request.getRemoteAddr(), 30,
                "Too many checks from this device. Try again later.");
        guard("instructor-status:id:" + id, 10, "Too many checks for this name or e-mail. Try again later.");
        return ResponseEntity.ok(instructors.applicationStatus(body == null ? null : body.identifier()));
    }

    @Operation(summary = "Fix a pending or rejected application without logging in (public)", description = "Body: {email, password, qualification, experienceYears, expertise, bio,profileLink}.A rejected application goes back to PENDING.")

    @PostMapping("/resubmit")
    public ResponseEntity<InstructorView> resubmit(@RequestBody ResubmitRequest body, HttpServletRequest request) {
        String email = body == null || body.email() == null ? "" : body.email().trim().toLowerCase();
        guard("instructor-resubmit:ip:" + request.getRemoteAddr(), 20,
                "Too many attempts from this device.Try again later.");
        guard("instructor-resubmit:email:" + email, 10, "Too many attempts for this account. Try again later.");
        return ResponseEntity.ok(instructors.resubmit(body));
    }

    private void guard(String key, int perHour, String message) {
        if (!limiter.tryAcquire(key, perHour, 60 * 60_000L)) {
            throw new RateLimitExceededException(message, limiter.retryAfterSeconds(key, 60 * 60_000L));
        }
    }

    @Operation(summary = "My application and its status")
    @GetMapping("/me")
    public ResponseEntity<InstructorView> me() {
        return ResponseEntity.ok(instructors.myProfile(access.currentEmail()));
    }

    @Operation(summary = "Fix my application", description = "Allowed while PENDING; a REJECTED applicationgoes backto PENDING.")

    @PutMapping("/me")
    public ResponseEntity<InstructorView> updateMe(@RequestBody InstructorDetails body) {
        return ResponseEntity.ok(instructors.updateMine(access.currentEmail(), body));
    }

    @Operation(summary = "My courses (approved instructors)")
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> myCourses() {
        return ResponseEntity.ok(courses.mine(access.currentEmail()));
    }

    @Operation(summary = "Add a course (approved instructors)")
    @PostMapping("/courses")
    public ResponseEntity<Course> add(@Valid @RequestBody Course course) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courses.create(access.currentEmail(),
                course));
    }

    @Operation(summary = "Edit one of my courses")
    @PutMapping("/courses/{courseId}")
    public ResponseEntity<Course> update(@PathVariable Long courseId, @Valid @RequestBody Course course) {
        return ResponseEntity.ok(courses.update(access.currentEmail(), courseId, course));
    }

    @Operation(summary = "Delete one of my courses")
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> delete(@PathVariable Long courseId) {
        courses.delete(access.currentEmail(), courseId);
        return ResponseEntity.noContent().build();
    }
}