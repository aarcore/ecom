package com.ait.pay.service.impl;

import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.mapper.PayRecordWriteMapper;
import com.ait.pay.service.PayRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Payment attempt log.
 *
 * <p>The idempotency story is worth stating plainly, because it is the part of
 * a payment integration that actually causes incidents:
 *
 * <ul>
 *   <li>{@code settle} is a conditional update guarded on {@code status = 0}, so
 *       of two concurrent callbacks exactly one gets a non-zero row count.</li>
 *   <li>The unique index on {@code (channel_code, trade_no)} is the backstop: if
 *       two attempts race all the way to insert, one loses at the database.</li>
 *   <li>{@code markPaid} reports whether <em>this</em> call did the transition,
 *       which is what stops us from granting points twice.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayRecordServiceImpl implements PayRecordService {

    private final PayRecordWriteMapper mapper;

    @Override
    public Long recordAttempt(String channelCode, PayRequest request, String tradeNo, Long memberId) {
        try {
            mapper.insertPending(
                    request.orderNo(),
                    memberId,
                    channelCode,
                    request.amount(),
                    tradeNo,
                    channelCode,
                    request.currency() == null ? "CNY" : request.currency());
        } catch (DuplicateKeyException ex) {
            // the same channel transaction already has a row: a retried create
            // rather than a new payment, which is fine to ignore
            log.info("payment attempt already recorded channel={} tradeNo={}", channelCode, tradeNo);
        }
        return null;
    }

    @Override
    public void recordSettled(String channelCode, PayRequest request, String tradeNo, Long memberId) {
        try {
            mapper.insertSettled(request.orderNo(), memberId, channelCode, request.amount(),
                    tradeNo, channelCode,
                    request.currency() == null ? "CNY" : request.currency());
        } catch (DuplicateKeyException ex) {
            log.info("settled payment already recorded channel={} tradeNo={}", channelCode, tradeNo);
        }
    }

    @Override
    public boolean markPaid(PayNotification notification) {
        int updated = mapper.settle(notification.channelCode(), notification.tradeNo(),
                notification.rawStatus());
        if (updated == 0) {
            log.info("payment callback was already settled channel={} tradeNo={}",
                    notification.channelCode(), notification.tradeNo());
            return false;
        }
        return true;
    }

    @Override
    public void markFailed(String orderNo, String reason) {
        int updated = mapper.failPending(orderNo, truncate(reason));
        if (updated > 0) {
            log.info("payment attempt marked failed order={} reason={}", orderNo, reason);
        }
    }

    @Override
    public boolean alreadySettled(String channelCode, String tradeNo) {
        return mapper.countSettled(channelCode, tradeNo) > 0;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
