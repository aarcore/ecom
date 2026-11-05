package com.ait.common.i18n;

/**
 * Holds the locale negotiated for the current request so services can resolve
 * translated content without threading a parameter through every signature.
 *
 * <p>Populated by {@link LocaleFilter} and cleared in a finally block, mirroring
 * {@code LoginUserContext}. Falls back to the source locale when unset, so a
 * scheduled job or a unit test behaves exactly like a Chinese browser.
 */
public final class LocaleContext {

    private static final ThreadLocal<SupportedLocale> CURRENT = new ThreadLocal<>();

    private LocaleContext() {
    }

    public static SupportedLocale get() {
        SupportedLocale locale = CURRENT.get();
        return locale == null ? SupportedLocale.ZH_CN : locale;
    }

    public static void set(SupportedLocale locale) {
        if (locale == null || locale.isSource()) {
            CURRENT.remove();
        } else {
            CURRENT.set(locale);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }
}
