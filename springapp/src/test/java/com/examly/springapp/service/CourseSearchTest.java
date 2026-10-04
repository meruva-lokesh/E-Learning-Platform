package com.examly.springapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * Pure unit tests for the sort selection and the LIKE pattern of the course search.
 *
 * @author Amogh
 */
class CourseSearchTest {

    @Test
    void newestIsTheDefaultAndSortsByIdDescending() {
        Sort sort = CourseServiceImpl.sortFor(null);
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("courseId").getDirection());
        assertEquals(Sort.Direction.DESC, CourseServiceImpl.sortFor("newest").getOrderFor("courseId").getDirection());
        assertEquals(Sort.Direction.DESC, CourseServiceImpl.sortFor("something-else").getOrderFor("courseId").getDirection());
    }

    @Test
    void priceSortsUseThePriceColumn() {
        assertEquals(Sort.Direction.ASC, CourseServiceImpl.sortFor("priceAsc").getOrderFor("coursePrice").getDirection());
        assertEquals(Sort.Direction.DESC, CourseServiceImpl.sortFor("priceDesc").getOrderFor("coursePrice").getDirection());
    }

    @Test
    void nameSortsByCourseTypeAscending() {
        assertEquals(Sort.Direction.ASC, CourseServiceImpl.sortFor("name").getOrderFor("courseType").getDirection());
    }

    @Test
    void blankKeywordMatchesEverything() {
        assertEquals("%", CourseServiceImpl.likePattern(null));
        assertEquals("%", CourseServiceImpl.likePattern("   "));
    }

    @Test
    void keywordIsTrimmedLowerCasedAndWrapped() {
        assertEquals("%java%", CourseServiceImpl.likePattern("  JaVa "));
    }

    @Test
    void wildcardCharactersAreEscaped() {
        assertEquals("%100!%%", CourseServiceImpl.likePattern("100%"));
        assertEquals("%a!_b%", CourseServiceImpl.likePattern("a_b"));
        assertEquals("%hi!!%", CourseServiceImpl.likePattern("hi!"));
    }
}
