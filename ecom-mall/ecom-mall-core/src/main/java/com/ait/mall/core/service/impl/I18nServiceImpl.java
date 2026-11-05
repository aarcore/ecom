package com.ait.mall.core.service.impl;

import com.ait.common.i18n.SupportedLocale;
import com.ait.mall.core.entity.I18nTerm;
import com.ait.mall.core.entity.I18nText;
import com.ait.mall.core.mapper.I18nTermMapper;
import com.ait.mall.core.mapper.I18nTextMapper;
import com.ait.mall.core.service.I18nService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translation overlay backed by two flat tables.
 *
 * <p>The source language short-circuits to empty maps, which keeps the Chinese
 * request path at zero extra queries — it simply renders the columns it
 * already selected.
 */
@Service
@RequiredArgsConstructor
public class I18nServiceImpl implements I18nService {

    private final I18nTextMapper textMapper;
    private final I18nTermMapper termMapper;

    @Override
    public Map<Long, Map<String, String>> textMap(String entity, List<Long> ids,
                                                  SupportedLocale locale) {
        if (locale == null || locale.isSource() || ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<I18nText> rows = textMapper.selectList(Wrappers.<I18nText>lambdaQuery()
                .eq(I18nText::getLang, locale.getCode())
                .eq(I18nText::getEntity, entity)
                .in(I18nText::getEntityId, ids));

        Map<Long, Map<String, String>> result = new HashMap<>();
        for (I18nText row : rows) {
            result.computeIfAbsent(row.getEntityId(), key -> new HashMap<>())
                    .put(row.getField(), row.getValue());
        }
        return result;
    }

    @Override
    public Map<String, String> termMap(SupportedLocale locale) {
        if (locale == null || locale.isSource()) {
            return Collections.emptyMap();
        }
        List<I18nTerm> rows = termMapper.selectList(Wrappers.<I18nTerm>lambdaQuery()
                .eq(I18nTerm::getLang, locale.getCode()));
        Map<String, String> result = new HashMap<>(rows.size() * 2);
        for (I18nTerm row : rows) {
            result.put(row.getSource(), row.getValue());
        }
        return result;
    }
}
