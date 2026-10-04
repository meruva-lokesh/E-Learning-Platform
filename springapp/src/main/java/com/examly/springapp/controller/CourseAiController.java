package com.examly.springapp.controller;

import com.examly.springapp.config.AiUsageLimiter;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.model.Course;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.CourseAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI course search (semantic with Gemini, lexical fallback). Limited per user so nobody can burn the AI quota.
 *
 * @author Sumit
 */
@RestController
@RequestMapping("/api/course-ai")
@Tag(name = "AI course search", description = "Natural-language course search")
public class CourseAiController {
    private static final Logger log = LoggerFactory.getLogger(CourseAiController.class);

    private final CourseAiService courseAiService;
    private final AiUsageLimiter limiter;
    private final AccessService access;
    private final int maxQueryLength;

    @Autowired
    public CourseAiController(CourseAiService courseAiService, AiUsageLimiter limiter, AccessService access,
                              @Value("${ai.max-query-length:200}") int maxQueryLength) {
        this.courseAiService = courseAiService;
        this.limiter = limiter;
        this.access = access;
        this.maxQueryLength = maxQueryLength;
    }

    @Operation(summary = "Search courses with natural language",
            description = "CUSTOMER only. Body: {\"query\": \"...\"}. A blank query returns all courses without using the AI quota.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courses ordered by relevance"),
            @ApiResponse(responseCode = "400", description = "Search text too long"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer"),
            @ApiResponse(responseCode = "429", description = "AI usage limit reached"),
            @ApiResponse(responseCode = "502", description = "AI service unavailable")
    })
    @PostMapping("/search")
    public ResponseEntity<List<Course>> search(@RequestBody(required = false) Map<String, String> body) {
        log.trace("AI search called");
        String query = body == null ? null : body.get("query");
        if (query != null && query.length() > maxQueryLength) {
            throw new InvalidRequestException("Search text is too long (maximum " + maxQueryLength + " characters)");
        }
        if (query != null) {
            query = query.replaceAll("\\p{Cntrl}", " ").trim();
        }
        // blank queries return the plain list without calling the AI, so they do not use up the quota
        if (query != null && !query.isEmpty()) {
            limiter.check(access.currentEmail());
        }
        List<Course> courses = courseAiService.searchCourses(query);
        log.debug("AI search returned {} course(s)", courses.size());
        return ResponseEntity.status(HttpStatus.OK).body(courses);
    }
}
