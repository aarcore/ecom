package com.ait.mall.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Coupon instance held by a member.
 */
@Data
@TableName("shop_member_coupon")
public class MemberCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long couponId;

    private Long memberId;

    /** 0 unused / 1 used / 2 expired */
    private Integer status;

    private String orderNo;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private LocalDateTime usedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
