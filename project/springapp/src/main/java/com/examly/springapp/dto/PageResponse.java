package com.examly.springapp.dto;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for paged lists: {content, page, size, totalElements, totalPages}.
 * We build this ourselves instead of serialising Spring's PageImpl, whose JSON format is not a public contract.
 *
 * @param <T> type of the items on the page
 * @author Amogh
 */
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    /** Empty first page. */
    public PageResponse() {
        this(new ArrayList<>(), 0, 0, 0L);
    }

    /** All-fields constructor (totalPages is derived, never passed in). */
    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content == null ? new ArrayList<>() : content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = computeTotalPages(totalElements, size);
    }

    /** Paging maths: how many pages are needed for the elements (ceiling division). 0 when empty or size is invalid. */
    public static int computeTotalPages(long totalElements, int size) {
        if (size <= 0 || totalElements <= 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }

    /** Copies the numbers of a Spring Data page into our response. */
    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(springPage.getContent(), springPage.getNumber(), springPage.getSize(),
                springPage.getTotalElements());
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
