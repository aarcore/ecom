package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Announcement shown as the scrolling line on the storefront home page.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_notice")
public class Notice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer enabled;

    private Integer sort;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
