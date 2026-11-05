package com.ait.mall.core.query;

import com.ait.annotation.Query;
import com.ait.common.web.param.OrderQueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Admin product search. Declared with {@link Query} annotations so the
 * condition building lives next to the searchable fields instead of inside a
 * growing chain of {@code if} statements in the service.
 *
 * <p>The {@code blurry} list makes the keyword search three columns at once —
 * name, subtitle and keywords — which is what operators actually expect when
 * they paste a term into the search box.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminProductQuery extends OrderQueryParam {

    private static final long serialVersionUID = 1L;

    @Query(type = Query.Type.GREATER_THAN_NQ, propName = "id")
    private Long afterId;

    @Query(type = Query.Type.INNER_LIKE, blurry = "name,subtitle,keywords")
    private String keyword;

    @Query(propName = "category_id")
    private Long categoryId;

    @Query(propName = "brand_id")
    private Long brandId;

    @Query
    private Integer status;

    /** Upper bound used by the low-stock widget. */
    @Query(type = Query.Type.LESS_THAN, propName = "stock")
    private Integer maxStock;
}
