package com.ait.mall.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order line snapshot; product names and prices are frozen at checkout time.
 */
@Data
@TableName("shop_order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private Long skuId;

    private String productName;

    private String productCover;

    private String specValues;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;
}
