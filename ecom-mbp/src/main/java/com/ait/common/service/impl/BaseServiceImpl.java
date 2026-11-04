package com.ait.common.service.impl;

import com.ait.common.api.PageResult;
import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import com.ait.common.service.BaseService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of the shared query helpers.
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {

    @Override
    public <V> PageResult<V> pageOf(Page<T> page, Wrapper<T> wrapper, Function<T, V> mapper) {
        Page<T> result = this.page(page, wrapper);
        List<V> records = result.getRecords() == null
                ? Collections.emptyList()
                : result.getRecords().stream().map(mapper).collect(Collectors.toList());
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public Page<T> pageBy(Page<T> page, Wrapper<T> wrapper) {
        return this.page(page, wrapper);
    }

    @Override
    public T requireById(Long id, String label) {
        T entity = id == null ? null : this.getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND, label + "不存在");
        }
        return entity;
    }

    @Override
    public <V> List<V> listOf(Wrapper<T> wrapper, Function<T, V> mapper) {
        List<T> list = this.list(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(mapper).collect(Collectors.toList());
    }
}
