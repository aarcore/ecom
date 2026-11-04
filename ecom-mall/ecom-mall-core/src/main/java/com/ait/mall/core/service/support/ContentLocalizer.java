package com.ait.mall.core.service.support;

import com.ait.common.i18n.LocaleContext;
import com.ait.common.i18n.SupportedLocale;
import com.ait.mall.core.service.I18nService;
import com.ait.mall.core.vo.BrandVo;
import com.ait.mall.core.vo.CategoryVo;
import com.ait.mall.core.vo.ProductVo;
import com.ait.mall.core.vo.SpecVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Applies the translation overlay to catalogue view objects.
 *
 * <p>Everything here is batch oriented and no-ops in Chinese, so the default
 * request path pays nothing for the feature.
 *
 * <p>One subtlety worth stating: spec <em>values</em> are translated on both
 * the product's spec matrix and on each SKU's comma-joined {@code specValues}.
 * Those two strings are compared on the client to decide which SKU is selected,
 * so they have to go through the same term map or the buy box would stop
 * resolving a selection.
 */
@Component
@RequiredArgsConstructor
public class ContentLocalizer {

    private static final String ENTITY_PRODUCT = "product";
    private static final String ENTITY_CATEGORY = "category";
    private static final String ENTITY_BRAND = "brand";

    private final I18nService i18nService;

    /**
     * Translates a page of products in place.
     *
     * @param withDetail set when the rows carry detail text and spec matrices
     *                   (product detail, not listings)
     */
    public void products(List<ProductVo> products, boolean withDetail, SupportedLocale locale) {
        if (locale == null || locale.isSource() || products == null || products.isEmpty()) {
            return;
        }
        List<Long> ids = products.stream().map(ProductVo::getId).collect(Collectors.toList());
        Map<Long, Map<String, String>> overrides = i18nService.textMap(ENTITY_PRODUCT, ids, locale);
        Map<String, String> terms = i18nService.termMap(locale);

        // category and brand labels live on the same VO, so translate them in
        // the same pass instead of issuing two more queries per product
        Map<Long, String> categoryNames = translateNames(ENTITY_CATEGORY,
                products.stream().map(ProductVo::getCategoryId).filter(java.util.Objects::nonNull)
                        .distinct().collect(Collectors.toList()), locale);
        Map<Long, String> brandNames = translateNames(ENTITY_BRAND,
                products.stream().map(ProductVo::getBrandId).filter(java.util.Objects::nonNull)
                        .distinct().collect(Collectors.toList()), locale);

        for (ProductVo product : products) {
            Map<String, String> fields = overrides.getOrDefault(product.getId(), Collections.emptyMap());
            product.setName(pick(fields, "name", product.getName()));
            product.setSubtitle(pick(fields, "subtitle", product.getSubtitle()));
            if (withDetail) {
                product.setDetail(pick(fields, "detail", product.getDetail()));
                translateSpecs(product.getSpecList(), terms);
                product.setUnit(term(terms, product.getUnit()));
            }
            product.setCategoryName(pick(categoryNames, product.getCategoryId(), product.getCategoryName()));
            product.setBrandName(pick(brandNames, product.getBrandId(), product.getBrandName()));

            if (product.getSkuList() != null) {
                product.getSkuList().forEach(sku ->
                        sku.setSpecValues(termList(terms, sku.getSpecValues())));
            }
        }
    }

    public void categories(List<CategoryVo> categories, SupportedLocale locale) {
        if (locale == null || locale.isSource() || categories == null || categories.isEmpty()) {
            return;
        }
        // walk the tree so children are translated too, without a second query
        List<Long> ids = new java.util.ArrayList<>();
        collectIds(categories, ids);
        Map<Long, Map<String, String>> overrides = i18nService.textMap(ENTITY_CATEGORY, ids, locale);
        for (CategoryVo category : categories) {
            applyCategory(category, overrides);
        }
    }

    public void brands(List<BrandVo> brands, SupportedLocale locale) {
        if (locale == null || locale.isSource() || brands == null || brands.isEmpty()) {
            return;
        }
        List<Long> ids = brands.stream().map(BrandVo::getId).collect(Collectors.toList());
        Map<Long, Map<String, String>> overrides = i18nService.textMap(ENTITY_BRAND, ids, locale);
        for (BrandVo brand : brands) {
            Map<String, String> fields = overrides.getOrDefault(brand.getId(), Collections.emptyMap());
            brand.setDescription(pick(fields, "description", brand.getDescription()));
        }
    }

    /** Applies the overlay to whatever locale {@link LocaleContext} negotiated. */
    public void products(List<ProductVo> products, boolean withDetail) {
        products(products, withDetail, LocaleContext.get());
    }

    public void categories(List<CategoryVo> categories) {
        categories(categories, LocaleContext.get());
    }

    public void brands(List<BrandVo> brands) {
        brands(brands, LocaleContext.get());
    }

    /** Translates a single comma-joined spec value string ("M,哑光黑"). */
    public String specValues(String specValues) {
        SupportedLocale locale = LocaleContext.get();
        if (locale.isSource() || specValues == null || specValues.isEmpty()) {
            return specValues;
        }
        return termList(i18nService.termMap(locale), specValues);
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private void collectIds(List<CategoryVo> nodes, List<Long> sink) {
        for (CategoryVo node : nodes) {
            sink.add(node.getId());
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                collectIds(node.getChildren(), sink);
            }
        }
    }

    private void applyCategory(CategoryVo category, Map<Long, Map<String, String>> overrides) {
        Map<String, String> fields = overrides.getOrDefault(category.getId(), Collections.emptyMap());
        category.setName(pick(fields, "name", category.getName()));
        if (category.getChildren() != null) {
            for (CategoryVo child : category.getChildren()) {
                applyCategory(child, overrides);
            }
        }
    }

    private Map<Long, String> translateNames(String entity, List<Long> ids, SupportedLocale locale) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Map<String, String>> overrides = i18nService.textMap(entity, ids, locale);
        Map<Long, String> result = new HashMap<>(overrides.size());
        overrides.forEach((id, fields) -> {
            String name = fields.get("name");
            if (name != null && !name.isEmpty()) {
                result.put(id, name);
            }
        });
        return result;
    }

    private void translateSpecs(List<SpecVo> specs, Map<String, String> terms) {
        if (specs == null || terms.isEmpty()) {
            return;
        }
        for (SpecVo spec : specs) {
            spec.setName(term(terms, spec.getName()));
            if (spec.getValues() != null) {
                spec.setValues(spec.getValues().stream()
                        .map(value -> term(terms, value))
                        .collect(Collectors.toList()));
            }
        }
    }

    private String termList(Map<String, String> terms, String joined) {
        if (joined == null || joined.isEmpty() || terms.isEmpty()) {
            return joined;
        }
        // spec values never contain a comma in this catalogue, so a plain split
        // mirrors how they were joined when the SKU was created
        String[] parts = joined.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = term(terms, parts[i]);
        }
        return String.join(",", parts);
    }

    private String term(Map<String, String> terms, String source) {
        if (source == null) {
            return null;
        }
        String translated = terms.get(source);
        return translated == null || translated.isEmpty() ? source : translated;
    }

    private String pick(Map<String, String> fields, String field, String fallback) {
        String value = fields.get(field);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private String pick(Map<Long, String> names, Long id, String fallback) {
        if (id == null) {
            return fallback;
        }
        String value = names.get(id);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
