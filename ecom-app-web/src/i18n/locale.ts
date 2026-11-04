/**
 * Locale plumbing for the desktop storefront.
 *
 * Deliberately dependency free: the app needs a dictionary lookup, an
 * interpolation helper and a reactive language switch, which is a hundred
 * lines rather than the ~14kB of runtime i18next would add. It also keeps the
 * shape explicit, so a missing key is a TypeScript error instead of a string
 * appearing on screen.
 */

export type Locale = 'zh-CN' | 'en' | 'ja';

export const LOCALES: { code: Locale; label: string; short: string }[] = [
  { code: 'zh-CN', label: '简体中文', short: '中' },
  { code: 'en', label: 'English', short: 'EN' },
  { code: 'ja', label: '日本語', short: '日' },
];

export const DEFAULT_LOCALE: Locale = 'zh-CN';

export const STORAGE_KEY = 'ecom_web_locale';

/** True when the language needs the backend translation overlay. */
export function isSourceLocale(locale: Locale): boolean {
  return locale === 'zh-CN';
}

/** Maps our locale to the tag `Intl`/`toLocaleString` expect. */
export function intlLocale(locale: Locale): string {
  return locale === 'zh-CN' ? 'zh-CN' : locale === 'ja' ? 'ja-JP' : 'en-GB';
}

export function resolveInitialLocale(): Locale {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'zh-CN' || stored === 'en' || stored === 'ja') return stored;
  } catch {
    /* storage can be blocked; fall through to the browser preference */
  }
  const candidates = navigator.languages ?? [navigator.language];
  for (const tag of candidates) {
    const primary = String(tag).toLowerCase().split('-')[0];
    if (primary === 'zh') return 'zh-CN';
    if (primary === 'ja') return 'ja';
    if (primary === 'en') return 'en';
  }
  return DEFAULT_LOCALE;
}

/**
 * Locale-aware number/currency formatting.
 *
 * Prices stay in CNY everywhere — this is a Chinese shop and converting would
 * invent an exchange rate — but the symbol placement and grouping follow the
 * visitor's language so "¥1,299.00" does not look like a typo in English.
 */
export function formatPrice(value: number, locale: Locale): string {
  return new Intl.NumberFormat(intlLocale(locale), {
    style: 'currency',
    currency: 'CNY',
    currencyDisplay: 'narrowSymbol',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(value) ? value : 0);
}

export function formatDate(value: string | number | Date, locale: Locale, withTime = true): string {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(intlLocale(locale), {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  }).format(date);
}
