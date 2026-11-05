package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Shipping address book entry.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_member_address")
public class MemberAddress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private String receiver;

    private String mobile;

    private String province;

    private String city;

    private String district;

    private String detail;

    private String postalCode;

    private Integer isDefault;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
