package com.ait.app.rest;

import com.ait.common.api.R;
import com.ait.common.security.LoginUserContext;
import com.ait.mall.core.entity.Coupon;
import com.ait.mall.core.service.CouponService;
import com.ait.mall.core.vo.MemberCouponVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/app/coupons")
@RequiredArgsConstructor
@Tag(name = "App · 优惠券")
public class AppCouponController {

    private final CouponService couponService;

    @GetMapping("/mine")
    @Operation(summary = "我的优惠券")
    public R<List<MemberCouponVo>> mine(@RequestParam(required = false) BigDecimal amount) {
        return R.ok(couponService.listForMember(LoginUserContext.requiredUserId(), amount));
    }

    @GetMapping("/available")
    @Operation(summary = "可领取的优惠券")
    public R<List<Coupon>> available() {
        return R.ok(couponService.listTemplates().stream()
                .filter(c -> c.getEnabled() != null && c.getEnabled() == 1)
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/{couponId}/receive")
    @Operation(summary = "领取优惠券")
    public R<MemberCouponVo> receive(@PathVariable Long couponId) {
        return R.ok(couponService.receive(LoginUserContext.requiredUserId(), couponId));
    }
}
