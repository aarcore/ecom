package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Store customer profile")
public class MemberVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String mobile;

    private String nickname;

    private String avatar;

    private Integer gender;

    private String genderText;

    private String platform;

    private Integer level;

    private String levelName;

    private Integer points;

    private BigDecimal balance;

    private BigDecimal totalSpent;

    private Integer orderCount;

    private Integer status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    @Schema(description = "Masked mobile for list screens, e.g. 138****8000")
    private String mobileMasked;
}
