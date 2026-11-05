package com.ait.mall.core.service;

import com.ait.common.i18n.SupportedLocale;

import java.util.List;
import java.util.Map;

/**
 * Reads the translation overlay.
 *
 * <p>Reads are always batched: a product listing needs the translations for
 * every row on the page, so the API takes a list of ids and returns a map
 * keyed by id. Calling this per row would turn one query into N.
 *
 * <p>There is deliberately no write API here. The overlay is generated from
 * {@code scripts/gen_i18n_sql.py} into a Flyway migration, so translations are
 * versioned with the schema rather than edited at runtime; a console editor was
 * never built, and carrying its half-implemented methods only invited misuse.
 */
public interface I18nService {

    /**
     * Translations for a set of entities, shaped as
     * {@code entityId -> field -> translatedValue}. Fields without a row for
     * the requested language are absent from the inner map so callers can keep
     * the original Chinese value.
     */
    Map<Long, Map<String, String>> textMap(String entity, List<Long> ids, SupportedLocale locale);

    /** Term map for a closed vocabulary, i.e. {@code source -> translation}. */
    Map<String, String> termMap(SupportedLocale locale);
}
