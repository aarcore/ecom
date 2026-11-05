package com.ait.admin.rest;

import com.ait.common.api.R;
import com.ait.logging.annotation.OperationLog;
import com.ait.mall.core.entity.Banner;
import com.ait.mall.core.entity.Brand;
import com.ait.mall.core.entity.Category;
import com.ait.mall.core.entity.Notice;
import com.ait.mall.core.service.CatalogService;
import com.ait.mall.core.vo.BrandVo;
import com.ait.mall.core.vo.CategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Categories, brands, banners and notices — everything that shapes the
 * storefront beyond the product list itself.
 */
@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@Tag(name = "Admin · 分类 / 品牌 / 轮播")
public class AdminCatalogController {

    private final CatalogService catalogService;

    // ---------------------------- categories ---------------------------
    @GetMapping("/categories")
    @Operation(summary = "分类树")
    public R<List<CategoryVo>> categories() {
        return R.ok(catalogService.categoryTree(false));
    }

    @GetMapping("/categories/flat")
    @Operation(summary = "分类平铺列表")
    public R<List<CategoryVo>> flatCategories() {
        return R.ok(catalogService.listCategories(false));
    }

    @PostMapping("/categories")
    @OperationLog(module = "分类", action = "保存分类")
    @Operation(summary = "新增 / 编辑分类")
    public R<Category> saveCategory(@RequestBody Category category) {
        return R.ok(catalogService.saveCategory(category));
    }

    @DeleteMapping("/categories/{id}")
    @OperationLog(module = "分类", action = "删除分类")
    @Operation(summary = "删除分类")
    public R<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return R.ok();
    }

    // ------------------------------ brands -----------------------------
    @GetMapping("/brands")
    @Operation(summary = "品牌列表")
    public R<List<BrandVo>> brands() {
        return R.ok(catalogService.listBrands(false));
    }

    @PostMapping("/brands")
    @OperationLog(module = "品牌", action = "保存品牌")
    @Operation(summary = "新增 / 编辑品牌")
    public R<Brand> saveBrand(@RequestBody Brand brand) {
        return R.ok(catalogService.saveBrand(brand));
    }

    @DeleteMapping("/brands/{id}")
    @OperationLog(module = "品牌", action = "删除品牌")
    @Operation(summary = "删除品牌")
    public R<Void> deleteBrand(@PathVariable Long id) {
        catalogService.deleteBrand(id);
        return R.ok();
    }

    // ----------------------------- banners -----------------------------
    @GetMapping("/banners")
    @Operation(summary = "轮播列表")
    public R<List<Banner>> banners(@RequestParam(required = false) String position) {
        return R.ok(catalogService.listBanners(position, false));
    }

    @PostMapping("/banners")
    @OperationLog(module = "轮播", action = "保存轮播")
    @Operation(summary = "新增 / 编辑轮播")
    public R<Banner> saveBanner(@RequestBody Banner banner) {
        return R.ok(catalogService.saveBanner(banner));
    }

    @DeleteMapping("/banners/{id}")
    @OperationLog(module = "轮播", action = "删除轮播")
    @Operation(summary = "删除轮播")
    public R<Void> deleteBanner(@PathVariable Long id) {
        catalogService.deleteBanner(id);
        return R.ok();
    }

    // ----------------------------- notices -----------------------------
    @GetMapping("/notices")
    @Operation(summary = "公告列表")
    public R<List<Notice>> notices() {
        return R.ok(catalogService.listNotices(false));
    }

    @PostMapping("/notices")
    @OperationLog(module = "公告", action = "保存公告")
    @Operation(summary = "新增 / 编辑公告")
    public R<Notice> saveNotice(@RequestBody Notice notice) {
        return R.ok(catalogService.saveNotice(notice));
    }

    @DeleteMapping("/notices/{id}")
    @OperationLog(module = "公告", action = "删除公告")
    @Operation(summary = "删除公告")
    public R<Void> deleteNotice(@PathVariable Long id) {
        catalogService.deleteNotice(id);
        return R.ok();
    }
}
