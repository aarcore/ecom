package com.ait.admin.rest;

import com.ait.common.api.R;
import com.ait.mall.core.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin · 数据看板")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "看板总览（KPI、趋势、品类分布）")
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.overview());
    }

    @GetMapping("/sales-trend")
    @Operation(summary = "销售趋势")
    public R<List<Map<String, Object>>> salesTrend(@RequestParam(defaultValue = "14") int days) {
        return R.ok(dashboardService.salesTrend(days));
    }

    @GetMapping("/top-products")
    @Operation(summary = "热销商品")
    public R<List<Map<String, Object>>> topProducts(@RequestParam(defaultValue = "6") int limit) {
        return R.ok(dashboardService.topProducts(limit));
    }
}
