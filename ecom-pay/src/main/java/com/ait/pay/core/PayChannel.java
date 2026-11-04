package com.ait.pay.core;

import java.math.BigDecimal;

/**
 * One payment provider.
 *
 * <p>Implementation contract:
 * <ul>
 *   <li>{@link #code()} must be stable and unique — it is the key the admin
 *       console toggles and what gets written to {@code shop_pay_record}</li>
 *   <li>every method receives {@link ChannelConfig} rather than reading
 *       properties itself, so an operator can change keys in the console
 *       without a redeploy</li>
 *   <li>methods must not touch the commerce database: the gateway is a leaf
 *       module and the order flow calls back into it</li>
 * </ul>
 *
 * <p>Adding a provider is: implement this interface, annotate it
 * {@code @Component}, insert a {@code shop_pay_channel} row. Nothing else in
 * the codebase changes.
 */
public interface PayChannel {

    /** Stable identifier, e.g. {@code stripe}. */
    String code();

    /** Display name, e.g. {@code Stripe}. */
    String label();

    /**
     * True when this channel needs the customer's browser to leave our page
     * (or scan a code). Used by the admin console to label the channel and by
     * the client to decide whether to open a new tab.
     */
    default boolean requiresRedirect() {
        return false;
    }

    /**
     * Opens a payment.
     *
     * @throws PayException when the channel is misconfigured or refuses the
     *                      request; the message is shown to the customer
     */
    PayResult createPayment(PayRequest request, ChannelConfig config);

    /**
     * Verifies an asynchronous notification.
     *
     * <p>Implementations must verify the signature before trusting anything in
     * the payload, and must be idempotent: channels retry until they get a 2xx.
     *
     * @param headers HTTP headers, needed for signature verification
     * @param body    raw request body, byte exact — re-serialising breaks signatures
     * @param config  the channel's configuration, which carries the webhook
     *                signing secret and any sandbox/live distinction
     */
    PayNotification parseNotification(java.util.Map<String, String> headers, String body,
                                      ChannelConfig config);

    /**
     * Refunds all or part of a payment.
     *
     * @return the channel's refund id
     */
    String refund(String tradeNo, BigDecimal amount, String reason, ChannelConfig config);

    /**
     * Cheap configuration self-check used by the admin console's "test
     * connection" button. Returns null when healthy, a human readable problem
     * otherwise.
     */
    String checkConfig(ChannelConfig config);

    /**
     * The response body a channel expects after a successful webhook, which
     * differs per provider (Stripe wants JSON, WeChat wants a JSON error code,
     * Alipay wants the literal string {@code success}).
     */
    default String notificationAck() {
        return "success";
    }

    /** Content type for {@link #notificationAck()}. */
    default String notificationAckContentType() {
        return "text/plain;charset=UTF-8";
    }
}
