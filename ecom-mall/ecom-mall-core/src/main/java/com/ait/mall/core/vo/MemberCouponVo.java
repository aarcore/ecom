package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Coupon held by the current member")
public class MemberCouponVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long couponId;

    private String name;

    /** amount | discount */
    private String type;

    private BigDecimal amount;

    private BigDecimal discount;

    private BigDecimal minAmount;

    /** 0 unused / 1 used / 2 expired */
    private Integer status;

    private String statusText;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Schema(description = "Computed usability for a given basket total; null when not evaluated")
    private Boolean usable;

    @Schema(description = "Discount this coupon would apply")
    private BigDecimal discountValue;
}
