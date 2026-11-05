package com.ait.common.i18n;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The languages the storefront ships with.
 *
 * <p>{@code zh-CN} is the source language: every database row, every default
 * label and every enum in this project is written in Chinese, and the other
 * entries are overlays on top of it. That means a missing translation simply
 * falls back to the original text instead of showing a key name.
 *
 * <p>Keep {@code code} stable — it is what the browser sends back in
 * {@code Accept-Language}, what the frontend stores in localStorage and what
 * the {@code lang} column of {@code shop_i18n_text} holds. The Japanese and
 * English codes are the bare language tags so that {@code en-GB} and
 * {@code ja-JP} resolve to the same overlay.
 */
@Getter
public enum SupportedLocale {

    ZH_CN("zh-CN", "简体中文", true),
    EN("en", "English", false),
    JA("ja", "日本語", false);

    /** Tag used on the wire and in the database. */
    private final String code;

    /** Name of the language in itself, for the picker. */
    private final String label;

    /** True when the database columns already hold this language. */
    private final boolean source;

    SupportedLocale(String code, String label, boolean source) {
        this.code = code;
        this.label = label;
        this.source = source;
    }

    public boolean isSource() {
        return source;
    }

    public static Optional<SupportedLocale> byCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        String wanted = code.trim();
        return Arrays.stream(values())
                .filter(locale -> locale.code.equalsIgnoreCase(wanted))
                .findFirst();
    }

    /**
     * Resolves a single language tag such as {@code en-GB} or {@code zh-Hans-CN}
     * to one of the supported locales, matching on the primary subtag.
     */
    public static Optional<SupportedLocale> byLanguage(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return Optional.empty();
        }
        String primary = tag.trim().toLowerCase(Locale.ROOT).split("[-_]")[0];
        for (SupportedLocale locale : values()) {
            String base = locale.code.split("-")[0].toLowerCase(Locale.ROOT);
            if (base.equals(primary)) {
                return Optional.of(locale);
            }
        }
        return Optional.empty();
    }

    public static List<SupportedLocale> all() {
        return Arrays.asList(values());
    }
}
