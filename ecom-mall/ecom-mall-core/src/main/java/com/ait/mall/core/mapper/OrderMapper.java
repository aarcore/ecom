package com.ait.mall.core.mapper;

import com.ait.common.mapper.CoreMapper;
import com.ait.mall.core.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends CoreMapper<Order> {

    /** GMV over the paid states, optionally constrained to a window. */
    @Select("select coalesce(sum(pay_amount), 0) from shop_order "
            + "where deleted = 0 and status in (1, 2, 3) "
            + "and created_at >= now() - (#{days} || ' day')::interval")
    BigDecimal revenueWithinDays(@Param("days") int days);

    /**
     * GMV in the window {@code [fromDaysAgo, toDaysAgo)} counted back from now.
     * Used to compare a period against the immediately preceding one of equal
     * length, so growth percentages are honest.
     */
    @Select("select coalesce(sum(pay_amount), 0) from shop_order "
            + "where deleted = 0 and status in (1, 2, 3) "
            + "and created_at >= now() - (#{fromDaysAgo} || ' day')::interval "
            + "and created_at <  now() - (#{toDaysAgo} || ' day')::interval")
    BigDecimal revenueBetweenDays(@Param("fromDaysAgo") int fromDaysAgo,
                                 @Param("toDaysAgo") int toDaysAgo);

    @Select("select count(*) from shop_order "
            + "where deleted = 0 "
            + "and created_at >= now() - (#{fromDaysAgo} || ' day')::interval "
            + "and created_at <  now() - (#{toDaysAgo} || ' day')::interval")
    long countBetweenDays(@Param("fromDaysAgo") int fromDaysAgo,
                          @Param("toDaysAgo") int toDaysAgo);

    @Select("select count(*) from shop_order "
            + "where deleted = 0 and created_at >= now() - (#{days} || ' day')::interval")
    long countWithinDays(@Param("days") int days);

    @Select("select count(*) from shop_order where deleted = 0 and status = #{status}")
    long countByStatus(@Param("status") int status);

    /**
     * Daily order count and GMV for the dashboard trend chart.
     * Days with no orders are filled in Java so the chart always has N points.
     */
    @Select("select to_char(d.day, 'MM-DD') as day, "
            + "       coalesce(count(o.id), 0) as orders, "
            + "       coalesce(sum(o.pay_amount) filter (where o.status in (1,2,3)), 0) as gmv "
            + "from generate_series(current_date - (#{days} - 1) * interval '1 day', current_date, interval '1 day') as d(day) "
            + "left join shop_order o on o.created_at::date = d.day::date and o.deleted = 0 "
            + "group by d.day order by d.day")
    List<Map<String, Object>> dailyTrend(@Param("days") int days);

    /** Top buyers, used on the dashboard member card. */
    @Select("select member_nickname as nickname, count(*) as orders, coalesce(sum(pay_amount), 0) as amount "
            + "from shop_order where deleted = 0 and status in (1,2,3) "
            + "group by member_nickname order by amount desc limit #{limit}")
    List<Map<String, Object>> topBuyers(@Param("limit") int limit);

    /** Auto-closes orders left unpaid past the deadline. */
    @Select("select * from shop_order where deleted = 0 and status = 0 and created_at < now() - interval '45 minute'")
    List<Order> findExpiredUnpaid();

    @Select("select * from shop_order where deleted = 0 and status = 2 and shipped_at < now() - interval '7 day'")
    List<Order> findAutoCompletable();
}
