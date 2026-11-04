package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Home / category / profile carousel slide.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_banner")
public class Banner extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String image;

    /** none | product | category | url */
    private String linkType;

    private String linkValue;

    /** home | category | mine */
    private String position;

    private Integer sort;

    private Integer enabled;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
