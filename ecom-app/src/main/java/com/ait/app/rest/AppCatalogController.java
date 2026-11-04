package com.ait.app.rest;

import com.ait.common.api.PageResult;
import com.ait.common.api.R;
import com.ait.common.i18n.LocaleContext;
import com.ait.common.i18n.SupportedLocale;
import com.ait.mall.core.entity.Banner;
import com.ait.mall.core.entity.Notice;
import com.ait.mall.core.service.CatalogService;
import com.ait.mall.core.service.ProductService;
import com.ait.mall.core.vo.BrandVo;
import com.ait.mall.core.vo.CategoryVo;
import com.ait.mall.core.vo.ProductVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public catalogue browsing — no token required so the storefront can render
 * before the customer signs in.
 */
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Tag(name = "App · 商品与内容")
public class AppCatalogController {

    private final ProductService productService;
    private final CatalogService catalogService;

    @GetMapping("/home")
    @Operation(summary = "首页聚合数据（轮播、分类、推荐位、公告）")
    public R<Map<String, Object>> home() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("banners", catalogService.listBanners("home", true));
        payload.put("categories", catalogService.categoryTree(true));
        payload.put("notices", catalogService.listNotices(true));
        payload.put("recommend", productService.recommended(8));
        payload.put("newArrivals", productService.newest(6));
        payload.put("hot", productService.hot(6));
        return R.ok(payload);
    }

    @GetMapping("/products")
    @Operation(summary = "商品列表 / 搜索")
    public R<PageResult<ProductVo>> products(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) Long brandId,
                                             @RequestParam(required = false) String sort,
                                             @RequestParam(required = false) Integer minPrice,
                                             @RequestParam(required = false) Integer maxPrice,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int limit) {
        return R.ok(productService.pageForStore(keyword, categoryId, brandId, sort,
                minPrice, maxPrice, page, limit));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "商品详情")
    public R<Map<String, Object>> productDetail(@PathVariable Long id) {
        productService.increaseViews(id);
        ProductVo product = productService.detail(id, true);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("product", product);
        payload.put("related", productService.related(id, 6));
        return R.ok(payload);
    }

    @GetMapping("/locales")
    @Operation(summary = "支持的语言列表")
    public R<Map<String, Object>> locales() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("current", LocaleContext.get().getCode());
        payload.put("locales", SupportedLocale.all().stream().map(locale -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", locale.getCode());
            item.put("label", locale.getLabel());
            item.put("source", locale.isSource());
            return item;
        }).collect(java.util.stream.Collectors.toList()));
        return R.ok(payload);
    }

    @GetMapping("/categories")
    @Operation(summary = "分类树")
    public R<List<CategoryVo>> categories() {
        return R.ok(catalogService.categoryTree(true));
    }

    @GetMapping("/brands")
    @Operation(summary = "品牌列表")
    public R<List<BrandVo>> brands() {
        return R.ok(catalogService.listBrands(true));
    }

    @GetMapping("/banners")
    @Operation(summary = "轮播图")
    public R<List<Banner>> banners(@RequestParam(defaultValue = "home") String position) {
        return R.ok(catalogService.listBanners(position, true));
    }

    @GetMapping("/notices")
    @Operation(summary = "公告")
    public R<List<Notice>> notices() {
        return R.ok(catalogService.listNotices(true));
    }
}
