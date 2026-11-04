package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * One spec dimension of a product, e.g. {@code {name: "颜色", values: ["黑","白"]}}.
 */
@Data
@Schema(description = "Product spec dimension")
public class SpecVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private List<String> values;

    public SpecVo() {
    }

    public SpecVo(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }
}
