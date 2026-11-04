package com.ait.mall.core.service;

import com.ait.common.i18n.SupportedLocale;
import com.ait.mall.core.entity.I18nText;
import com.ait.mall.core.entity.I18nTerm;

import java.util.List;
import java.util.Map;

/**
 * Reads and writes the translation overlay.
 *
 * <p>Reads are always batched: a product listing needs the translations for
 * every row on the page, so the API takes a list of ids and returns a map
 * keyed by id. Calling this per row would turn one query into N.
 */
public interface I18nService {

    /**
     * Translations for a set of entities, shaped as
     * {@code entityId -> field -> translatedValue}. Fields without a row for
     * the requested language are absent from the inner map so callers can keep
     * the original Chinese value.
     */
    Map<Long, Map<String, String>> textMap(String entity, List<Long> ids, SupportedLocale locale);

    /** Single-field lookup used by the term resolver. */
    Map<String, String> text(String entity, Long id, SupportedLocale locale);

    /** Term map for a closed vocabulary, i.e. {@code source -> translation}. */
    Map<String, String> termMap(SupportedLocale locale);

    /** All rows for one entity and language, for the admin translation editor. */
    List<I18nText> listText(String entity, Long entityId);

    /** Creates or replaces one field translation. */
    void saveText(String entity, Long entityId, String field, String lang, String value);

    /** Removes one field translation, reverting that field to Chinese. */
    void deleteText(String entity, Long entityId, String field, String lang);

    List<I18nTerm> listTerms(String lang);
}
