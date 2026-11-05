package com.ait.mall.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@Schema(description = "Submit an order")
public class OrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Address id; when absent the default address is used")
    private Long addressId;

    @Schema(description = "Buy-now lines; when empty the checked cart lines are used")
    private java.util.List<com.ait.mall.common.dto.OrderItemRequest> items;

    @Schema(description = "Coupon id chosen by the customer")
    private Long couponId;

    /**
     * Set when the customer explicitly declined every coupon. Without this the
     * server would re-apply the best available coupon on checkout, silently
     * overriding the customer's "no coupon" choice.
     */
    @Schema(description = "True to skip coupon auto-selection entirely")
    private boolean couponOptOut;

    @Schema(description = "Customer note")
    private String remark;

    @Schema(description = "wechat | alipay | balance | mock")
    private String payType = "mock";

    @NotBlank(message = "缺少下单来源")
    @Schema(description = "cart | buy_now", example = "cart")
    private String source = "cart";
}
