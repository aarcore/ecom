package com.ait.mall.core.service;

import com.ait.common.api.PageResult;
import com.ait.mall.core.dto.CheckoutPreview;
import com.ait.mall.core.dto.OrderCreateRequest;
import com.ait.mall.core.dto.PayLaunch;
import com.ait.mall.core.vo.OrderVo;

import java.util.List;

/**
 * Order read/write operations shared by the admin console and the storefront.
 * Write methods take an explicit member id so admin-triggered actions can reuse
 * them.
 */
public interface OrderService {

    /**
     * Builds the checkout screen payload without persisting anything.
     *
     * @param couponId     explicit coupon selection; {@code null} lets the server
     *                     pick the best one
     * @param couponOptOut when true, never auto-select a coupon
     */
    CheckoutPreview preview(Long memberId, List<com.ait.mall.common.dto.OrderItemRequest> items,
                            Long couponId, boolean couponOptOut, Long addressId, String source);

    /** Creates the order and locks stock. Returns the new order. */
    OrderVo create(Long memberId, String platform, OrderCreateRequest request);

    /**
     * Marks an order paid and post-processes member stats and the coupon.
     *
     * <p>Only for channels that settle without a redirect — the store balance
     * and the demo channel. Real providers go through {@link #launchPayment}
     * and {@link #settleFromChannel}.
     */
    OrderVo pay(Long memberId, String orderNo, String payType);

    /**
     * Opens a payment on a real channel and returns what the client must do
     * next (redirect, QR, SDK payload) without marking the order paid.
     *
     * <p>The order stays in {@code PENDING_PAYMENT} until the channel calls back:
     * trusting the browser's return URL would let a customer pay a one-yuan
     * order with a forged {@code ?result=success}.
     */
    PayLaunch launchPayment(Long memberId, String orderNo, String channelCode, String clientIp);

    /**
     * Applies a verified channel callback. Idempotent.
     *
     * @return the order number that was settled, or null when the callback did
     *         not correspond to a payable order
     */
    String settleFromChannel(com.ait.pay.core.PayNotification notification);

    /**
     * True when this channel transaction was already applied. Used to answer a
     * replayed callback without touching the order twice.
     */
    boolean isChannelTradeSettled(String channelCode, String tradeNo);

    OrderVo cancel(Long memberId, String orderNo, String reason);

    OrderVo confirmReceipt(Long memberId, String orderNo);

    /** Member facing list, filtered by status; null means all. */
    PageResult<OrderVo> pageForMember(Long memberId, Integer status, int page, int limit);

    OrderVo detailForMember(Long memberId, String orderNo);

    /** Admin list with free-form filters. */
    PageResult<OrderVo> pageForAdmin(String keyword, Integer status, String platform,
                                     String startDate, String endDate, int page, int limit);

    OrderVo detailForAdmin(String orderNo);

    OrderVo ship(String orderNo, String expressCompany, String expressNo);

    OrderVo adminRemark(String orderNo, String remark);

    OrderVo refund(String orderNo, String reason);

    /** Closes unpaid orders past their deadline; invoked by the scheduler. */
    int closeExpiredOrders();

    /** Auto-confirms receipt for orders shipped longer than the grace period. */
    int autoCompleteShipped();

    /** Counters for the member centre badges. */
    java.util.Map<String, Long> statusCounts(Long memberId);
}
