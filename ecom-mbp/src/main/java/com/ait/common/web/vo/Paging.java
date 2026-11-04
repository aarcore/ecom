package com.ait.common.web.vo;

import com.ait.common.api.PageResult;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Alias kept for compatibility with the original module API; new code should
 * use {@link PageResult}.
 */
public class Paging<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total = 0;
    private List<T> records = Collections.emptyList();

    public Paging() {
    }

    public Paging(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    public static <T> Paging<T> from(PageResult<T> page) {
        return new Paging<>(page.getTotal(), page.getRecords());
    }

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
}
