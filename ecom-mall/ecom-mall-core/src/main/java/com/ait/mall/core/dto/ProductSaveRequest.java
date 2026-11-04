package com.ait.mall.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Create/update payload for a product, including its SKU matrix.
 */
@Data
@Schema(description = "Product save payload")
public class ProductSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Null for create")
    private Long id;

    @NotNull(message = "请选择商品分类")
    private Long categoryId;

    private Long brandId;

    @NotBlank(message = "请输入商品名称")
    private String name;

    private String subtitle;

    private String cover;

    private List<String> images;

    private String detail;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String unit;

    private List<SpecVo> specs;

    private String keywords;

    private Integer isNew;

    private Integer isHot;

    private Integer isRecommend;

    @Schema(description = "1 on sale / 0 off shelf")
    private Integer status;

    private Integer sort;

    private List<SkuSaveRequest> skus;

    @Data
    @Schema(description = "SKU row inside a product save payload")
    public static class SkuSaveRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;

        private String skuCode;

        private String specValues;

        private String image;

        private BigDecimal price;

        private Integer stock;
    }

    @Data
    @Schema(description = "Spec dimension")
    public static class SpecVo implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        private List<String> values;
    }
}
