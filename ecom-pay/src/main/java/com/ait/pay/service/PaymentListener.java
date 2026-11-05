package com.ait.pay.service;

import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;

/**
 * Implemented by the commerce module to react to a verified callback.
 *
 * <p>Splitting it out means {@code ecom-pay} never imports an order entity: the
 * gateway verifies, the mall decides. It also makes the callback testable
 * without an HTTP server.
 */
public interface PaymentListener {

    /**
     * Applies a confirmed payment to whatever it belongs to.
     *
     * <p>Implementations must be idempotent. Channels retry on any non-2xx and
     * sometimes deliver the same event twice; returning normally twice must
     * leave the order paid exactly once.
     *
     * @return a short human readable note for the log, e.g. the order number
     */
    String onPaid(PayNotification notification);

    /**
     * Called for a verified callback that reports a non-successful state.
     * Implementations may ignore this: an unpaid notification is often just
     * noise from a closed payment window.
     */
    default void onUnpaid(PayNotification notification) {
        /* no-op by default */
    }

    /** Builds the callback URL this listener expects for a channel. */
    String notifyUrlFor(String channelCode);

    /** Where the browser lands after a redirect flow completes. */
    String returnUrlFor(String channelCode, PayRequest request);
}
