package com.ait.mall.core.service.impl;

import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import com.ait.mall.core.entity.Banner;
import com.ait.mall.core.entity.Brand;
import com.ait.mall.core.entity.Category;
import com.ait.mall.core.entity.Notice;
import com.ait.mall.core.mapper.BannerMapper;
import com.ait.mall.core.mapper.BrandMapper;
import com.ait.mall.core.mapper.CategoryMapper;
import com.ait.mall.core.mapper.NoticeMapper;
import com.ait.mall.core.mapper.ProductMapper;
import com.ait.mall.core.service.CatalogService;
import com.ait.mall.core.service.I18nService;
import com.ait.mall.core.service.support.ContentLocalizer;
import com.ait.mall.core.vo.BrandVo;
import com.ait.mall.core.vo.CategoryVo;
import com.ait.common.i18n.LocaleContext;
import com.ait.common.i18n.SupportedLocale;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final BannerMapper bannerMapper;
    private final NoticeMapper noticeMapper;
    private final ProductMapper productMapper;
    private final ContentLocalizer localizer;
    private final I18nService i18nService;

    // ------------------------------------------------------------------
    //  Categories
    // ------------------------------------------------------------------
    @Override
    public List<CategoryVo> listCategories(boolean visibleOnly) {
        var wrapper = Wrappers.<Category>lambdaQuery().orderByDesc(Category::getSort).orderByAsc(Category::getId);
        if (visibleOnly) {
            wrapper.eq(Category::getVisible, 1);
        }
        List<Category> categories = categoryMapper.selectList(wrapper);
        Map<Long, Long> counts = countProductsByCategory();
        List<CategoryVo> rows = categories.stream().map(c -> toVo(c, counts)).collect(Collectors.toList());
        localizer.categories(rows);
        return rows;
    }

    @Override
    public List<CategoryVo> categoryTree(boolean visibleOnly) {
        List<CategoryVo> flat = listCategories(visibleOnly);
        Map<Long, CategoryVo> byId = new HashMap<>();
        flat.forEach(c -> byId.put(c.getId(), c));

        List<CategoryVo> roots = new ArrayList<>();
        for (CategoryVo vo : flat) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            } else {
                CategoryVo parent = byId.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    @Override
    public Category saveCategory(Category category) {
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getId() != null && category.getId().equals(category.getParentId())) {
            throw new BizException(ResultCode.BAD_REQUEST, "上级分类不能是自己");
        }
        if (category.getParentId() != 0) {
            Category parent = categoryMapper.selectById(category.getParentId());
            if (parent == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "上级分类不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() != 0) {
                throw new BizException(ResultCode.BAD_REQUEST, "最多支持两级分类");
            }
            category.setLevel(2);
        } else {
            category.setLevel(1);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getVisible() == null) {
            category.setVisible(1);
        }

        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Long children = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(ResultCode.CATEGORY_HAS_CHILDREN);
        }
        Long products = productMapper.selectCount(Wrappers.<com.ait.mall.core.entity.Product>lambdaQuery()
                .eq(com.ait.mall.core.entity.Product::getCategoryId, id));
        if (products != null && products > 0) {
            throw new BizException(ResultCode.CATEGORY_HAS_PRODUCT);
        }
        categoryMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    //  Brands
    // ------------------------------------------------------------------
    @Override
    public List<BrandVo> listBrands(boolean visibleOnly) {
        var wrapper = Wrappers.<Brand>lambdaQuery().orderByDesc(Brand::getSort).orderByAsc(Brand::getId);
        if (visibleOnly) {
            wrapper.eq(Brand::getVisible, 1);
        }
        List<Brand> brands = brandMapper.selectList(wrapper);

        Map<Long, Long> counts = new HashMap<>();
        List<com.ait.mall.core.entity.Product> products = productMapper.selectList(
                Wrappers.<com.ait.mall.core.entity.Product>lambdaQuery()
                        .select(com.ait.mall.core.entity.Product::getBrandId));
        for (var product : products) {
            if (product.getBrandId() != null) {
                counts.merge(product.getBrandId(), 1L, Long::sum);
            }
        }

        List<BrandVo> rows = brands.stream().map(b -> {
            BrandVo vo = new BrandVo();
            vo.setId(b.getId());
            vo.setName(b.getName());
            vo.setLogo(b.getLogo());
            vo.setDescription(b.getDescription());
            vo.setSort(b.getSort());
            vo.setVisible(b.getVisible());
            vo.setCreatedAt(b.getCreatedAt());
            vo.setProductCount(counts.getOrDefault(b.getId(), 0L));
            return vo;
        }).collect(Collectors.toList());
        localizer.brands(rows);
        return rows;
    }

    @Override
    public Brand saveBrand(Brand brand) {
        if (brand.getSort() == null) {
            brand.setSort(0);
        }
        if (brand.getVisible() == null) {
            brand.setVisible(1);
        }
        if (brand.getId() == null) {
            brandMapper.insert(brand);
        } else {
            brandMapper.updateById(brand);
        }
        return brand;
    }

    @Override
    public void deleteBrand(Long id) {
        brandMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    //  Banners
    // ------------------------------------------------------------------
    @Override
    public List<Banner> listBanners(String position, boolean enabledOnly) {
        var wrapper = Wrappers.<Banner>lambdaQuery().orderByDesc(Banner::getSort).orderByAsc(Banner::getId);
        if (position != null && !position.isEmpty()) {
            wrapper.eq(Banner::getPosition, position);
        }
        if (enabledOnly) {
            wrapper.eq(Banner::getEnabled, 1);
        }
        List<Banner> banners = bannerMapper.selectList(wrapper);
        SupportedLocale locale = LocaleContext.get();
        if (!locale.isSource() && !banners.isEmpty()) {
            Map<Long, Map<String, String>> overrides = i18nService.textMap("banner",
                    banners.stream().map(Banner::getId).collect(Collectors.toList()), locale);
            for (Banner banner : banners) {
                String title = overrides.getOrDefault(banner.getId(), Map.of()).get("title");
                if (title != null && !title.isEmpty()) {
                    banner.setTitle(title);
                }
            }
        }
        return banners;
    }

    @Override
    public Banner saveBanner(Banner banner) {
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        if (banner.getEnabled() == null) {
            banner.setEnabled(1);
        }
        if (banner.getPosition() == null) {
            banner.setPosition("home");
        }
        if (banner.getLinkType() == null) {
            banner.setLinkType("none");
        }
        if (banner.getId() == null) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
        return banner;
    }

    @Override
    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    //  Notices
    // ------------------------------------------------------------------
    @Override
    public List<Notice> listNotices(boolean enabledOnly) {
        var wrapper = Wrappers.<Notice>lambdaQuery().orderByDesc(Notice::getSort).orderByDesc(Notice::getId);
        if (enabledOnly) {
            wrapper.eq(Notice::getEnabled, 1);
        }
        List<Notice> notices = noticeMapper.selectList(wrapper);
        SupportedLocale locale = LocaleContext.get();
        if (!locale.isSource() && !notices.isEmpty()) {
            Map<Long, Map<String, String>> overrides = i18nService.textMap("notice",
                    notices.stream().map(Notice::getId).collect(Collectors.toList()), locale);
            for (Notice notice : notices) {
                Map<String, String> fields = overrides.getOrDefault(notice.getId(), Map.of());
                if (fields.containsKey("title")) notice.setTitle(fields.get("title"));
                if (fields.containsKey("content")) notice.setContent(fields.get("content"));
            }
        }
        return notices;
    }

    @Override
    public Notice saveNotice(Notice notice) {
        if (notice.getSort() == null) {
            notice.setSort(0);
        }
        if (notice.getEnabled() == null) {
            notice.setEnabled(1);
        }
        if (notice.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        return notice;
    }

    @Override
    public void deleteNotice(Long id) {
        noticeMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    /**
     * Counts products per category, rolling child counts up to their parent so a
     * root category reflects everything beneath it. Products normally live in
     * level-2 categories, so a naive count would show 0 for every root.
     */
    private Map<Long, Long> countProductsByCategory() {
        Map<Long, Long> direct = new HashMap<>();
        List<com.ait.mall.core.entity.Product> products = productMapper.selectList(
                Wrappers.<com.ait.mall.core.entity.Product>lambdaQuery()
                        .select(com.ait.mall.core.entity.Product::getCategoryId));
        for (var product : products) {
            if (product.getCategoryId() != null) {
                direct.merge(product.getCategoryId(), 1L, Long::sum);
            }
        }

        // parentId -> direct count, only for roots
        Map<Long, Long> rolled = new HashMap<>(direct);
        List<Category> all = categoryMapper.selectList(Wrappers.emptyWrapper());
        for (Category category : all) {
            if (category.getParentId() != null && category.getParentId() != 0) {
                rolled.merge(category.getParentId(),
                        direct.getOrDefault(category.getId(), 0L), Long::sum);
            }
        }
        return rolled;
    }

    private CategoryVo toVo(Category category, Map<Long, Long> counts) {
        CategoryVo vo = new CategoryVo();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setBanner(category.getBanner());
        vo.setLevel(category.getLevel());
        vo.setSort(category.getSort());
        vo.setVisible(category.getVisible());
        vo.setProductCount(counts.getOrDefault(category.getId(), 0L));
        return vo;
    }
}
