import { useEffect, useRef, useState } from 'react';
import { LOCALES, type Locale } from '@/i18n/locale';
import { useLocale } from '@/store/locale';
import { useT } from '@/store/locale';
import { IconGlobe } from './icons';

/**
 * Language switch.
 *
 * Three languages is few enough that a dropdown would be more chrome than it is
 * worth, but the labels are long enough that three side-by-side buttons crowd
 * the header. So the trigger shows the active language's short tag and opens a
 * small menu with the full names — the same pattern as the account menu next to
 * it, which keeps the header's interaction language consistent.
 */
export function LocaleSwitch({ compact = false }: { compact?: boolean }) {
  const { locale, setLocale } = useLocale();
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onClick = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const current = LOCALES.find((item) => item.code === locale) ?? LOCALES[0];

  return (
    <div className="locale-switch" ref={ref}>
      <button
        type="button"
        className="locale-trigger"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={t('nav.language')}
        title={t('nav.language')}
      >
        <IconGlobe />
        <span className={compact ? 'locale-tag' : 'locale-tag hide-mobile'}>{current.short}</span>
      </button>

      {open && (
        <div className="locale-menu" role="menu">
          {LOCALES.map((item) => (
            <button
              key={item.code}
              role="menuitemradio"
              aria-checked={item.code === locale}
              className={item.code === locale ? 'on' : ''}
              onClick={() => {
                setLocale(item.code as Locale);
                setOpen(false);
              }}
            >
              <span className="label">{item.label}</span>
              <span className="tag">{item.short}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
