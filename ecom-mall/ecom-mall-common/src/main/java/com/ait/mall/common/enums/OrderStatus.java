package com.ait.mall.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * Order life cycle.
 *
 * <pre>
 *  PENDING_PAYMENT --pay--> PENDING_SHIPMENT --ship--> SHIPPED --confirm--> COMPLETED
 *         |                       |                       |
 *     cancel / timeout         refund                  refund
 *         v                       v                       v
 *     CANCELLED               REFUNDED                REFUNDED
 * </pre>
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT(0, "待付款"),
    PENDING_SHIPMENT(1, "待发货"),
    SHIPPED(2, "待收货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(null);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }

    /** Paid states that count towards GMV. */
    public boolean isRevenue() {
        return this == PENDING_SHIPMENT || this == SHIPPED || this == COMPLETED;
    }
}
