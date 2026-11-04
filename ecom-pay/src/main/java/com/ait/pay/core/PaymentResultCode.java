package com.ait.pay.core;

import com.ait.common.api.ResultCode;
import lombok.Getter;

/**
 * Payment specific result codes.
 *
 * <p>Kept in their own enum rather than appended to {@link ResultCode} so the
 * shared module stays free of payment concepts, while still using the same
 * numeric convention (the 4xxx block is reserved for payments).
 */
@Getter
public enum PaymentResultCode {

    PAYMENT_FAILED(4000, "支付失败，请稍后重试"),
    CHANNEL_DISABLED(4001, "该支付渠道当前不可用"),
    CHANNEL_NOT_CONFIGURED(4002, "该支付渠道尚未完成配置"),
    CHANNEL_UNSUPPORTED(4003, "该支付渠道不支持此操作"),
    CHANNEL_NOT_FOUND(4004, "支付渠道不存在"),
    CHANNEL_EXISTS(4005, "支付渠道标识已存在"),
    SIGNATURE_INVALID(4006, "支付回调签名校验失败"),
    NOTIFICATION_INVALID(4007, "支付回调内容无法识别"),
    AMOUNT_MISMATCH(4008, "支付金额与订单金额不一致"),
    ALREADY_PAID(4009, "该订单已完成支付"),
    REFUND_FAILED(4010, "退款失败");

    private final int code;
    private final String message;

    PaymentResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
