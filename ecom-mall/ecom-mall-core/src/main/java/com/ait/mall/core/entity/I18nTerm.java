package com.ait.mall.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Translation for a small closed vocabulary where an id would be meaningless:
 * spec dimension names ("颜色"), spec values ("哑光黑") and units ("台").
 *
 * <p>Keyed by the Chinese source string itself. That is safe here because the
 * values are short, unique and stable — unlike product names, which get edited
 * and therefore need an id.
 */
@Data
@TableName("shop_i18n_term")
public class I18nTerm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String source;

    private String lang;

    private String value;

    private LocalDateTime createdAt;
}
