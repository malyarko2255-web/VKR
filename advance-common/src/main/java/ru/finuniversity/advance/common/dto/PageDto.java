package ru.finuniversity.advance.common.dto;

import java.util.List;

public class PageDto<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final int totalPages;
    private final long totalElements;

    public PageDto(List<T> content, int page, int size, int totalPages, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public List<T> getContent()      { return content; }
    public int getPage()             { return page; }
    public int getSize()             { return size; }
    public int getTotalPages()       { return totalPages; }
    public long getTotalElements()   { return totalElements; }
}
