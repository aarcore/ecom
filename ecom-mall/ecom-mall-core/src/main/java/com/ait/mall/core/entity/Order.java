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
 * Order header. Shipping details are snapshotted so later address edits never
 * rewrite history.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_order")
public class Order extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long memberId;

    private String memberNickname;

    private Integer status;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private String payType;

    private LocalDateTime paidAt;

    private String receiver;

    private String receiverMobile;

    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverDetail;

    private String remark;

    private Long couponId;

    private String platform;

    private String expressCompany;

    private String expressNo;

    private LocalDateTime shippedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime closedAt;

    private String adminRemark;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
