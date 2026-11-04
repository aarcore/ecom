package com.ait.mall.core.service.support;

import com.ait.mall.core.entity.Coupon;
import com.ait.mall.core.entity.MemberCoupon;
import com.ait.mall.core.mapper.CouponMapper;
import com.ait.mall.core.mapper.MemberCouponMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure pricing rules for coupons, kept separate from persistence so the same
 * maths is used by the preview, the order creation and the wallet screen.
 *
 * <ul>
 *   <li>{@code amount}   — fixed deduction, capped at the basket total</li>
 *   <li>{@code discount} — rate out of 10 (8.5 means pay 85%)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CouponCalculator {

    private final CouponMapper couponMapper;
    private final MemberCouponMapper memberCouponMapper;

    /** Discount this coupon yields for the given total, or ZERO when unusable. */
    public BigDecimal compute(Coupon template, BigDecimal total) {
        if (template == null || total == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (template.getMinAmount() != null && total.compareTo(template.getMinAmount()) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal result;
        if ("discount".equalsIgnoreCase(template.getType())) {
            BigDecimal rate = template.getDiscount() == null ? BigDecimal.TEN : template.getDiscount();
            BigDecimal payable = total.multiply(rate).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            result = total.subtract(payable);
        } else {
            result = template.getAmount() == null ? BigDecimal.ZERO : template.getAmount();
        }
        if (result.signum() < 0) {
            result = BigDecimal.ZERO;
        }
        if (result.compareTo(total) > 0) {
            result = total;
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Validates that the member owns the coupon and it is still usable, then
     * returns the discount. Unknown or invalid coupon ids yield ZERO rather than
     * an error so the preview screen stays usable while the customer edits.
     */
    public BigDecimal discountFor(Long memberId, Long couponId, BigDecimal total) {
        if (memberId == null || couponId == null || total == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        MemberCoupon holding = memberCouponMapper.selectOne(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(MemberCoupon::getCouponId, couponId)
                .eq(MemberCoupon::getStatus, 0)
                .orderByDesc(MemberCoupon::getId)
                .last("limit 1"));
        if (holding == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (holding.getEndAt() != null && holding.getEndAt().isBefore(LocalDateTime.now())) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return compute(couponMapper.selectById(couponId), total);
    }

    /** Picks the coupon that saves the customer the most for this basket. */
    public Long bestCoupon(Long memberId, BigDecimal total) {
        if (memberId == null || total == null) {
            return null;
        }
        List<MemberCoupon> holdings = memberCouponMapper.selectList(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(MemberCoupon::getStatus, 0));
        if (holdings.isEmpty()) {
            return null;
        }
        List<Long> ids = holdings.stream().map(MemberCoupon::getCouponId).distinct().collect(Collectors.toList());
        Map<Long, Coupon> templates = couponMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));

        Long bestId = null;
        BigDecimal bestValue = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        for (MemberCoupon holding : holdings) {
            if (holding.getEndAt() != null && holding.getEndAt().isBefore(now)) {
                continue;
            }
            BigDecimal value = compute(templates.get(holding.getCouponId()), total);
            if (value.compareTo(bestValue) > 0) {
                bestValue = value;
                bestId = holding.getCouponId();
            }
        }
        return bestId;
    }
}
