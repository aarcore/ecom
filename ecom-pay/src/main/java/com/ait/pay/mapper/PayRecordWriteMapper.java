package com.ait.pay.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Payment log writes.
 *
 * <p>Declared here rather than reusing {@code com.ait.mall.core.mapper.PayRecordMapper}
 * so the payment module keeps owning the table it extends — the mall mapper stays
 * as it was for the admin console's read-only views.
 */
@Mapper
public interface PayRecordWriteMapper {

    @Insert("""
            insert into shop_pay_record
                (order_no, member_id, pay_type, amount, trade_no, status,
                 channel_code, out_trade_no, currency, created_at)
            values
                (#{orderNo}, #{memberId}, #{payType}, #{amount}, #{tradeNo}, 0,
                 #{channelCode}, #{orderNo}, #{currency}, now())
            """)
    int insertPending(@Param("orderNo") String orderNo,
                      @Param("memberId") Long memberId,
                      @Param("payType") String payType,
                      @Param("amount") java.math.BigDecimal amount,
                      @Param("tradeNo") String tradeNo,
                      @Param("channelCode") String channelCode,
                      @Param("currency") String currency);

    @Insert("""
            insert into shop_pay_record
                (order_no, member_id, pay_type, amount, trade_no, status,
                 channel_code, out_trade_no, currency, paid_at, notify_at, created_at)
            values
                (#{orderNo}, #{memberId}, #{payType}, #{amount}, #{tradeNo}, 1,
                 #{channelCode}, #{orderNo}, #{currency}, now(), now(), now())
            """)
    int insertSettled(@Param("orderNo") String orderNo,
                      @Param("memberId") Long memberId,
                      @Param("payType") String payType,
                      @Param("amount") java.math.BigDecimal amount,
                      @Param("tradeNo") String tradeNo,
                      @Param("channelCode") String channelCode,
                      @Param("currency") String currency);

    /**
     * Settles a pending row. The {@code status = 0} guard is what makes a
     * replayed callback a no-op instead of a second settlement.
     */
    @Update("""
            update shop_pay_record
               set status = 1, paid_at = now(), notify_at = now(), raw_status = #{rawStatus}
             where channel_code = #{channelCode}
               and trade_no = #{tradeNo}
               and status = 0
            """)
    int settle(@Param("channelCode") String channelCode,
               @Param("tradeNo") String tradeNo,
               @Param("rawStatus") String rawStatus);

    @Select("""
            select count(1) from shop_pay_record
             where channel_code = #{channelCode} and trade_no = #{tradeNo} and status = 1
            """)
    int countSettled(@Param("channelCode") String channelCode,
                     @Param("tradeNo") String tradeNo);
}
