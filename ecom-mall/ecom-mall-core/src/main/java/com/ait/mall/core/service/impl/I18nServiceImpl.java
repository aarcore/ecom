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

import java.time.LocalDateTime;
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
    public Map<String, String> text(String entity, Long id, SupportedLocale locale) {
        if (locale == null || locale.isSource() || id == null) {
            return Collections.emptyMap();
        }
        List<I18nText> rows = textMapper.selectList(Wrappers.<I18nText>lambdaQuery()
                .eq(I18nText::getLang, locale.getCode())
                .eq(I18nText::getEntity, entity)
                .eq(I18nText::getEntityId, id));
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> fields = new HashMap<>();
        for (I18nText row : rows) {
            fields.put(row.getField(), row.getValue());
        }
        return fields;
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

    @Override
    public List<I18nText> listText(String entity, Long entityId) {
        return textMapper.selectList(Wrappers.<I18nText>lambdaQuery()
                .eq(I18nText::getEntity, entity)
                .eq(I18nText::getEntityId, entityId)
                .orderByAsc(I18nText::getField)
                .orderByAsc(I18nText::getLang));
    }

    @Override
    public void saveText(String entity, Long entityId, String field, String lang, String value) {
        if (value == null || value.trim().isEmpty()) {
            // an empty value means "revert to Chinese", which is the same thing
            // as having no overlay row at all
            deleteText(entity, entityId, field, lang);
            return;
        }
        I18nText existing = textMapper.selectOne(Wrappers.<I18nText>lambdaQuery()
                .eq(I18nText::getEntity, entity)
                .eq(I18nText::getEntityId, entityId)
                .eq(I18nText::getField, field)
                .eq(I18nText::getLang, lang));

        if (existing == null) {
            I18nText row = new I18nText();
            row.setEntity(entity);
            row.setEntityId(entityId);
            row.setField(field);
            row.setLang(lang);
            row.setValue(value.trim());
            row.setCreatedAt(LocalDateTime.now());
            row.setUpdatedAt(LocalDateTime.now());
            textMapper.insert(row);
        } else {
            // updateById with a full entity would also touch created_at
            I18nText patch = new I18nText();
            patch.setId(existing.getId());
            patch.setValue(value.trim());
            patch.setUpdatedAt(LocalDateTime.now());
            textMapper.updateById(patch);
        }
    }

    @Override
    public void deleteText(String entity, Long entityId, String field, String lang) {
        textMapper.delete(Wrappers.<I18nText>lambdaQuery()
                .eq(I18nText::getEntity, entity)
                .eq(I18nText::getEntityId, entityId)
                .eq(I18nText::getField, field)
                .eq(I18nText::getLang, lang));
    }

    @Override
    public List<I18nTerm> listTerms(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            return termMapper.selectList(Wrappers.<I18nTerm>lambdaQuery()
                    .orderByAsc(I18nTerm::getSource)
                    .orderByAsc(I18nTerm::getLang));
        }
        return termMapper.selectList(Wrappers.<I18nTerm>lambdaQuery()
                .eq(I18nTerm::getLang, lang)
                .orderByAsc(I18nTerm::getSource));
    }
}
