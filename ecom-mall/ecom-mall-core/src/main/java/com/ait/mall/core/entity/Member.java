package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Store customer. One row serves both the H5 storefront and the WeChat mini
 * program; {@link #platform} records where the account was created while
 * {@link #wxOpenid} is only populated for mini-program logins.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_member")
public class Member extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String mobile;

    private String password;

    private String nickname;

    private String avatar;

    /** 0 unknown / 1 male / 2 female */
    private Integer gender;

    private LocalDate birthday;

    private String wxOpenid;

    private String wxUnionid;

    private String platform;

    private Integer level;

    private Integer points;

    private BigDecimal balance;

    private BigDecimal totalSpent;

    private Integer orderCount;

    /** 1 normal / 0 disabled */
    private Integer status;

    private LocalDateTime lastLoginAt;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
