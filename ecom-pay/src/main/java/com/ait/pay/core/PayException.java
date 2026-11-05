package com.ait.pay.core;

import com.ait.common.exception.BizException;

/**
 * A payment failure that is safe to show a customer.
 *
 * <p>Extends {@link BizException} so {@code GlobalExceptionHandler} renders it in
 * the standard envelope with no extra wiring, while the payment module keeps its
 * own code block ({@link PaymentResultCode}, 4xxx) — {@code BizException} accepts
 * a raw int precisely so other modules can define their own codes without the
 * common module having to know about them.
 *
 * <p>The message must never contain credentials or raw channel payloads; those
 * go to the log instead.
 */
public class PayException extends BizException {

    private static final long serialVersionUID = 1L;

    public PayException(String message) {
        super(PaymentResultCode.PAYMENT_FAILED.getCode(), message);
    }

    public PayException(PaymentResultCode resultCode) {
        super(resultCode.getCode(), resultCode.getMessage());
    }

    public PayException(PaymentResultCode resultCode, String message) {
        super(resultCode.getCode(), message);
    }

    public static PayException channelDisabled(String code) {
        return new PayException(PaymentResultCode.CHANNEL_DISABLED,
                "支付渠道「" + code + "」当前不可用");
    }

    public static PayException notConfigured(String code) {
        return new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                "支付渠道「" + code + "」尚未完成配置");
    }

    public static PayException unsupported(String what) {
        return new PayException(PaymentResultCode.CHANNEL_UNSUPPORTED, what);
    }
}
