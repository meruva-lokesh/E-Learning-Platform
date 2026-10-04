package com.examly.springapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Query parameters of GET /api/course/search. Bound from the query string and validated with @Valid.
 * Every field is optional; the effective*() methods apply the defaults.
 *
 * @author Amogh
 */
public class CourseSearchRequest {
    public static final String DEFAULT_SORT = "newest";
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 9;
    public static final int MAX_SIZE = 50;

    @Size(max = 100, message = "Keyword is too long")
    private String keyword;

    @PositiveOrZero(message = "Minimum price cannot be negative")
    private Double minPrice;

    @PositiveOrZero(message = "Maximum price cannot be negative")
    private Double maxPrice;

    @Pattern(regexp = "^$|^(newest|priceAsc|priceDesc|name)$",
            message = "Sort must be one of: newest, priceAsc, priceDesc, name")
    private String sort;

    @Min(value = 0, message = "Page cannot be negative")
    private Integer page;

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = MAX_SIZE, message = "Size cannot be more than 50")
    private Integer size;

    /** No-arg constructor used by Spring's data binder; all values stay null so the defaults apply. */
    public CourseSearchRequest() {
        this(null, null, null, null, null, null);
    }

    /** All-fields constructor. */
    public CourseSearchRequest(String keyword, Double minPrice, Double maxPrice, String sort, Integer page, Integer size) {
        this.keyword = keyword;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.sort = sort;
        this.page = page;
        this.size = size;
    }

    /** Sort key to use: the default (newest) when none was given. */
    public String effectiveSort() {
        return sort == null || sort.isBlank() ? DEFAULT_SORT : sort;
    }

    /** Page index to use (0-based). */
    public int effectivePage() {
        return page == null ? DEFAULT_PAGE : page;
    }

    /** Page size to use, capped at MAX_SIZE even if validation was bypassed. */
    public int effectiveSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
