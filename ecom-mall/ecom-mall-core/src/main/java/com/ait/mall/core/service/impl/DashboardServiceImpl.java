package com.ait.mall.core.service.impl;

import com.ait.mall.common.enums.OrderStatus;
import com.ait.mall.core.mapper.MemberMapper;
import com.ait.mall.core.mapper.OrderMapper;
import com.ait.mall.core.mapper.ProductMapper;
import com.ait.mall.core.service.DashboardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final ProductMapper productMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal gmvToday = orderMapper.revenueWithinDays(1);
        BigDecimal gmv7 = orderMapper.revenueWithinDays(7);
        BigDecimal gmv30 = orderMapper.revenueWithinDays(30);
        // previous 7 day window: days 7..14 ago, so the comparison is like-for-like
        BigDecimal gmvPrev7 = orderMapper.revenueBetweenDays(14, 7);

        long ordersToday = orderMapper.countWithinDays(1);
        long orders7 = orderMapper.countWithinDays(7);
        long orders30 = orderMapper.countWithinDays(30);
        long ordersPrev7 = orderMapper.countBetweenDays(14, 7);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("gmvToday", gmvToday);
        kpi.put("gmv7", gmv7);
        kpi.put("gmv30", gmv30);
        kpi.put("gmvGrowth", growth(gmv7, gmvPrev7));
        kpi.put("ordersToday", ordersToday);
        kpi.put("orders7", orders7);
        kpi.put("orders30", orders30);
        kpi.put("ordersGrowth", growth(BigDecimal.valueOf(orders7),
                BigDecimal.valueOf(ordersPrev7)));
        kpi.put("avgOrderValue", orders30 == 0 ? BigDecimal.ZERO
                : gmv30.divide(BigDecimal.valueOf(orders30), 2, java.math.RoundingMode.HALF_UP));

        kpi.put("pendingShipment", orderMapper.countByStatus(OrderStatus.PENDING_SHIPMENT.getCode()));
        kpi.put("pendingPayment", orderMapper.countByStatus(OrderStatus.PENDING_PAYMENT.getCode()));
        kpi.put("shipped", orderMapper.countByStatus(OrderStatus.SHIPPED.getCode()));
        kpi.put("refunding", orderMapper.countByStatus(OrderStatus.REFUNDING.getCode()));

        kpi.put("memberTotal", memberMapper.selectCount(Wrappers.lambdaQuery()));
        kpi.put("memberNew7", memberMapper.countNewWithinDays(7));
        kpi.put("productTotal", productMapper.selectCount(Wrappers.lambdaQuery()));
        result.put("kpi", kpi);

        result.put("trend", salesTrend(14));
        result.put("memberTrend", memberMapper.dailyRegistrations(14));
        result.put("categoryShare", productMapper.salesByCategory());
        result.put("memberLevels", memberMapper.levelDistribution());
        result.put("topProducts", productMapper.topSelling(6));
        result.put("topBuyers", orderMapper.topBuyers(5));

        Map<String, Object> orderStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            orderStatus.put(status.name(), orderMapper.countByStatus(status.getCode()));
        }
        result.put("orderStatus", orderStatus);
        return result;
    }

    @Override
    public List<Map<String, Object>> salesTrend(int days) {
        return new ArrayList<>(orderMapper.dailyTrend(Math.max(1, Math.min(days, 90))));
    }

    @Override
    public List<Map<String, Object>> topProducts(int limit) {
        return productMapper.topSelling(Math.max(1, Math.min(limit, 20)));
    }

    /** Percentage change, guarding against a zero denominator. */
    private BigDecimal growth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() <= 0) {
            return current == null ? BigDecimal.ZERO : new BigDecimal("100.00");
        }
        BigDecimal base = current == null ? BigDecimal.ZERO : current;
        return base.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, java.math.RoundingMode.HALF_UP);
    }
}
