package com.ait.mall.core.service;

import com.ait.mall.core.vo.CartItemVo;
import com.ait.mall.common.dto.OrderItemRequest;

import java.util.List;

/**
 * Cart operations for the storefront.
 */
public interface CartService {

    List<CartItemVo> list(Long memberId);

    /** Adds to cart or increments the existing line. Quantities are clamped to stock. */
    CartItemVo add(Long memberId, Long skuId, int quantity);

    CartItemVo updateQuantity(Long memberId, Long cartId, int quantity);

    void updateChecked(Long memberId, Long cartId, boolean checked);

    void checkAll(Long memberId, boolean checked);

    void remove(Long memberId, Long cartId);

    void clear(Long memberId);

    int count(Long memberId);

    /** Checked lines formatted as checkout requests. */
    List<OrderItemRequest> checkedLines(Long memberId);

    void removeSkuIds(Long memberId, List<Long> skuIds);

    /** Expands arbitrary sku+quantity requests into display rows. */
    List<CartItemVo> buildItemViews(List<OrderItemRequest> lines);
}
