package com.ait.mall.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One translated field of one entity, addressed by (entity, entityId, field, lang).
 *
 * <p>Deliberately not a {@code BaseEntity}: these rows are a pure overlay, they
 * are never soft deleted (a removed translation is simply absent) and they are
 * always read in bulk by language.
 */
@Data
@TableName("shop_i18n_text")
public class I18nText implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** product | category | brand | coupon | banner | notice */
    private String entity;

    private Long entityId;

    /** name | subtitle | detail | description | title | content */
    private String field;

    private String lang;

    private String value;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
