package com.ait.mall.core.service;

import com.ait.common.api.PageResult;
import com.ait.mall.core.dto.ProductSaveRequest;
import com.ait.mall.core.vo.ProductVo;

import java.util.List;

public interface ProductService {

    PageResult<ProductVo> pageForAdmin(String keyword, Long categoryId, Integer status, int page, int limit);

    /** Storefront listing with sort modes: newest | hot | price_asc | price_desc | sales. */
    PageResult<ProductVo> pageForStore(String keyword, Long categoryId, Long brandId, String sort,
                                       Integer minPrice, Integer maxPrice, int page, int limit);

    ProductVo detail(Long productId, boolean forStore);

    List<ProductVo> recommended(int limit);

    List<ProductVo> newest(int limit);

    List<ProductVo> hot(int limit);

    List<ProductVo> related(Long productId, int limit);

    ProductVo save(ProductSaveRequest request);

    void updateStatus(Long productId, int status);

    void delete(Long productId);

    void increaseViews(Long productId);
}
