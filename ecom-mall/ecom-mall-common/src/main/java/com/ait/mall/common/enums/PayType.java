package com.ait.mall.common.enums;

import lombok.Getter;

/**
 * Payment channels. {@code MOCK} is used by the demo environment so the whole
 * purchase flow can be exercised without real merchant credentials.
 */
@Getter
public enum PayType {

    WECHAT("wechat", "微信支付"),
    ALIPAY("alipay", "支付宝"),
    BALANCE("balance", "余额支付"),
    MOCK("mock", "模拟支付");

    private final String code;
    private final String label;

    PayType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static PayType of(String code) {
        for (PayType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return MOCK;
    }
}
