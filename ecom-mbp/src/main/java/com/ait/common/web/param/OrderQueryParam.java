package com.ait.common.web.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Query object that also accepts sorting instructions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Query parameters with sorting")
public class OrderQueryParam extends QueryParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Column to order by, snake_case", example = "created_at")
    private String sortField;

    @Schema(description = "asc or desc", example = "desc")
    private String sortOrder = "desc";

    public boolean isAsc() {
        return "asc".equalsIgnoreCase(sortOrder);
    }
}
