package com.examly.springapp.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for the paging maths and the defaults of the search request.
 *
 * @author Amogh
 */
class PageResponseTest {

    @Test
    void totalPagesIsCeilingOfElementsOverSize() {
        assertEquals(0, PageResponse.computeTotalPages(0, 9));
        assertEquals(1, PageResponse.computeTotalPages(1, 9));
        assertEquals(1, PageResponse.computeTotalPages(9, 9));
        assertEquals(2, PageResponse.computeTotalPages(10, 9));
        assertEquals(3, PageResponse.computeTotalPages(25, 10));
    }

    @Test
    void totalPagesIsZeroForInvalidSize() {
        assertEquals(0, PageResponse.computeTotalPages(50, 0));
        assertEquals(0, PageResponse.computeTotalPages(50, -3));
    }

    @Test
    void constructorDerivesTotalPages() {
        List<String> items = Arrays.asList("a", "b");
        PageResponse<String> page = new PageResponse<>(items, 1, 2, 5);
        assertEquals(items, page.getContent());
        assertEquals(1, page.getPage());
        assertEquals(2, page.getSize());
        assertEquals(5L, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
    }

    @Test
    void noArgConstructorGivesAnEmptyPage() {
        PageResponse<String> page = new PageResponse<>();
        assertNotNull(page.getContent());
        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalPages());
    }

    @Test
    void searchRequestDefaultsAreApplied() {
        CourseSearchRequest request = new CourseSearchRequest();
        assertEquals("newest", request.effectiveSort());
        assertEquals(0, request.effectivePage());
        assertEquals(9, request.effectiveSize());
    }

    @Test
    void searchRequestSizeIsCappedAtFifty() {
        assertEquals(50, new CourseSearchRequest(null, null, null, null, 0, 500).effectiveSize());
        assertEquals(1, new CourseSearchRequest(null, null, null, null, 0, 0).effectiveSize());
        assertEquals(20, new CourseSearchRequest(null, null, null, "name", 2, 20).effectiveSize());
    }

    @Test
    void ratingIsRoundedToOneDecimal() {
        assertEquals(4.3, CourseRatingDTO.round1(4.26), 1e-9);
        assertEquals(4.0, CourseRatingDTO.round1(4.0), 1e-9);
        assertEquals(3.7, new CourseRatingDTO("Java", 3.6667, 3).getAverageRating(), 1e-9);
        assertEquals(3L, new CourseRatingDTO("Java", 3.6667, 3).getReviewCount());
    }
}
