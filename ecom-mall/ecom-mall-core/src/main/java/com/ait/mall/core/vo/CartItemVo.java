package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Cart line joined with live product and SKU data.
 */
@Data
@Schema(description = "Cart line")
public class CartItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private Long skuId;

    private String productName;

    private String cover;

    private String specValues;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer quantity;

    private Integer stock;

    private Integer checked;

    @Schema(description = "product on sale and stock sufficient for the quantity")
    private Boolean available;

    public BigDecimal getLineAmount() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
