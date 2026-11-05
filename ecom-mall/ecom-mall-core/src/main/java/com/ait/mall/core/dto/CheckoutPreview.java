package com.ait.mall.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pre-order calculation so the checkout screen can show the final amount before
 * the customer commits.
 */
@Data
@Schema(description = "Checkout preview")
public class CheckoutPreview implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<com.ait.mall.core.vo.CartItemVo> items = new ArrayList<>();

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal freightAmount = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal payAmount = BigDecimal.ZERO;

    @Schema(description = "Address that will be used")
    private com.ait.mall.core.vo.AddressVo address;

    @Schema(description = "Coupons usable for this basket")
    private List<com.ait.mall.core.vo.MemberCouponVo> availableCoupons = new ArrayList<>();

    private Long selectedCouponId;
}
