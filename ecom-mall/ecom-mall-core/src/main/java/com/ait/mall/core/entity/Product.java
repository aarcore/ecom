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
 * Sellable product (SPU). Prices and stock on this row are denormalised
 * aggregates over {@link ProductSku} so listing queries never need a join.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_product")
public class Product extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private Long brandId;

    private String name;

    private String subtitle;

    private String cover;

    /** JSON array of image URLs. */
    private String images;

    private String detail;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private Integer views;

    private String unit;

    /** JSON array of {@code {name, values[]}} spec dimensions. */
    private String specs;

    private String keywords;

    private Integer isNew;

    private Integer isHot;

    private Integer isRecommend;

    /** 1 on sale / 0 off shelf */
    private Integer status;

    private Integer sort;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
