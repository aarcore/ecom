import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { IconArrowRight } from './icons';
import { Photo } from './Photo';
import { useLocale, useT } from '@/store/locale';

/** Money formatted the same way everywhere: ¥ with two decimals, tabular. */
export function formatMoney(value?: number | string | null): string {
  const n = Number(value ?? 0);
  return `¥${n.toFixed(2)}`;
}

/** Splits a price so the decimals can be typeset smaller than the integer. */
export function Price({
  value,
  size = 16,
  accent = false,
  className = '',
}: {
  value?: number | string | null;
  size?: number;
  accent?: boolean;
  className?: string;
}) {
  const locale = useLocale((s) => s.locale);
  // Intl gives us the right symbol placement and grouping per language, but we
  // still want the decimals set smaller, so split the formatted parts instead
  // of the raw number: "¥1,299.00" / "CN¥1,299.00" both survive the split.
  const parts = new Intl.NumberFormat(locale === 'zh-CN' ? 'zh-CN' : locale === 'ja' ? 'ja-JP' : 'en-GB', {
    style: 'currency',
    currency: 'CNY',
    currencyDisplay: 'narrowSymbol',
    minimumFractionDigits: 2,
  }).formatToParts(Number(value ?? 0));
  const currency = parts.find((part) => part.type === 'currency')?.value ?? '¥';
  const major = parts.filter((part) => part.type === 'integer' || part.type === 'group')
    .map((part) => part.value).join('');
  const dec = parts.find((part) => part.type === 'fraction')?.value ?? '00';

  return (
    <span
      className={`price ${accent ? 'is-accent' : ''} ${className}`}
      style={{ fontSize: size }}
    >
      <span className="sym">{currency}</span>
      <span>{major}</span>
      <span style={{ fontSize: size * 0.72, fontWeight: 500, opacity: 0.8 }}>.{dec}</span>
    </span>
  );
}

export function Empty({
  glyph,
  title,
  description,
  action,
}: {
  glyph?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty">
      {glyph && <div className="glyph">{glyph}</div>}
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  );
}

export function Toasts({ items }: { items: { id: number; text: string; kind: string }[] }) {
  return (
    <div className="toasts" role="status" aria-live="polite">
      {items.map((toast) => (
        <div key={toast.id} className={`toast ${toast.kind}`}>
          {toast.text}
        </div>
      ))}
    </div>
  );
}

export interface ProductCardData {
  id: number;
  name: string;
  cover?: string;
  price: number;
  originalPrice?: number;
  isNew?: number;
  isHot?: number;
  sales?: number;
  subtitle?: string;
}

/**
 * The primary catalogue unit. Image-forward with a restrained flag; the whole
 * card is one button so keyboard users get a single tab stop per product.
 */
export function ProductCard({ product }: { product: ProductCardData }) {
  const navigate = useNavigate();
  const { t } = useT();
  const hasDiscount =
    product.originalPrice != null && Number(product.originalPrice) > Number(product.price);

  return (
    <button
      className="pcard"
      onClick={() => navigate(`/product/${product.id}`)}
      aria-label={product.name}
    >
      <div className="thumb">
        {product.cover ? (
          <Photo src={product.cover} loading="lazy" />
        ) : (
          <div className="skel" style={{ width: '100%', height: '100%' }} />
        )}
        {product.isNew === 1 && <span className="tag volt flag">{t('badge.new')}</span>}
        {product.isNew !== 1 && product.isHot === 1 && (
          <span className="tag solid flag">{t('badge.hot')}</span>
        )}
      </div>
      <div className="body">
        <div className="name">{product.name}</div>
        <div className="meta">
          <Price value={product.price} size={20} accent />
          {hasDiscount && (
            <span className="price-was">¥{Number(product.originalPrice).toFixed(0)}</span>
          )}
        </div>
        {product.sales ? (
          <div className="sales">{t('badge.sold', { count: product.sales })}</div>
        ) : null}
      </div>
    </button>
  );
}

/** Section header with an optional outlined index numeral and a trailing link. */
export function SectionHead({
  title,
  aside,
  action,
  numeral,
}: {
  title: string;
  aside?: string;
  action?: { label: string; to: string };
  numeral?: string;
}) {
  const navigate = useNavigate();
  return (
    <div className="section-head">
      <div className="row" style={{ alignItems: 'flex-end', gap: 24 }}>
        {numeral && <span className="numeral">{numeral}</span>}
        <div>
          <h2>{title}</h2>
          {aside && <div className="aside" style={{ marginTop: 10 }}>{aside}</div>}
        </div>
      </div>
      {action && (
        <div className="aside-right">
          <button className="link-arrow" onClick={() => navigate(action.to)}>
            {action.label}
            <IconArrowRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
}
