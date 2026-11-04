package com.ait.pay.core;

import java.util.Map;

/**
 * A verified, channel-agnostic payment notification.
 *
 * <p>Channels differ wildly here — Stripe sends signed JSON, WeChat sends
 * AES-encrypted resources, Alipay sends form-encoded fields signed with RSA2 —
 * so each adapter verifies and decrypts on its own and hands us this shape. The
 * order flow then only has to look at {@code paid} and {@code orderNo}.
 *
 * @param channelCode  which channel sent this
 * @param orderNo      our reference, resolved from the channel payload
 * @param tradeNo      the channel transaction id
 * @param amount       amount the channel says was paid
 * @param currency     ISO 4217 code
 * @param paid         true when the channel reports success
 * @param rawStatus    the channel's own status string, kept for support tickets
 * @param extra        anything channel specific worth persisting
 */
public record PayNotification(
        String channelCode,
        String orderNo,
        String tradeNo,
        java.math.BigDecimal amount,
        String currency,
        boolean paid,
        String rawStatus,
        Map<String, Object> extra) {

    public boolean matchesAmount(java.math.BigDecimal expected) {
        // channel amounts must be compared with compareTo, never equals: 10.0
        // and 10.00 are the same money but different BigDecimal instances
        return amount != null && expected != null && amount.compareTo(expected) == 0;
    }
}
