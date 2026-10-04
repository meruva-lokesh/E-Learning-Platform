package com.examly.springapp.controller;

import com.examly.springapp.dto.CourseSearchRequest;
import com.examly.springapp.dto.PageResponse;
import com.examly.springapp.model.Course;
import com.examly.springapp.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Course endpoints: management (ADMIN) and viewing / search (ADMIN and CUSTOMER).
 *
 * <p><b>ResponseEntity styles.</b> In this project every controller method builds its answer with
 * {@code ResponseEntity.status(HttpStatus.X).body(...)} because the status is then visible in one place.
 * These are the equivalent alternatives you may meet elsewhere:
 * <ul>
 *   <li>{@code ResponseEntity.ok(body)} - shortcut for 200 with a body</li>
 *   <li>{@code ResponseEntity.created(uri).body(body)} - 201 plus a Location header pointing at the new resource</li>
 *   <li>{@code new ResponseEntity<>(body, status)} - constructor form with an explicit status</li>
 *   <li>{@code ResponseEntity.noContent().build()} - 204 without a body</li>
 *   <li>{@code ResponseEntity.notFound().build()} - 404 without a body</li>
 *   <li>{@code ResponseEntity.of(Optional)} - 200 with the value, or 404 when the Optional is empty</li>
 * </ul>
 *
 * @author Sivamuthu
 * @author Amogh (search endpoint)
 */
@RestController
@RequestMapping("/api/course")
@Validated
@Tag(name = "Courses", description = "Create, view, search, update and delete courses")
public class CourseController {
    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    private final CourseService courseService;

    @Autowired
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "Add a course", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Course created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @PostMapping
    public ResponseEntity<Course> addCourse(@Valid @RequestBody Course course) {
        log.trace("addCourse called");
        Course saved = courseService.addCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "List all courses", description = "ADMIN and CUSTOMER. Returns the plain list (no paging); use /search for paging.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of courses (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        log.trace("getAllCourses called");
        List<Course> courses = courseService.getAllCourses();
        log.debug("getAllCourses: returning {} course(s)", courses.size());
        return ResponseEntity.status(HttpStatus.OK).body(courses);
    }

    /**
     * Declared before the "/{courseId}" mapping on purpose: Spring always prefers the literal path
     * "/search" over the variable path, so "search" is never read as a course id.
     *
     * @author Amogh
     */
    @Operation(summary = "Search courses (paged)",
            description = "ADMIN and CUSTOMER. Case-insensitive keyword on course type or details, optional price range, "
                    + "sort = newest (default) | priceAsc | priceDesc | name, page (default 0), size (default 9, max 50). "
                    + "Returns {content, page, size, totalElements, totalPages}.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of courses (content may be empty)"),
            @ApiResponse(responseCode = "400", description = "Invalid sort, page, size or price range"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping("/search")
    public ResponseEntity<PageResponse<Course>> searchCourses(@Valid @ParameterObject @ModelAttribute CourseSearchRequest request) {
        log.trace("searchCourses called");
        PageResponse<Course> result = courseService.searchCourses(request);
        log.debug("searchCourses: page {} of {} with {} course(s)", result.getPage(), result.getTotalPages(), result.getContent().size());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @Operation(summary = "Update a course", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PutMapping("/{courseId}")
    public ResponseEntity<Course> updateCourse(@PathVariable @Min(value = 1, message = "must be a positive number") Long courseId,
                                               @Valid @RequestBody Course course) {
        log.trace("updateCourse called for courseId={}", courseId);
        return ResponseEntity.status(HttpStatus.OK).body(courseService.updateCourse(courseId, course));
    }

    // path from the API table; the shorter form below is used by the Angular service
    @Operation(summary = "Get a course by id (SRS table path)", description = "ADMIN and CUSTOMER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<Course> getCourseByIdTable(@PathVariable @Min(value = 1, message = "must be a positive number") Long courseId) {
        log.trace("getCourseByIdTable called for courseId={}", courseId);
        return ResponseEntity.status(HttpStatus.OK).body(courseService.getCourseById(courseId));
    }

    @Operation(summary = "Get a course by id", description = "ADMIN and CUSTOMER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<Course> getCourseById(@PathVariable @Min(value = 1, message = "must be a positive number") Long courseId) {
        log.trace("getCourseById called for courseId={}", courseId);
        return ResponseEntity.status(HttpStatus.OK).body(courseService.getCourseById(courseId));
    }

    @Operation(summary = "Delete a course", description = "ADMIN only. The course is also removed from carts and orders.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable @Min(value = 1, message = "must be a positive number") Long courseId) {
        log.trace("deleteCourse called for courseId={}", courseId);
        courseService.deleteCourse(courseId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
