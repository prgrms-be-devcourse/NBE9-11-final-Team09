package com.back.team9.moyeota.global.response;

import org.springframework.data.domain.Page;

public record PageInfoResponse(
        int currentPage,
        int totalPages,
        long totalElements,
        int size,
        boolean isLast
) {
    public static PageInfoResponse from(Page<?> page) {
        return new PageInfoResponse(
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isLast()
        );
    }
}