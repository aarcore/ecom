package com.ait.pay.core;

import java.math.BigDecimal;

/**
 * A payment to be handed to a channel.
 *
 * <p>Deliberately not the mall's own order type: the gateway module has no
 * dependency on the catalogue or order tables, so a mini-program top-up or a
 * future subscription can reuse it by passing a different subject.
 *
 * @param orderNo    our reference; the channel echoes it back in webhooks
 * @param subject    what the customer sees on the payment sheet
 * @param amount     amount in the major unit of {@code currency}
 * @param currency   ISO 4217 code, upper case
 * @param description optional longer description for channels that show one
 * @param clientIp   buyer IP, required by some channels for fraud checks
 * @param returnUrl  where to send the browser after a redirect flow
 * @param notifyUrl  where the channel posts asynchronous results
 */
public record PayRequest(
        String orderNo,
        String subject,
        BigDecimal amount,
        String currency,
        String description,
        String clientIp,
        String returnUrl,
        String notifyUrl) {

    /** Convenience factory for the common CNY case. */
    public static PayRequest cny(String orderNo, String subject, BigDecimal amount) {
        return new PayRequest(orderNo, subject, amount, "CNY", null, null, null, null);
    }
}
