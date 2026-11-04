package com.ait.admin.rest;

import com.ait.common.api.R;
import com.ait.logging.annotation.OperationLog;
import com.ait.mall.core.entity.Coupon;
import com.ait.mall.core.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Admin · 优惠券")
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    @Operation(summary = "优惠券模板列表")
    public R<List<Coupon>> list() {
        return R.ok(couponService.listTemplates());
    }

    @PostMapping
    @OperationLog(module = "优惠券", action = "保存优惠券")
    @Operation(summary = "新增 / 编辑优惠券")
    public R<Coupon> save(@RequestBody Coupon coupon) {
        return R.ok(couponService.saveTemplate(coupon));
    }
}
