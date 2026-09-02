package com.ankkun.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> data;
    private int page;
    private int limit;
    private long total;
    private int totalPages;

    public static <T> PaginatedResponse<T> of(List<T> data, int page, int limit, long total) {
        int totalPages = (int) Math.ceil((double) total / limit);
        return new PaginatedResponse<T>(data, page, limit, total, totalPages);
    }
}
