package com.ait.common.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.ait.common.service.BaseService;
import com.ait.common.web.param.OrderQueryParam;
import com.ait.common.web.param.QueryParam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

public class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {
    protected Page setPageParam(QueryParam queryParam) {
        return setPageParam(queryParam, null);
    }

    protected Page setPageParam(QueryParam queryParam, OrderItem defaultOrder) {
        Page page = new Page();
        page.setCurrent(queryParam.getPage());
        page.setSize(queryParam.getLimit());

        if(queryParam instanceof OrderQueryParam) {
            OrderQueryParam orderQueryParam = (OrderQueryParam) queryParam;
            List<OrderItem> orderItems = orderQueryParam.getOrders();

            if(CollectionUtil.isEmpty(orderItems)) {
                page.setOrders(Arrays.asList(defaultOrder));
            }else {
                page.setOrders(orderItems);
            }
        } else {
            page.setOrders(Arrays.asList(defaultOrder));
        }

        return page;
    }

    protected void getPage(Pageable pageable) {
        String order = null;
        if(pageable.getSort() != null) {
            order = pageable.getSort().toString();
            order = order.replace(":", "");
            if("UNSSTORTED".equals(order)){
                order = "id desc";
            }

            PageHelper.startPage(pageable.getPageNumber() + 1, pageable.getPageSize(), order);
        }
    }
}
