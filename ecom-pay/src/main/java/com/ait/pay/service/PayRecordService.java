package com.ait.pay.service;

import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;

/**
 * Durable record of a payment attempt.
 *
 * <p>Lives in the payment module rather than the mall because it is written on
 * both sides of the flow: the storefront opens a payment, the callback closes
 * it, and the two can arrive on different threads minutes apart. Keeping the
 * write here means the reconciliation query has one owner.
 */
public interface PayRecordService {

    /**
     * Records the moment a payment is opened, while it is still pending.
     * Returns the local record id.
     */
    Long recordAttempt(String channelCode, PayRequest request, String tradeNo, Long memberId);

    /**
     * Records an attempt that the channel already settled inline (the store
     * balance and the demo channel). Writing it as pending and then settling it
     * in a second statement would leave a window where a reconciliation job sees
     * a paid order with an unpaid payment row.
     */
    void recordSettled(String channelCode, PayRequest request, String tradeNo, Long memberId);

    /**
     * Marks the attempt as settled. Idempotent: a replayed callback must not
     * create a second paid row.
     *
     * @return true when this call is the one that transitioned the record
     */
    boolean markPaid(PayNotification notification);

    /**
     * True when this channel transaction has already been settled. The unique
     * index on (channel_code, trade_no) is the real guard; this is the cheap
     * pre-check that avoids relying on a constraint violation for flow control.
     */
    boolean alreadySettled(String channelCode, String tradeNo);
}
