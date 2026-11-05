package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Shipping address")
public class AddressVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String receiver;

    private String mobile;

    private String province;

    private String city;

    private String district;

    private String detail;

    private String postalCode;

    private Integer isDefault;

    @Schema(description = "Concatenated readable address")
    private String fullAddress;

    private LocalDateTime createdAt;
}
