package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Two level category tree, flattened into one table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_category")
public class Category extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 0 for roots. */
    private Long parentId;

    private String name;

    private String icon;

    private String banner;

    private Integer level;

    private Integer sort;

    private Integer visible;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
