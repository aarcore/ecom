package com.ait.mall.core.service.impl;

import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import com.ait.mall.core.entity.Coupon;
import com.ait.mall.core.entity.MemberCoupon;
import com.ait.mall.core.mapper.CouponMapper;
import com.ait.mall.core.mapper.MemberCouponMapper;
import com.ait.mall.core.service.CouponService;
import com.ait.mall.core.service.I18nService;
import com.ait.common.i18n.LocaleContext;
import com.ait.mall.core.service.support.CouponCalculator;
import com.ait.mall.core.vo.MemberCouponVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final CouponCalculator couponCalculator;
    private final I18nService i18nService;

    @Override
    public List<MemberCouponVo> listForMember(Long memberId, BigDecimal basketTotal) {
        List<MemberCoupon> holdings = memberCouponMapper.selectList(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId)
                .orderByAsc(MemberCoupon::getStatus)
                .orderByAsc(MemberCoupon::getEndAt));
        if (holdings.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = holdings.stream().map(MemberCoupon::getCouponId).distinct().collect(Collectors.toList());
        Map<Long, Coupon> templates = couponMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));

        List<MemberCouponVo> result = new ArrayList<>(holdings.size());
        LocalDateTime now = LocalDateTime.now();
        for (MemberCoupon holding : holdings) {
            Coupon template = templates.get(holding.getCouponId());
            if (template == null) {
                continue;
            }
            MemberCouponVo vo = new MemberCouponVo();
            vo.setId(holding.getId());
            vo.setCouponId(template.getId());
            vo.setName(template.getName());
            vo.setType(template.getType());
            vo.setAmount(template.getAmount());
            vo.setDiscount(template.getDiscount());
            vo.setMinAmount(template.getMinAmount());
            vo.setStartAt(holding.getStartAt());
            vo.setEndAt(holding.getEndAt());

            boolean expired = holding.getEndAt() != null && holding.getEndAt().isBefore(now);
            int status = holding.getStatus() == null ? 0 : holding.getStatus();
            if (status == 0 && expired) {
                status = 2;
            }
            vo.setStatus(status);
            vo.setStatusText(status == 0 ? "可使用" : (status == 1 ? "已使用" : "已过期"));

            if (basketTotal != null) {
                boolean usable = status == 0
                        && template.getMinAmount() != null
                        && basketTotal.compareTo(template.getMinAmount()) >= 0;
                vo.setUsable(usable);
                vo.setDiscountValue(usable ? couponCalculator.compute(template, basketTotal) : BigDecimal.ZERO);
            }
            result.add(vo);
        }
        localizeCoupons(result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberCouponVo receive(Long memberId, Long couponId) {
        Coupon template = couponMapper.selectById(couponId);
        if (template == null || !Integer.valueOf(1).equals(template.getEnabled())) {
            throw new BizException(ResultCode.COUPON_NOT_AVAILABLE);
        }
        Long held = memberCouponMapper.selectCount(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(MemberCoupon::getCouponId, couponId));
        if (held != null && held > 0) {
            throw new BizException(ResultCode.COUPON_NOT_AVAILABLE, "您已经领取过该优惠券");
        }
        if (template.getTotal() != null && template.getTaken() != null
                && template.getTaken() >= template.getTotal()) {
            throw new BizException(ResultCode.COUPON_NOT_AVAILABLE, "优惠券已被领完");
        }

        LocalDateTime now = LocalDateTime.now();
        MemberCoupon holding = new MemberCoupon();
        holding.setCouponId(couponId);
        holding.setMemberId(memberId);
        holding.setStatus(0);
        holding.setStartAt(now);
        holding.setEndAt(now.plusDays(template.getValidDays() == null ? 7 : template.getValidDays()));
        holding.setCreatedAt(now);
        holding.setUpdatedAt(now);
        memberCouponMapper.insert(holding);

        Coupon patch = new Coupon();
        patch.setId(couponId);
        patch.setTaken((template.getTaken() == null ? 0 : template.getTaken()) + 1);
        couponMapper.updateById(patch);

        MemberCouponVo vo = new MemberCouponVo();
        vo.setId(holding.getId());
        vo.setCouponId(couponId);
        vo.setName(template.getName());
        vo.setType(template.getType());
        vo.setAmount(template.getAmount());
        vo.setDiscount(template.getDiscount());
        vo.setMinAmount(template.getMinAmount());
        vo.setStatus(0);
        vo.setStatusText("可使用");
        vo.setStartAt(holding.getStartAt());
        vo.setEndAt(holding.getEndAt());
        return vo;
    }

    /**
     * Coupon names are template data, so they follow the same overlay as
     * products.
     *
     * <p>{@code statusText} is deliberately left in Chinese: it is derived from
     * the numeric status at read time, and every client already has that code
     * plus its own label. Translating derived strings on the server would mean
     * shipping a second message catalogue for no benefit.
     */
    private void localizeCoupons(List<MemberCouponVo> coupons) {
        if (coupons == null || coupons.isEmpty() || LocaleContext.get().isSource()) {
            return;
        }
        var locale = LocaleContext.get();
        List<Long> ids = coupons.stream().map(MemberCouponVo::getCouponId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        var overrides = i18nService.textMap("coupon", ids, locale);
        for (MemberCouponVo coupon : coupons) {
            String name = overrides.getOrDefault(coupon.getCouponId(),
                    java.util.Collections.emptyMap()).get("name");
            if (name != null && !name.isEmpty()) {
                coupon.setName(name);
            }
        }
    }

    @Override
    public void markUsed(Long couponId, Long memberId, String orderNo) {
        MemberCoupon holding = findHolding(memberId, couponId);
        if (holding == null) {
            return;
        }
        holding.setStatus(1);
        holding.setOrderNo(orderNo);
        holding.setUsedAt(LocalDateTime.now());
        holding.setUpdatedAt(LocalDateTime.now());
        memberCouponMapper.updateById(holding);
    }

    @Override
    public void release(Long couponId, Long memberId) {
        MemberCoupon holding = findHolding(memberId, couponId);
        if (holding == null || Integer.valueOf(1).equals(holding.getStatus())) {
            return;
        }
        holding.setStatus(0);
        holding.setOrderNo(null);
        holding.setUpdatedAt(LocalDateTime.now());
        memberCouponMapper.updateById(holding);
    }

    @Override
    public void lockForOrder(Long couponId, Long memberId, String orderNo) {
        MemberCoupon holding = findHolding(memberId, couponId);
        if (holding == null) {
            throw new BizException(ResultCode.COUPON_USED);
        }
        holding.setOrderNo(orderNo);
        holding.setUpdatedAt(LocalDateTime.now());
        memberCouponMapper.updateById(holding);
    }

    @Override
    public List<Coupon> listTemplates() {
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery().orderByDesc(Coupon::getId));
    }

    @Override
    public Coupon saveTemplate(Coupon coupon) {
        if (coupon.getId() == null) {
            if (coupon.getTaken() == null) {
                coupon.setTaken(0);
            }
            couponMapper.insert(coupon);
        } else {
            couponMapper.updateById(coupon);
        }
        return coupon;
    }

    private MemberCoupon findHolding(Long memberId, Long couponId) {
        if (couponId == null) {
            return null;
        }
        return memberCouponMapper.selectOne(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(MemberCoupon::getCouponId, couponId)
                .orderByDesc(MemberCoupon::getId)
                .last("limit 1"));
    }
}
