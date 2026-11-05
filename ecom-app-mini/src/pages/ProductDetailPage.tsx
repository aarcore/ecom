import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { Price, ProductCard } from '@/components/ui';
import { useAuth } from '@/store/auth';
import type { ProductDetail, Sku } from '@/types';

export default function ProductDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = useAuth((s) => s.token);
  const refreshCart = useAuth((s) => s.refreshCart);

  const [data, setData] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [galleryIndex, setGalleryIndex] = useState(0);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [mode, setMode] = useState<'cart' | 'buy'>('cart');

  /** Selected value per spec dimension, e.g. { 颜色: '幻夜黑', 存储: '8+128G' } */
  const [selection, setSelection] = useState<Record<string, string>>({});
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    setLoading(true);
    api
      .get<ProductDetail>(`/app/products/${id}`)
      .then((result) => {
        setData(result);
        // preselect the first in-stock SKU so the CTA is actionable immediately
        const firstSku = result.product.skuList?.find((s) => s.stock > 0) ?? result.product.skuList?.[0];
        if (firstSku && result.product.specList?.length) {
          const values = firstSku.specValues?.split(',') ?? [];
          const preset: Record<string, string> = {};
          result.product.specList.forEach((spec, i) => {
            if (values[i]) preset[spec.name] = values[i];
          });
          setSelection(preset);
        }
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  const product = data?.product;

  /** Resolves the SKU matching the current spec selection. */
  const matchedSku: Sku | null = useMemo(() => {
    if (!product?.skuList?.length) return null;
    if (!product.specList?.length) return product.skuList[0];

    const wanted = product.specList
      .map((spec) => selection[spec.name])
      .filter(Boolean)
      .join(',');
    if (!wanted || product.specList.some((spec) => !selection[spec.name])) return null;
    return product.skuList.find((sku) => sku.specValues === wanted) ?? null;
  }, [product, selection]);

  const maxQuantity = matchedSku?.stock ?? product?.stock ?? 0;

  /** A spec value is selectable when some SKU with that value has stock. */
  const isValueAvailable = (specName: string, value: string) => {
    if (!product?.skuList?.length || !product.specList) return true;
    const index = product.specList.findIndex((s) => s.name === specName);
    return product.skuList.some((sku) => {
      const parts = sku.specValues?.split(',') ?? [];
      return parts[index] === value && sku.stock > 0;
    });
  };

  const addToCart = async () => {
    if (!token) {
      navigate('/login', { state: { from: `/product/${id}` } });
      return;
    }
    if (!matchedSku) {
      setMode('cart');
      setSheetOpen(true);
      return;
    }
    const result = await attempt(
      () => api.post('/app/cart', { skuId: matchedSku.id, quantity }),
      '已加入购物车',
    );
    if (result) {
      refreshCart();
      setSheetOpen(false);
    }
  };

  const buyNow = () => {
    if (!token) {
      navigate('/login', { state: { from: `/product/${id}` } });
      return;
    }
    if (!matchedSku) {
      setMode('buy');
      setSheetOpen(true);
      return;
    }
    navigate('/checkout', {
      state: {
        source: 'buy_now',
        items: [{ skuId: matchedSku.id, quantity }],
      },
    });
  };

  if (loading) {
    return (
      <div className="page">
        <div className="skel" style={{ height: 330, borderRadius: 16 }} />
        <div className="skel" style={{ height: 22, marginTop: 16, width: '70%' }} />
        <div className="skel" style={{ height: 40, marginTop: 10, width: '40%' }} />
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="page">
        <div className="empty">
          <div className="glyph">😢</div>
          <p>{error ?? '商品不存在'}</p>
          <button className="btn btn-outline btn-sm" onClick={() => navigate('/')}>
            返回首页
          </button>
        </div>
      </div>
    );
  }

  const images = product.imageList?.length ? product.imageList : [product.cover ?? ''];

  return (
    <div style={{ paddingBottom: 78 }}>
      <div className="topbar" style={{ position: 'absolute', left: 0, right: 0, background: 'transparent', border: 'none' }}>
        <button
          className="icon-btn"
          style={{ background: 'rgba(255,255,255,0.9)' }}
          onClick={() => navigate(-1)}
        >
          ‹
        </button>
        <div style={{ flex: 1 }} />
        <button
          className="icon-btn"
          style={{ background: 'rgba(255,255,255,0.9)' }}
          onClick={() => navigate('/cart')}
        >
          🛒
        </button>
      </div>

      {/* gallery */}
      <div style={{ position: 'relative', background: '#fff' }}>
        <div
          style={{ display: 'flex', overflowX: 'auto', scrollSnapType: 'x mandatory', scrollbarWidth: 'none' }}
          onScroll={(event) => {
            const el = event.currentTarget;
            setGalleryIndex(Math.round(el.scrollLeft / el.clientWidth));
          }}
        >
          {images.map((src, i) => (
            <div key={`${src}-${i}`} style={{ minWidth: '100%', scrollSnapAlign: 'start', aspectRatio: '1 / 1' }}>
              <img src={src} alt={product.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </div>
          ))}
        </div>
        {images.length > 1 && (
          <div className="hero-dots" style={{ bottom: 14 }}>
            {images.map((src, i) => (
              <i key={`dot-${i}`} className={i === galleryIndex ? 'on' : ''} />
            ))}
          </div>
        )}
      </div>

      {/* price block */}
      <div style={{ background: '#fff', padding: '14px 16px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
          <Price value={matchedSku?.price ?? product.price} size="lg" />
          {product.originalPrice && Number(product.originalPrice) > Number(product.price) && (
            <span className="price-old" style={{ fontSize: 12 }}>
              ¥{Number(product.originalPrice).toFixed(2)}
            </span>
          )}
          {product.isHot === 1 && <span className="pill" style={{ position: 'static' }}>热销</span>}
        </div>

        <h1 style={{ fontSize: 17, lineHeight: 1.45, margin: '10px 0 6px', fontWeight: 600 }}>
          {product.name}
        </h1>
        {product.subtitle && <p style={{ margin: 0, color: 'var(--ink-2)', fontSize: 13 }}>{product.subtitle}</p>}

        <div
          style={{
            display: 'flex',
            gap: 18,
            marginTop: 12,
            paddingTop: 12,
            borderTop: '1px solid var(--line)',
            fontSize: 12,
            color: 'var(--ink-3)',
          }}
        >
          <span>已售 {product.sales}</span>
          <span>库存 {matchedSku?.stock ?? product.stock}</span>
          {product.brandName && <span>{product.brandName}</span>}
        </div>
      </div>

      {/* spec entry */}
      <div className="list-card" style={{ marginTop: 10, borderRadius: 0 }}>
        <div className="row" onClick={() => setSheetOpen(true)}>
          <span className="label">选择</span>
          <span className="value">
            {product.specList?.length
              ? product.specList.map((spec) => selection[spec.name]).filter(Boolean).join(' · ') || '请选择规格'
              : '默认规格'}
            {quantity > 1 ? ` · ${quantity} 件` : ''}
          </span>
          <span className="arrow">›</span>
        </div>
        <div className="row">
          <span className="label">服务</span>
          <span className="value" style={{ color: 'var(--ink-2)' }}>
            7 天无理由 · 官方授权 · 整车包邮
          </span>
        </div>
      </div>

      {/* detail */}
      {product.detail && (
        <div style={{ background: '#fff', marginTop: 10, padding: '16px' }}>
          <h3 style={{ fontSize: 15, margin: '0 0 10px' }}>商品详情</h3>
          <p style={{ margin: 0, fontSize: 13.5, lineHeight: 1.85, color: '#3b414c', whiteSpace: 'pre-wrap' }}>
            {product.detail}
          </p>
        </div>
      )}

      {/* related */}
      {data?.related?.length ? (
        <div style={{ background: '#fff', marginTop: 10, padding: '16px 14px' }}>
          <h3 style={{ fontSize: 15, margin: '0 0 12px' }}>相关推荐</h3>
          <div className="product-grid">
            {data.related.slice(0, 4).map((item) => (
              <ProductCard
                key={item.id}
                product={item}
                onClick={() => {
                  navigate(`/product/${item.id}`);
                  window.scrollTo(0, 0);
                }}
              />
            ))}
          </div>
        </div>
      ) : null}

      {/* sticky action bar */}
      <div className="bottom-bar" style={{ position: 'fixed', left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: 480 }}>
        <button className="btn btn-ghost btn-sm" style={{ flexDirection: 'column', height: 44, padding: 0 }} onClick={() => navigate('/mine')}>
          <span style={{ fontSize: 16 }}>👤</span>
          <span style={{ fontSize: 10 }}>我的</span>
        </button>
        <button className="btn btn-ghost btn-sm" style={{ flexDirection: 'column', height: 44, padding: 0 }} onClick={() => navigate('/cart')}>
          <span style={{ fontSize: 16 }}>🛒</span>
          <span style={{ fontSize: 10 }}>购物车</span>
        </button>
        <button className="btn btn-outline" style={{ flex: 1 }} onClick={addToCart} disabled={maxQuantity === 0}>
          加入购物车
        </button>
        <button className="btn btn-primary" style={{ flex: 1 }} onClick={buyNow} disabled={maxQuantity === 0}>
          {maxQuantity === 0 ? '已售罄' : '立即购买'}
        </button>
      </div>

      {/* spec sheet */}
      {sheetOpen && (
        <div className="sheet-mask" onClick={() => setSheetOpen(false)}>
          <div className="sheet" onClick={(event) => event.stopPropagation()}>
            <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
              <img
                src={matchedSku?.image || product.cover}
                alt=""
                style={{ width: 84, height: 84, borderRadius: 12, objectFit: 'cover', background: '#f2f4f8' }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <Price value={matchedSku?.price ?? product.price} size="md" />
                <div className="muted" style={{ marginTop: 4 }}>
                  库存 {matchedSku?.stock ?? product.stock} 件
                </div>
                <div className="muted" style={{ marginTop: 2 }}>
                  {product.specList?.map((s) => selection[s.name]).filter(Boolean).join(' · ') || '请选择规格'}
                </div>
              </div>
            </div>

            {product.specList?.map((spec) => (
              <div key={spec.name} style={{ marginBottom: 16 }}>
                <div style={{ fontSize: 13, color: 'var(--ink-2)', marginBottom: 8 }}>{spec.name}</div>
                <div className="chips">
                  {spec.values.map((value) => {
                    const on = selection[spec.name] === value;
                    const available = isValueAvailable(spec.name, value);
                    return (
                      <button
                        key={value}
                        className={`chip ${on ? 'on' : ''} ${available ? '' : 'off'}`}
                        disabled={!available}
                        onClick={() => setSelection({ ...selection, [spec.name]: value })}
                      >
                        {value}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}

            <div className="row-between" style={{ marginBottom: 18 }}>
              <span style={{ fontSize: 13, color: 'var(--ink-2)' }}>购买数量</span>
              <div className="stepper">
                <button onClick={() => setQuantity(Math.max(1, quantity - 1))}>−</button>
                <input
                  value={quantity}
                  readOnly
                  inputMode="numeric"
                  onChange={(event) => {
                    const value = Number(event.target.value.replace(/\D/g, ''));
                    if (value >= 1) setQuantity(Math.min(value, maxQuantity || 1));
                  }}
                />
                <button onClick={() => setQuantity(Math.min(maxQuantity || 1, quantity + 1))}>+</button>
              </div>
            </div>

            <button
              className="btn btn-primary btn-block"
              disabled={!matchedSku || maxQuantity === 0}
              onClick={() => {
                if (mode === 'buy') buyNow();
                else addToCart();
              }}
            >
              {!matchedSku ? '请选择完整规格' : maxQuantity === 0 ? '已售罄' : mode === 'buy' ? '立即购买' : '加入购物车'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
