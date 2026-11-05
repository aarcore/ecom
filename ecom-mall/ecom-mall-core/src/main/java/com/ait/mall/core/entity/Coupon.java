package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon template.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_coupon")
public class Coupon extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** amount | discount */
    private String type;

    /** Face value when {@code type = amount}. */
    private BigDecimal amount;

    /** Rate out of 10 when {@code type = discount} (8.5 means 85%). */
    private BigDecimal discount;

    private BigDecimal minAmount;

    private Integer total;

    private Integer taken;

    /** Validity window in days counted from the moment of receiving. */
    private Integer validDays;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer enabled;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
