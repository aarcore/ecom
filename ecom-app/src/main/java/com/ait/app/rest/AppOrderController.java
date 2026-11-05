package com.ait.app.rest;

import com.ait.common.api.PageResult;
import com.ait.common.api.R;
import com.ait.common.security.LoginUserContext;
import com.ait.common.utils.RequestUtils;
import com.ait.mall.core.dto.CheckoutPreview;
import com.ait.mall.core.dto.OrderCreateRequest;
import com.ait.mall.core.service.OrderService;
import com.ait.mall.core.vo.OrderVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/app/orders")
@RequiredArgsConstructor
@Tag(name = "App · 订单")
public class AppOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "我的订单列表")
    public R<PageResult<OrderVo>> page(@RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int limit) {
        return R.ok(orderService.pageForMember(LoginUserContext.requiredUserId(), status, page, limit));
    }

    @GetMapping("/counts")
    @Operation(summary = "各状态订单数量")
    public R<Map<String, Long>> counts() {
        return R.ok(orderService.statusCounts(LoginUserContext.requiredUserId()));
    }

    @PostMapping("/preview")
    @Operation(summary = "结算预览（金额、优惠、地址）")
    public R<CheckoutPreview> preview(@RequestBody OrderCreateRequest request) {
        return R.ok(orderService.preview(LoginUserContext.requiredUserId(), request.getItems(),
                request.getCouponId(), request.isCouponOptOut(), request.getAddressId(), request.getSource()));
    }

    @PostMapping
    @Operation(summary = "提交订单")
    public R<OrderVo> create(@Valid @RequestBody OrderCreateRequest request, HttpServletRequest http) {
        return R.ok(orderService.create(LoginUserContext.requiredUserId(),
                RequestUtils.resolvePlatform(http), request));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "订单详情")
    public R<OrderVo> detail(@PathVariable String orderNo) {
        return R.ok(orderService.detailForMember(LoginUserContext.requiredUserId(), orderNo));
    }

    @PostMapping("/{orderNo}/pay")
    @Operation(summary = "支付订单")
    public R<Map<String, Object>> pay(@PathVariable String orderNo,
                                      @RequestBody(required = false) PayRequest request) {
        String payType = request == null || request.getPayType() == null ? "mock" : request.getPayType();
        OrderVo order = orderService.pay(LoginUserContext.requiredUserId(), orderNo, payType);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("order", order);
        payload.put("paid", true);
        // Real channels would return a prepay payload here; the demo confirms
        // synchronously so the storefront can move straight to the success page.
        payload.put("payType", payType);
        payload.put("message", "支付成功");
        return R.ok(payload);
    }

    @PostMapping("/{orderNo}/cancel")
    @Operation(summary = "取消订单")
    public R<OrderVo> cancel(@PathVariable String orderNo,
                             @RequestBody(required = false) CancelRequest request) {
        String reason = request == null || request.getReason() == null ? "用户主动取消" : request.getReason();
        return R.ok(orderService.cancel(LoginUserContext.requiredUserId(), orderNo, reason));
    }

    @PostMapping("/{orderNo}/confirm")
    @Operation(summary = "确认收货")
    public R<OrderVo> confirm(@PathVariable String orderNo) {
        return R.ok(orderService.confirmReceipt(LoginUserContext.requiredUserId(), orderNo));
    }

    @lombok.Data
    public static class PayRequest {
        private String payType;
    }

    @lombok.Data
    public static class CancelRequest {
        private String reason;
    }
}
