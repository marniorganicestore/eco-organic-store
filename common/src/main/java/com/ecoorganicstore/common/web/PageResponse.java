package com.ecoorganicstore.common.web;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        int safeSize = Math.max(size, 1);
        int totalPages = totalElements <= 0 ? 0 : (int) Math.ceil(totalElements / (double) safeSize);
        boolean hasNext = (long) (page + 1) * safeSize < totalElements;
        return new PageResponse<>(List.copyOf(items), page, safeSize, Math.max(totalElements, 0), totalPages, hasNext);
    }
}
