package com.examly.springapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examly.springapp.model.Course;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the AI search maths (cosine, token overlap).
 *
 * @author Sumit
 */
class CourseAiServiceTest {

    @Test
    void cosineOfIdenticalVectorsIsOne() {
        assertEquals(1.0, CourseAiService.cosine(new float[]{1, 2, 3}, new float[]{1, 2, 3}), 1e-6);
    }

    @Test
    void cosineOfOrthogonalVectorsIsZero() {
        assertEquals(0.0, CourseAiService.cosine(new float[]{1, 0}, new float[]{0, 1}), 1e-6);
    }

    @Test
    void cosineWithZeroVectorIsZero() {
        assertEquals(0.0, CourseAiService.cosine(new float[]{0, 0}, new float[]{1, 1}), 1e-6);
    }

    @Test
    void jaccardCountsSharedWords() {
        Set<String> a = CourseAiService.tokens("cheap beginner java");
        Set<String> b = CourseAiService.tokens("Java for beginner developers");
        assertTrue(CourseAiService.jaccard(a, b) > 0);
        assertEquals(0.0, CourseAiService.jaccard(a, CourseAiService.tokens("cooking pasta")), 1e-6);
    }

    @Test
    void profileTextMentionsTypeDetailsAndPrice() {
        Course c = new Course();
        c.setCourseType("Java");
        c.setCourseDetails("master java");
        c.setCoursePrice(200.0);
        String profile = CourseAiService.profileText(c);
        assertTrue(profile.contains("Java") && profile.contains("master java") && profile.contains("200"));
    }
}
