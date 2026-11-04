package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Immutable order line shown on both the admin order detail and the customer
 * order detail screens.
 */
@Data
@Schema(description = "Order line")
public class OrderItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private Long skuId;

    private String productName;

    private String productCover;

    private String specValues;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalAmount;
}
