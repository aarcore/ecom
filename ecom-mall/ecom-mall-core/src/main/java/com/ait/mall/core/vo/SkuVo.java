package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "Product SKU")
public class SkuVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private String skuCode;

    @Schema(description = "Comma separated spec values, e.g. 幻夜黑,12+256G")
    private String specValues;

    private String image;

    private BigDecimal price;

    private Integer stock;

    private Integer sales;
}
