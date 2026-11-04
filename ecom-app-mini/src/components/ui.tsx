import type { ReactNode } from 'react';

/** Formats a money value as integer + 2 decimal parts for styled price output. */
export function Price({
  value,
  size = 'md',
}: {
  value?: number | string | null;
  size?: 'sm' | 'md' | 'lg';
}) {
  const num = Number(value ?? 0);
  const [int, dec] = num.toFixed(2).split('.');
  const scale = size === 'lg' ? { sym: 14, int: 26, dec: 14 } : size === 'sm' ? { sym: 10, int: 14, dec: 10 } : { sym: 11, int: 18, dec: 11 };
  return (
    <span className="price">
      <span className="sym" style={{ fontSize: scale.sym }}>
        ¥
      </span>
      <span className="int" style={{ fontSize: scale.int }}>
        {int}
      </span>
      <span className="dec" style={{ fontSize: scale.dec }}>
        .{dec}
      </span>
    </span>
  );
}

export function ProductCard({
  product,
  onClick,
}: {
  product: {
    id: number;
    name: string;
    cover?: string;
    price: number;
    originalPrice?: number;
    isNew?: number;
    isHot?: number;
    sales?: number;
  };
  onClick: () => void;
}) {
  return (
    <div className="pcard" onClick={onClick}>
      <div className="thumb">
        {product.cover ? (
          <img src={product.cover} alt={product.name} loading="lazy" />
        ) : (
          <div className="skel" style={{ width: '100%', height: '100%' }} />
        )}
        {product.isNew === 1 && <span className="pill">新品</span>}
        {product.isNew !== 1 && product.isHot === 1 && <span className="pill gold">热销</span>}
      </div>
      <div className="body">
        <div className="name">{product.name}</div>
        {product.sales ? <div className="sub">已售 {product.sales}</div> : null}
        <div className="foot">
          <Price value={product.price} />
          {product.originalPrice && Number(product.originalPrice) > Number(product.price) ? (
            <span className="price-old">¥{Number(product.originalPrice).toFixed(0)}</span>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export function Empty({
  glyph = '🛒',
  text,
  action,
}: {
  glyph?: string;
  text: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty">
      <div className="glyph">{glyph}</div>
      <p>{text}</p>
      {action}
    </div>
  );
}

export function Toasts({ items }: { items: { id: number; text: string; kind: string }[] }) {
  return (
    <div className="toasts">
      {items.map((toast) => (
        <div key={toast.id} className={`toast ${toast.kind}`}>
          {toast.text}
        </div>
      ))}
    </div>
  );
}
