package com.ait.common.web.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("Query parameter objects")
public class QueryParam implements Serializable {
    private static final long serialVersionUID = -3263921252635611410L;

    @ApiModelProperty(value = "")
    private Integer page = 1;

    @ApiModelProperty(value = "")
    private Integer limit = 10;

    @ApiModelProperty(value = "")
    private String keyword;

    @ApiModelProperty(value = "Current page")
    public void setCurrent(Integer current) {
        if(current == null || current <= 0) {
            this.page = 1;
        } else {
            this.page = current;
        }
    }

    public void setSize(Integer size) {
        if (size == null || size <= 0) {
            this.limit = 10;
        } else {
            this.limit = size;
        }
    }
}
