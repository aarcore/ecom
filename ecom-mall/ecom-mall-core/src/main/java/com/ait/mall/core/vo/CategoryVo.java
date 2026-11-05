package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Category node with children")
public class CategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String name;

    private String icon;

    private String banner;

    private Integer level;

    private Integer sort;

    private Integer visible;

    @Schema(description = "Number of products directly in this category")
    private Long productCount;

    private List<CategoryVo> children = new ArrayList<>();
}
