package com.ait.mall.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment attempt log, one row per pay call.
 */
@Data
@TableName("shop_pay_record")
public class PayRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long memberId;

    private String payType;

    private BigDecimal amount;

    private String tradeNo;

    /** 0 pending / 1 success / 2 failed */
    private Integer status;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
}
