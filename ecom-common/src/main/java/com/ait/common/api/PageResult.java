package com.ait.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Paged payload used by every list endpoint.
 */
@Data
@Schema(description = "Paged result")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Current page, 1 based")
    private long page = 1;

    @Schema(description = "Page size")
    private long size = 10;

    @Schema(description = "Total rows")
    private long total = 0;

    @Schema(description = "Rows of the current page")
    private List<T> records = Collections.emptyList();

    public PageResult() {
    }

    public PageResult(long page, long size, long total, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
    }

    public static <T> PageResult<T> of(long page, long size, long total, List<T> records) {
        return new PageResult<>(page, size, total, records);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(page, size, 0, Collections.emptyList());
    }

    public long getPages() {
        if (size <= 0) {
            return 0;
        }
        long pages = total / size;
        return total % size == 0 ? pages : pages + 1;
    }
}
