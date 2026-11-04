package com.ait.common.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ait.common.api.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * Adds a handful of query helpers on top of {@link IService}.
 */
public interface BaseService<T> extends IService<T> {

    /**
     * Runs a paged query and converts the entity page into a DTO page.
     */
    <V> PageResult<V> pageOf(Page<T> page, Wrapper<T> wrapper, Function<T, V> mapper);

    /**
     * Runs a paged query returning entities.
     */
    Page<T> pageBy(Page<T> page, Wrapper<T> wrapper);

    /**
     * Convenience wrapper that throws a business exception when nothing matches.
     */
    T requireById(Long id, String label);

    <V> List<V> listOf(Wrapper<T> wrapper, Function<T, V> mapper);
}
