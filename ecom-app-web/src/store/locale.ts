import { create } from 'zustand';
import {
  DEFAULT_LOCALE,
  STORAGE_KEY,
  isSourceLocale,
  resolveInitialLocale,
  type Locale,
} from '@/i18n/locale';
import { DICTIONARIES, type MessageKey } from '@/i18n/messages';

/**
 * Fills `{placeholders}` in a template. Unknown placeholders are left alone so
 * a typo in a translation shows up as literal text rather than a blank space.
 */
export function interpolate(template: string, values?: Record<string, string | number>): string {
  if (!values) return template;
  return template.replace(/\{(\w+)\}/g, (match, key: string) =>
    Object.prototype.hasOwnProperty.call(values, key) ? String(values[key]) : match,
  );
}

interface LocaleState {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  /** Cycles zh → en → ja → zh, used by the header's compact switch. */
  cycle: () => void;
}

function applyDocumentLang(locale: Locale) {
  document.documentElement.setAttribute('lang', locale);
  document.documentElement.setAttribute('data-locale', locale);
}

const initial = resolveInitialLocale();
applyDocumentLang(initial);

export const useLocale = create<LocaleState>((set, get) => ({
  locale: initial,

  setLocale: (locale) => {
    if (locale === get().locale) return;
    applyDocumentLang(locale);
    try {
      localStorage.setItem(STORAGE_KEY, locale);
    } catch {
      /* ignore quota / privacy mode */
    }
    set({ locale });
  },

  cycle: () => {
    const order: Locale[] = ['zh-CN', 'en', 'ja'];
    const next = order[(order.indexOf(get().locale) + 1) % order.length];
    get().setLocale(next);
  },
}));

/** Raw lookup. Prefer {@link useT} inside components so the view re-renders. */
export function translate(
  locale: Locale,
  key: MessageKey,
  values?: Record<string, string | number>,
): string {
  const table = DICTIONARIES[locale] ?? DICTIONARIES[DEFAULT_LOCALE];
  const template = table[key] ?? DICTIONARIES[DEFAULT_LOCALE][key] ?? key;
  return interpolate(template, values);
}

/**
 * The hook every component uses.
 *
 * Returns a `t` bound to the active locale plus that locale, so a component
 * that also needs `Intl` formatting (dates, prices) does not have to read the
 * store twice.
 */
export function useT() {
  const locale = useLocale((s) => s.locale);
  const t = (key: MessageKey, values?: Record<string, string | number>) =>
    translate(locale, key, values);
  return { t, locale, isSource: isSourceLocale(locale) };
}
