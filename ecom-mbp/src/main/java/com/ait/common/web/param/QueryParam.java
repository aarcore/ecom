package com.ait.common.web.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Base query object. {@code page} is 1 based so it matches the admin tables and
 * the storefront infinite scroll cursor equally well.
 */
@Data
@Schema(description = "Paging query parameters")
public class QueryParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Page number, starting at 1", example = "1")
    private Integer page = 1;

    @Schema(description = "Rows per page", example = "10")
    private Integer limit = 10;

    @Schema(description = "Free text keyword")
    private String keyword;

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safeLimit() {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 100);
    }

    /** Alias accepted from Ant Design style clients. */
    public void setCurrent(Integer current) {
        this.page = (current == null || current <= 0) ? 1 : current;
    }

    /** Alias accepted from Ant Design style clients. */
    public void setSize(Integer size) {
        this.limit = (size == null || size <= 0) ? 10 : size;
    }
}
