package com.ait.pay.service;

import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.core.PayResult;

import java.util.Map;

/**
 * Facade the commerce layer calls into.
 *
 * <p>It owns the parts that are the same for every provider — resolving the
 * channel, checking it is enabled, normalising errors — so each {@code
 * PayChannel} only has to speak its own protocol.
 *
 * <p>It deliberately knows nothing about orders: the callback is handed back to
 * a listener supplied by the calling module. That keeps the dependency arrow
 * pointing one way, mall → pay, and makes the gateway reusable.
 */
public interface PayGateway {

    /** Opens a payment on the channel named in the request's config. */
    PayResult create(String channelCode, PayRequest request);

    /**
     * Verifies and normalises a callback.
     *
     * @param channelCode which endpoint received it
     * @param headers     HTTP headers, needed for signatures
     * @param body        raw body — must not be re-serialised
     */
    PayNotification verify(String channelCode, Map<String, String> headers, String body);

    /**
     * Refunds through the channel that took the money.
     *
     * @param tradeNo the channel transaction id (not our order number)
     * @param amount  amount to refund; {@code null} means the full amount
     * @return the channel's refund id
     */
    String refund(String channelCode, String tradeNo, java.math.BigDecimal amount, String reason);

    /** The body a channel expects in reply, so it stops retrying. */
    Ack ack(String channelCode);

    record Ack(String body, String contentType) {
    }

    /** Thrown when the channel exists but is switched off or unsupported. */
    void assertAvailable(String channelCode);
}
