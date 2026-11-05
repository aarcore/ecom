package com.ait.app.rest;

import com.ait.common.api.R;
import com.ait.common.constant.CommonConstants;
import com.ait.common.security.LoginUserContext;
import com.ait.common.utils.RequestUtils;
import com.ait.mall.core.dto.AppLoginRequest;
import com.ait.mall.core.dto.LoginResult;
import com.ait.mall.core.dto.MpLoginRequest;
import com.ait.mall.core.service.AddressService;
import com.ait.mall.core.service.CartService;
import com.ait.mall.core.service.CouponService;
import com.ait.mall.core.service.MemberService;
import com.ait.mall.core.service.OrderService;
import com.ait.mall.core.vo.AddressVo;
import com.ait.mall.core.vo.MemberCouponVo;
import com.ait.mall.core.vo.MemberVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer account, sessions and the "my" tab aggregate.
 */
@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
@Tag(name = "App · 登录与个人中心")
public class AppAuthController {

    private final MemberService memberService;
    private final OrderService orderService;
    private final CartService cartService;
    private final CouponService couponService;
    private final AddressService addressService;

    @PostMapping("/login")
    @Operation(summary = "手机号登录（密码或验证码）")
    public R<LoginResult> login(@Valid @RequestBody AppLoginRequest request, HttpServletRequest http) {
        request.setPlatform(RequestUtils.resolvePlatform(http));
        return R.ok(memberService.loginByMobile(request));
    }

    @PostMapping("/login/mp")
    @Operation(summary = "微信小程序登录")
    public R<LoginResult> loginByWechat(@Valid @RequestBody MpLoginRequest request) {
        return R.ok(memberService.loginByWechat(request));
    }

    @PostMapping("/sms")
    @Operation(summary = "发送短信验证码")
    public R<Map<String, Object>> sendSms(@RequestParam String mobile) {
        String code = memberService.sendSmsCode(mobile);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mobile", mobile);
        payload.put("ttl", 300);
        // The demo build has no SMS provider wired up, so the code is echoed back
        // for the client to display. Remove this field in production.
        payload.put("demoCode", code);
        return R.ok(payload);
    }

    @GetMapping("/profile")
    @Operation(summary = "当前登录会员")
    public R<MemberVo> profile() {
        return R.ok(memberService.profile(LoginUserContext.requiredUserId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新昵称 / 头像 / 性别")
    public R<MemberVo> updateProfile(@RequestBody MemberVo request) {
        return R.ok(memberService.updateProfile(LoginUserContext.requiredUserId(), request));
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@RequestBody PasswordRequest request) {
        memberService.changePassword(LoginUserContext.requiredUserId(),
                request.getOldPassword(), request.getNewPassword());
        return R.ok();
    }

    /**
     * Everything the "my" tab needs in a single round trip: profile, order
     * badges, cart count and coupon count.
     */
    @GetMapping("/mine")
    @Operation(summary = "个人中心聚合数据")
    public R<Map<String, Object>> mine() {
        Long memberId = LoginUserContext.requiredUserId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("profile", memberService.profile(memberId));
        payload.put("orderCounts", orderService.statusCounts(memberId));
        payload.put("cartCount", cartService.count(memberId));
        List<MemberCouponVo> coupons = couponService.listForMember(memberId, null);
        payload.put("couponCount", coupons.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 0).count());
        List<AddressVo> addresses = addressService.list(memberId);
        payload.put("addressCount", addresses.size());
        return R.ok(payload);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout() {
        LoginUserContext.clear();
        return R.ok();
    }

    @lombok.Data
    public static class PasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
