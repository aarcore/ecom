package com.ait.mall.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Cart line. A (member, sku) pair is unique, enforced by a database index.
 */
@Data
@TableName("shop_cart")
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Long productId;

    private Long skuId;

    private Integer quantity;

    private Integer checked;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
