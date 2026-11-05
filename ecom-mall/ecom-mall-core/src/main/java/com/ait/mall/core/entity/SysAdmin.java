package com.ait.mall.core.entity;

import com.ait.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Back-office operator.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_admin")
public class SysAdmin extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt hash; never serialised to clients. */
    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private String role;

    private Integer enabled;

    private LocalDateTime lastLoginAt;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
