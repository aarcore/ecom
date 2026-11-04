package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Concrete purchasable variant.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_product_sku")
public class ProductSku extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String skuCode;

    /** Comma separated spec values, e.g. {@code 幻夜黑,12+256G}. */
    private String specValues;

    private String image;

    private BigDecimal price;

    private Integer stock;

    private Integer sales;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
