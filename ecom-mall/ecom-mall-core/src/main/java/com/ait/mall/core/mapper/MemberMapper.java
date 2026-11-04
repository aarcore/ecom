package com.ait.mall.core.mapper;

import com.ait.common.mapper.CoreMapper;
import com.ait.mall.core.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface MemberMapper extends CoreMapper<Member> {

    @Select("select count(*) from shop_member "
            + "where deleted = 0 and created_at >= now() - (#{days} || ' day')::interval")
    long countNewWithinDays(@Param("days") int days);

    @Select("select to_char(d.day, 'MM-DD') as day, count(m.id) as members "
            + "from generate_series(current_date - (#{days} - 1) * interval '1 day', current_date, interval '1 day') as d(day) "
            + "left join shop_member m on m.created_at::date = d.day::date and m.deleted = 0 "
            + "group by d.day order by d.day")
    List<Map<String, Object>> dailyRegistrations(@Param("days") int days);

    /** Accumulates lifetime spend after a successful payment. */
    @Update("update shop_member set total_spent = total_spent + #{amount}, order_count = order_count + 1, "
            + " points = points + floor(#{amount})::int, "
            + " level = case when total_spent + #{amount} >= 10000 then 4 "
            + "              when total_spent + #{amount} >= 5000 then 3 "
            + "              when total_spent + #{amount} >= 1000 then 2 else 1 end, "
            + " updated_at = now() "
            + "where id = #{memberId}")
    int accumulateSpend(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    @Update("update shop_member set balance = balance - #{amount} where id = #{memberId} and balance >= #{amount}")
    int deductBalance(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    @Update("update shop_member set balance = balance + #{amount} where id = #{memberId}")
    int addBalance(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    /** Member level distribution for the dashboard. */
    @Select("select concat('L', level) as name, count(*) as value from shop_member "
            + "where deleted = 0 group by level order by level")
    List<Map<String, Object>> levelDistribution();
}
