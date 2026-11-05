package com.ait.mall.core.service.impl;

import com.ait.common.api.PageResult;
import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import com.ait.common.utils.JsonUtils;
import com.ait.mall.core.dto.ProductSaveRequest;
import com.ait.mall.core.entity.Category;
import com.ait.mall.core.entity.Product;
import com.ait.mall.core.entity.ProductSku;
import com.ait.mall.core.mapper.BrandMapper;
import com.ait.mall.core.mapper.CategoryMapper;
import com.ait.mall.core.mapper.ProductMapper;
import com.ait.mall.core.mapper.ProductSkuMapper;
import com.ait.mall.core.query.AdminProductQuery;
import com.ait.mall.core.service.ProductService;
import com.ait.mall.core.service.support.ContentLocalizer;
import com.ait.mall.core.vo.ProductVo;
import com.ait.mall.core.vo.SkuVo;
import com.ait.mall.core.vo.SpecVo;
import com.ait.common.utils.QueryHelpPlus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final ContentLocalizer localizer;

    @Override
    public PageResult<ProductVo> pageForAdmin(String keyword, Long categoryId, Integer status, int page, int limit) {
        AdminProductQuery query = new AdminProductQuery();
        query.setKeyword(keyword);
        query.setCategoryId(categoryId);
        query.setStatus(status);

        // QueryHelpPlus turns the annotated fields into the WHERE clause, which
        // keeps this method free of hand-rolled condition chains.
        QueryWrapper<Product> wrapper = QueryHelpPlus.getPredicate(new Product(), query);
        wrapper.orderByDesc("id");

        Page<Product> result = productMapper.selectPage(new Page<>(page, limit), wrapper);
        List<ProductVo> rows = enrich(result.getRecords(), false);
        localizer.products(rows, false);
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), rows);
    }

    @Override
    public PageResult<ProductVo> pageForStore(String keyword, Long categoryId, Long brandId, String sort,
                                              Integer minPrice, Integer maxPrice, int page, int limit) {
        LambdaQueryWrapper<Product> wrapper = Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, 1);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getName, kw)
                    .or().like(Product::getKeywords, kw)
                    .or().like(Product::getSubtitle, kw));
        }
        if (categoryId != null) {
            // include children so tapping a root category shows everything beneath it
            List<Long> ids = new ArrayList<>();
            ids.add(categoryId);
            List<Category> children = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                    .eq(Category::getParentId, categoryId));
            ids.addAll(children.stream().map(Category::getId).collect(Collectors.toList()));
            wrapper.in(Product::getCategoryId, ids);
        }
        if (brandId != null) {
            wrapper.eq(Product::getBrandId, brandId);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, BigDecimal.valueOf(minPrice));
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, BigDecimal.valueOf(maxPrice));
        }

        switch (sort == null ? "" : sort) {
            case "price_asc":
                wrapper.orderByAsc(Product::getPrice);
                break;
            case "price_desc":
                wrapper.orderByDesc(Product::getPrice);
                break;
            case "sales":
                wrapper.orderByDesc(Product::getSales);
                break;
            case "hot":
                wrapper.orderByDesc(Product::getIsHot).orderByDesc(Product::getSales);
                break;
            case "newest":
            default:
                wrapper.orderByDesc(Product::getId);
                break;
        }

        Page<Product> result = productMapper.selectPage(new Page<>(page, limit), wrapper);
        List<ProductVo> rows = enrich(result.getRecords(), false);
        localizer.products(rows, false);
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), rows);
    }

    @Override
    public ProductVo detail(Long productId, boolean forStore) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (forStore && !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
        }
        List<ProductSku> skus = skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getProductId, productId)
                .orderByAsc(ProductSku::getId));
        ProductVo vo = toVo(product, true);
        vo.setSkuList(skus.stream().map(this::toSkuVo).collect(Collectors.toList()));
        // detail used to skip the name joins that listings do, so the buy box
        // had no category or brand line to render
        vo.setCategoryName(loadNames(Collections.singletonList(product.getCategoryId()), true)
                .get(product.getCategoryId()));
        if (product.getBrandId() != null) {
            vo.setBrandName(loadNames(Collections.singletonList(product.getBrandId()), false)
                    .get(product.getBrandId()));
        }
        localizer.products(Collections.singletonList(vo), true);
        return vo;
    }

    @Override
    public List<ProductVo> recommended(int limit) {
        return query(limit, w -> w.eq(Product::getIsRecommend, 1).orderByDesc(Product::getSort));
    }

    @Override
    public List<ProductVo> newest(int limit) {
        return query(limit, w -> w.orderByDesc(Product::getId));
    }

    @Override
    public List<ProductVo> hot(int limit) {
        return query(limit, w -> w.eq(Product::getIsHot, 1).orderByDesc(Product::getSales));
    }

    @Override
    public List<ProductVo> related(Long productId, int limit) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Collections.emptyList();
        }
        List<Product> others = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, 1)
                .eq(Product::getCategoryId, product.getCategoryId())
                .ne(Product::getId, productId)
                .orderByDesc(Product::getSales)
                .last("limit " + Math.max(1, limit)));
        List<ProductVo> rows = enrich(others, false);
        localizer.products(rows, false);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVo save(ProductSaveRequest request) {
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "分类不存在");
        }

        Product product = request.getId() == null ? new Product() : productMapper.selectById(request.getId());
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }

        product.setCategoryId(request.getCategoryId());
        product.setBrandId(request.getBrandId());
        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setCover(request.getCover());
        product.setImages(JsonUtils.toJson(request.getImages() == null
                ? Collections.emptyList() : request.getImages()));
        product.setDetail(request.getDetail());
        product.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setUnit(request.getUnit() == null ? "件" : request.getUnit());
        product.setSpecs(JsonUtils.toJson(request.getSpecs() == null
                ? Collections.emptyList() : request.getSpecs()));
        product.setKeywords(request.getKeywords());
        product.setIsNew(request.getIsNew() == null ? 0 : request.getIsNew());
        product.setIsHot(request.getIsHot() == null ? 0 : request.getIsHot());
        product.setIsRecommend(request.getIsRecommend() == null ? 0 : request.getIsRecommend());
        product.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        product.setSort(request.getSort() == null ? 0 : request.getSort());

        if (product.getId() == null) {
            product.setStock(0);
            product.setSales(0);
            product.setViews(0);
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }

        saveSkus(product.getId(), request.getSkus());
        productMapper.refreshAggregates(product.getId());

        Product refreshed = productMapper.selectById(product.getId());
        ProductVo vo = toVo(refreshed, true);
        vo.setSkuList(skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                        .eq(ProductSku::getProductId, product.getId())
                        .orderByAsc(ProductSku::getId))
                .stream().map(this::toSkuVo).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public void updateStatus(Long productId, int status) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        Product patch = new Product();
        patch.setId(productId);
        patch.setStatus(status);
        productMapper.updateById(patch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        // soft delete parent; SKUs follow so they disappear from checkout lookups
        skuMapper.delete(Wrappers.<ProductSku>lambdaQuery().eq(ProductSku::getProductId, productId));
        productMapper.deleteById(productId);
    }

    @Override
    public void increaseViews(Long productId) {
        productMapper.increaseViews(productId);
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private void saveSkus(Long productId, List<ProductSaveRequest.SkuSaveRequest> skus) {
        if (skus == null || skus.isEmpty()) {
            // keep the single implicit SKU in sync with the product level price
            Product product = productMapper.selectById(productId);
            List<ProductSku> existing = skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                    .eq(ProductSku::getProductId, productId));
            if (existing.isEmpty()) {
                ProductSku sku = new ProductSku();
                sku.setProductId(productId);
                sku.setSkuCode("SKU-" + productId + "-1");
                sku.setSpecValues("标准");
                sku.setImage(product.getCover());
                sku.setPrice(product.getPrice());
                sku.setStock(0);
                sku.setSales(0);
                skuMapper.insert(sku);
            }
            return;
        }

        List<Long> keepIds = new ArrayList<>();
        for (ProductSaveRequest.SkuSaveRequest row : skus) {
            ProductSku sku;
            if (row.getId() != null) {
                sku = skuMapper.selectById(row.getId());
                if (sku == null) {
                    continue;
                }
            } else {
                sku = new ProductSku();
                sku.setProductId(productId);
                sku.setSales(0);
            }
            sku.setSkuCode(row.getSkuCode());
            sku.setSpecValues(row.getSpecValues());
            sku.setImage(row.getImage());
            sku.setPrice(row.getPrice() == null ? BigDecimal.ZERO : row.getPrice());
            sku.setStock(row.getStock() == null ? 0 : row.getStock());
            if (sku.getId() == null) {
                skuMapper.insert(sku);
            } else {
                skuMapper.updateById(sku);
            }
            keepIds.add(sku.getId());
        }

        List<ProductSku> stale = skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getProductId, productId));
        for (ProductSku sku : stale) {
            if (!keepIds.contains(sku.getId())) {
                skuMapper.deleteById(sku.getId());
            }
        }
    }

    private List<ProductVo> query(int limit, java.util.function.Consumer<LambdaQueryWrapper<Product>> customizer) {
        LambdaQueryWrapper<Product> wrapper = Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1);
        customizer.accept(wrapper);
        wrapper.last("limit " + Math.max(1, Math.min(limit, 50)));
        List<Product> products = productMapper.selectList(wrapper);
        List<ProductVo> rows = enrich(products, false);
        localizer.products(rows, false);
        return rows;
    }

    private List<ProductVo> enrich(List<Product> products, boolean withDetail) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> categories = loadNames(products.stream()
                .map(Product::getCategoryId).distinct().collect(Collectors.toList()), true);
        Map<Long, String> brands = loadNames(products.stream()
                .map(Product::getBrandId).filter(java.util.Objects::nonNull).distinct()
                .collect(Collectors.toList()), false);

        List<ProductVo> result = new ArrayList<>(products.size());
        for (Product product : products) {
            ProductVo vo = toVo(product, withDetail);
            vo.setCategoryName(categories.get(product.getCategoryId()));
            vo.setBrandName(brands.get(product.getBrandId()));
            result.add(vo);
        }
        return result;
    }

    private Map<Long, String> loadNames(List<Long> ids, boolean category) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        if (category) {
            categoryMapper.selectBatchIds(ids).forEach(c -> map.put(c.getId(), c.getName()));
        } else {
            brandMapper.selectBatchIds(ids).forEach(b -> map.put(b.getId(), b.getName()));
        }
        return map;
    }

    private ProductVo toVo(Product product, boolean withDetail) {
        ProductVo vo = new ProductVo();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setCover(product.getCover());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setViews(product.getViews());
        vo.setUnit(product.getUnit());
        vo.setCategoryId(product.getCategoryId());
        vo.setBrandId(product.getBrandId());
        vo.setStatus(product.getStatus());
        vo.setIsNew(product.getIsNew());
        vo.setIsHot(product.getIsHot());
        vo.setIsRecommend(product.getIsRecommend());
        vo.setSort(product.getSort());
        vo.setKeywords(product.getKeywords());
        vo.setCreatedAt(product.getCreatedAt());
        if (withDetail) {
            vo.setDetail(product.getDetail());
            vo.setImageList(JsonUtils.parseStringList(product.getImages()));
            vo.setSpecList(JsonUtils.parseList(product.getSpecs(), SpecVo.class));
        }
        return vo;
    }

    private SkuVo toSkuVo(ProductSku sku) {
        SkuVo vo = new SkuVo();
        vo.setId(sku.getId());
        vo.setProductId(sku.getProductId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setSpecValues(sku.getSpecValues());
        vo.setImage(sku.getImage());
        vo.setPrice(sku.getPrice());
        vo.setStock(sku.getStock());
        vo.setSales(sku.getSales());
        return vo;
    }
}
