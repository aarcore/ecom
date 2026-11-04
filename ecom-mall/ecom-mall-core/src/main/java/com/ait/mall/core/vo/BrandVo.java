package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "Brand")
public class BrandVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String logo;

    private String description;

    private Integer sort;

    private Integer visible;

    @Schema(description = "Number of products of this brand")
    private Long productCount;

    private LocalDateTime createdAt;
}
