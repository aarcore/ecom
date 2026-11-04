package com.ait.mall.core.service;

import com.ait.mall.core.entity.Coupon;
import com.ait.mall.core.vo.MemberCouponVo;

import java.util.List;

public interface CouponService {

    /**
     * @param basketTotal when provided, each coupon is annotated with usability
     *                    and the discount it would produce
     */
    List<MemberCouponVo> listForMember(Long memberId, java.math.BigDecimal basketTotal);

    /** Issues a coupon to a member; refuses when stock is exhausted. */
    MemberCouponVo receive(Long memberId, Long couponId);

    /** Marks the member coupon as consumed by an order. */
    void markUsed(Long couponId, Long memberId, String orderNo);

    /** Returns the coupon to the wallet when an unpaid order is cancelled. */
    void release(Long couponId, Long memberId);

    /** Reserves the coupon so it cannot be picked by a second checkout. */
    void lockForOrder(Long couponId, Long memberId, String orderNo);

    /** Admin CRUD. */
    List<Coupon> listTemplates();

    Coupon saveTemplate(Coupon coupon);
}
