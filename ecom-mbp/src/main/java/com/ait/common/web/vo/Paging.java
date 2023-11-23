package com.ait.common.web.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Paging<T> implements Serializable {
    private static final long serialVersionUID = -1683800405530086022L;

    @ApiModelProperty("Total Count")
    @JSONField(name = "total")
    @JsonProperty("total")
    private long total = 0;

    @ApiModelProperty("Data List")
    @JSONField(name = "records")
    @JsonProperty("records")
    private List<T> records = Collections.emptyList();

    public Paging() {}

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    @Override
    public String toString() {
        return "Paging{" + "total=" + total + ", records=" + records + '}';
    }
}
