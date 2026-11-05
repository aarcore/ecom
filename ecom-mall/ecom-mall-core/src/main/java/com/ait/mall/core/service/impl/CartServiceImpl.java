package com.ait.mall.core.service.impl;

import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import com.ait.mall.common.dto.OrderItemRequest;
import com.ait.mall.core.entity.Cart;
import com.ait.mall.core.entity.Product;
import com.ait.mall.core.entity.ProductSku;
import com.ait.mall.core.mapper.CartMapper;
import com.ait.mall.core.mapper.ProductMapper;
import com.ait.mall.core.mapper.ProductSkuMapper;
import com.ait.mall.core.service.CartService;
import com.ait.mall.core.service.I18nService;
import com.ait.common.i18n.LocaleContext;
import com.ait.mall.core.vo.CartItemVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cart implementation. Every quantity is validated against live SKU stock so the
 * badge and the checkout screen never promise something that cannot ship.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final I18nService i18nService;

    @Override
    public List<CartItemVo> list(Long memberId) {
        List<Cart> lines = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getMemberId, memberId)
                .orderByDesc(Cart::getId));
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = lines.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        List<Long> skuIds = lines.stream().map(Cart::getSkuId).distinct().collect(Collectors.toList());
        Map<Long, Product> products = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, ProductSku> skus = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));

        List<CartItemVo> result = new ArrayList<>(lines.size());
        for (Cart line : lines) {
            Product product = products.get(line.getProductId());
            ProductSku sku = skus.get(line.getSkuId());
            if (product == null || sku == null) {
                // product removed from the catalogue: drop the dangling line
                cartMapper.deleteById(line.getId());
                continue;
            }
            result.add(toVo(line, product, sku));
        }
        localize(result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartItemVo add(Long memberId, Long skuId, int quantity) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || Integer.valueOf(1).equals(sku.getDeleted())) {
            throw new BizException(ResultCode.SKU_NOT_FOUND);
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
        }

        Cart existing = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getMemberId, memberId)
                .eq(Cart::getSkuId, skuId));

        int requested = Math.max(1, quantity);
        int target = existing == null ? requested : existing.getQuantity() + requested;
        if (target > sku.getStock()) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                    "库存仅剩 " + sku.getStock() + " 件");
        }

        Cart saved;
        if (existing == null) {
            saved = new Cart();
            saved.setMemberId(memberId);
            saved.setProductId(sku.getProductId());
            saved.setSkuId(skuId);
            saved.setQuantity(requested);
            saved.setChecked(1);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            cartMapper.insert(saved);
        } else {
            if (target < 1) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
            existing.setQuantity(target);
            existing.setUpdatedAt(LocalDateTime.now());
            cartMapper.updateById(existing);
            saved = existing;
        }
        return toVo(saved, product, sku);
    }

    @Override
    public CartItemVo updateQuantity(Long memberId, Long cartId, int quantity) {
        Cart line = requireOwned(memberId, cartId);
        ProductSku sku = skuMapper.selectById(line.getSkuId());
        if (sku == null) {
            throw new BizException(ResultCode.SKU_NOT_FOUND);
        }
        int qty = Math.max(1, quantity);
        if (qty > sku.getStock()) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "库存仅剩 " + sku.getStock() + " 件");
        }
        line.setQuantity(qty);
        line.setUpdatedAt(LocalDateTime.now());
        cartMapper.updateById(line);
        Product product = productMapper.selectById(line.getProductId());
        return toVo(line, product, sku);
    }

    @Override
    public void updateChecked(Long memberId, Long cartId, boolean checked) {
        Cart line = requireOwned(memberId, cartId);
        line.setChecked(checked ? 1 : 0);
        line.setUpdatedAt(LocalDateTime.now());
        cartMapper.updateById(line);
    }

    @Override
    public void checkAll(Long memberId, boolean checked) {
        Cart patch = new Cart();
        patch.setChecked(checked ? 1 : 0);
        patch.setUpdatedAt(LocalDateTime.now());
        cartMapper.update(patch, Wrappers.<Cart>lambdaUpdate().eq(Cart::getMemberId, memberId));
    }

    @Override
    public void remove(Long memberId, Long cartId) {
        requireOwned(memberId, cartId);
        cartMapper.deleteById(cartId);
    }

    @Override
    public void clear(Long memberId) {
        cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getMemberId, memberId));
    }

    @Override
    public int count(Long memberId) {
        Long total = cartMapper.selectCount(Wrappers.<Cart>lambdaQuery().eq(Cart::getMemberId, memberId));
        return total == null ? 0 : total.intValue();
    }

    @Override
    public List<OrderItemRequest> checkedLines(Long memberId) {
        List<Cart> lines = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getMemberId, memberId)
                .eq(Cart::getChecked, 1));
        return lines.stream().map(line -> {
            OrderItemRequest request = new OrderItemRequest();
            request.setSkuId(line.getSkuId());
            request.setQuantity(line.getQuantity());
            return request;
        }).collect(Collectors.toList());
    }

    @Override
    public void removeSkuIds(Long memberId, List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getMemberId, memberId)
                .in(Cart::getSkuId, skuIds));
    }

    @Override
    public List<CartItemVo> buildItemViews(List<OrderItemRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> skuIds = lines.stream().map(OrderItemRequest::getSkuId).collect(Collectors.toList());
        Map<Long, ProductSku> skus = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        List<Long> productIds = skus.values().stream().map(ProductSku::getProductId)
                .distinct().collect(Collectors.toList());
        Map<Long, Product> products = productIds.isEmpty()
                ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<CartItemVo> views = new ArrayList<>(lines.size());
        for (OrderItemRequest line : lines) {
            ProductSku sku = skus.get(line.getSkuId());
            if (sku == null) {
                throw new BizException(ResultCode.SKU_NOT_FOUND);
            }
            Product product = products.get(sku.getProductId());
            CartItemVo vo = new CartItemVo();
            vo.setId(null);
            vo.setProductId(sku.getProductId());
            vo.setSkuId(sku.getId());
            vo.setProductName(product == null ? "" : product.getName());
            vo.setCover(sku.getImage() != null ? sku.getImage()
                    : (product == null ? null : product.getCover()));
            vo.setSpecValues(sku.getSpecValues());
            vo.setPrice(sku.getPrice());
            vo.setOriginalPrice(product == null ? null : product.getOriginalPrice());
            vo.setQuantity(line.getQuantity());
            vo.setStock(sku.getStock());
            vo.setChecked(1);
            vo.setAvailable(product != null
                    && Integer.valueOf(1).equals(product.getStatus())
                    && sku.getStock() >= line.getQuantity());
            views.add(vo);
        }
        return views;
    }

    private Cart requireOwned(Long memberId, Long cartId) {
        Cart line = cartMapper.selectById(cartId);
        if (line == null || !memberId.equals(line.getMemberId())) {
            throw new BizException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        return line;
    }

    /**
     * Translates product names and spec values for the whole basket in one
     * query. A cart is small, but a per-line lookup here would still double the
     * request count on every page load.
     */
    private void localize(List<CartItemVo> items) {
        if (LocaleContext.get().isSource() || items == null || items.isEmpty()) {
            return;
        }
        var locale = LocaleContext.get();
        List<Long> productIds = items.stream().map(CartItemVo::getProductId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        var overrides = i18nService.textMap("product", productIds, locale);
        var terms = i18nService.termMap(locale);
        for (CartItemVo item : items) {
            String name = overrides.getOrDefault(item.getProductId(), java.util.Map.of()).get("name");
            if (name != null && !name.isEmpty()) {
                item.setProductName(name);
            }
            if (item.getSpecValues() != null && !terms.isEmpty()) {
                item.setSpecValues(java.util.Arrays.stream(item.getSpecValues().split(","))
                        .map(part -> terms.getOrDefault(part, part))
                        .collect(Collectors.joining(",")));
            }
        }
    }

    private CartItemVo toVo(Cart line, Product product, ProductSku sku) {
        CartItemVo vo = new CartItemVo();
        vo.setId(line.getId());
        vo.setProductId(line.getProductId());
        vo.setSkuId(line.getSkuId());
        vo.setProductName(product == null ? "" : product.getName());
        vo.setCover(sku.getImage() != null ? sku.getImage() : (product == null ? null : product.getCover()));
        vo.setSpecValues(sku.getSpecValues());
        vo.setPrice(sku.getPrice());
        vo.setOriginalPrice(product == null ? null : product.getOriginalPrice());
        vo.setQuantity(line.getQuantity());
        vo.setStock(sku.getStock());
        vo.setChecked(line.getChecked());
        vo.setAvailable(product != null
                && Integer.valueOf(1).equals(product.getStatus())
                && sku.getStock() >= line.getQuantity());
        return vo;
    }
}
