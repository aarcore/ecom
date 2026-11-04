package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Product summary for listings")
public class ProductVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String subtitle;

    private String cover;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private Integer views;

    private String unit;

    private Long categoryId;

    private String categoryName;

    private Long brandId;

    private String brandName;

    private Integer status;

    private Integer isNew;

    private Integer isHot;

    private Integer isRecommend;

    private Integer sort;

    private String keywords;

    private LocalDateTime createdAt;

    @Schema(description = "Only present on detail responses")
    private List<String> imageList;

    @Schema(description = "Only present on detail responses")
    private String detail;

    @Schema(description = "Only present on detail responses")
    private List<SpecVo> specList;

    @Schema(description = "Only present on detail responses")
    private List<SkuVo> skuList;

    /** Derived label for the storefront ribbon. */
    public String getPriceLabel() {
        return price == null ? "" : price.stripTrailingZeros().toPlainString();
    }
}
