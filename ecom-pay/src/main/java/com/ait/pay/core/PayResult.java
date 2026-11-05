package com.ait.pay.core;

import java.util.Map;

/**
 * What a channel returns when a payment is opened.
 *
 * <p>The storefront has to cope with three genuinely different flows, so the
 * result describes the flow rather than pretending they are all the same:
 *
 * <ul>
 *   <li>{@link Mode#REDIRECT} — send the browser to {@code url} (Stripe
 *       Checkout, Alipay page pay)</li>
 *   <li>{@link Mode#QR_CODE} — render {@code qrContent} as a QR the customer
 *       scans (WeChat native, Alipay pre-order)</li>
 *   <li>{@link Mode#SDK} — hand {@code sdkParams} to the platform SDK, e.g. a
 *       WeChat JSAPI/小程序 prepay id</li>
 *   <li>{@link Mode#SYNC} — the channel settled inline and no further step is
 *       needed (our mock channel)</li>
 * </ul>
 *
 * @param channelCode the channel that produced this, echoed for the client
 * @param tradeNo     the channel's own id, stored for reconciliation and refunds
 * @param mode        how the client should continue
 * @param url         redirect target, {@code null} for the other modes
 * @param qrContent   raw string to encode as a QR, {@code null} unless QR mode
 * @param sdkParams   extra fields for a platform SDK, may be empty
 * @param paid        true when the payment already succeeded
 * @param message     short human readable note, already localized by the caller
 */
public record PayResult(
        String channelCode,
        String tradeNo,
        Mode mode,
        String url,
        String qrContent,
        Map<String, Object> sdkParams,
        boolean paid,
        String message) {

    public enum Mode {
        REDIRECT,
        QR_CODE,
        SDK,
        SYNC
    }

    public static PayResult sync(String channelCode, String tradeNo, String message) {
        return new PayResult(channelCode, tradeNo, Mode.SYNC, null, null, Map.of(), true, message);
    }

    public static PayResult redirect(String channelCode, String tradeNo, String url) {
        return new PayResult(channelCode, tradeNo, Mode.REDIRECT, url, null, Map.of(), false, null);
    }

    public static PayResult qr(String channelCode, String tradeNo, String qrContent) {
        return new PayResult(channelCode, tradeNo, Mode.QR_CODE, null, qrContent, Map.of(), false, null);
    }

    public static PayResult sdk(String channelCode, String tradeNo, Map<String, Object> params) {
        return new PayResult(channelCode, tradeNo, Mode.SDK, null, null, params, false, null);
    }
}
