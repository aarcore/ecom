package com.ait.mall.core.service;

import com.ait.mall.core.entity.Banner;
import com.ait.mall.core.entity.Brand;
import com.ait.mall.core.entity.Category;
import com.ait.mall.core.entity.Notice;
import com.ait.mall.core.vo.BrandVo;
import com.ait.mall.core.vo.CategoryVo;

import java.util.List;

/**
 * Catalogue taxonomy and merchandising content.
 */
public interface CatalogService {

    /** Flat list with product counts. */
    List<CategoryVo> listCategories(boolean visibleOnly);

    /** Two level tree used by the storefront category page and the admin tree select. */
    List<CategoryVo> categoryTree(boolean visibleOnly);

    Category saveCategory(Category category);

    void deleteCategory(Long id);

    List<BrandVo> listBrands(boolean visibleOnly);

    Brand saveBrand(Brand brand);

    void deleteBrand(Long id);

    List<Banner> listBanners(String position, boolean enabledOnly);

    Banner saveBanner(Banner banner);

    void deleteBanner(Long id);

    List<Notice> listNotices(boolean enabledOnly);

    Notice saveNotice(Notice notice);

    void deleteNotice(Long id);
}
