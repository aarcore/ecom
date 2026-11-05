package com.ait.admin.rest;

import com.ait.common.api.PageResult;
import com.ait.common.api.R;
import com.ait.logging.annotation.OperationLog;
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

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin · 订单管理")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "订单列表")
    public R<PageResult<OrderVo>> page(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String platform,
                                       @RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int limit) {
        return R.ok(orderService.pageForAdmin(keyword, status, platform, startDate, endDate, page, limit));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "订单详情")
    public R<OrderVo> detail(@PathVariable String orderNo) {
        return R.ok(orderService.detailForAdmin(orderNo));
    }

    @PostMapping("/{orderNo}/ship")
    @OperationLog(module = "订单", action = "订单发货")
    @Operation(summary = "订单发货")
    public R<OrderVo> ship(@PathVariable String orderNo, @RequestBody ShipRequest request) {
        return R.ok(orderService.ship(orderNo, request.getExpressCompany(), request.getExpressNo()));
    }

    @PostMapping("/{orderNo}/remark")
    @OperationLog(module = "订单", action = "添加商家备注")
    @Operation(summary = "商家备注")
    public R<OrderVo> remark(@PathVariable String orderNo, @RequestBody RemarkRequest request) {
        return R.ok(orderService.adminRemark(orderNo, request.getRemark()));
    }

    @PostMapping("/{orderNo}/refund")
    @OperationLog(module = "订单", action = "订单退款")
    @Operation(summary = "订单退款")
    public R<OrderVo> refund(@PathVariable String orderNo, @RequestBody RemarkRequest request) {
        return R.ok(orderService.refund(orderNo, request.getRemark()));
    }

    @PostMapping("/{orderNo}/cancel")
    @OperationLog(module = "订单", action = "关闭订单")
    @Operation(summary = "关闭未支付订单")
    public R<OrderVo> cancel(@PathVariable String orderNo, @RequestBody(required = false) RemarkRequest request) {
        String reason = request == null || request.getRemark() == null
                ? "运营手动关闭" : request.getRemark();
        return R.ok(orderService.cancel(null, orderNo, reason));
    }

    @lombok.Data
    public static class ShipRequest {
        private String expressCompany;
        private String expressNo;
    }

    @lombok.Data
    public static class RemarkRequest {
        private String remark;
    }
}
