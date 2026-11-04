package com.ait.mall.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * One line of a "buy now" or cart-checkout submission.
 */
@Data
@Schema(description = "Checkout line")
public class OrderItemRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品不能为空")
    @Schema(description = "SKU id")
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1")
    @Schema(description = "Quantity")
    private Integer quantity;
}
