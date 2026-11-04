package com.ait.mall.core.service;

import java.util.List;
import java.util.Map;

/**
 * Aggregated figures for the admin dashboard.
 */
public interface DashboardService {

    /**
     * @return headline counters, 7 day deltas and trend series in one payload so
     *         the console loads the dashboard with a single request
     */
    Map<String, Object> overview();

    List<Map<String, Object>> salesTrend(int days);

    List<Map<String, Object>> topProducts(int limit);
}
