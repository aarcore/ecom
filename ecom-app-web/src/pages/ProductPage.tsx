import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { IconArrowRight, IconRefresh, IconShield, IconTruck } from '@/components/icons';
import { Photo } from '@/components/Photo';
import { Empty, Price, ProductCard, SectionHead } from '@/components/ui';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { ProductDetail, Sku } from '@/types';

export default function ProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { token, refreshCart } = useAuth();
  const { t } = useT();
  const locale = useLocale((s) => s.locale);

  const [data, setData] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeImage, setActiveImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [selection, setSelection] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setLoading(true);
    setActiveImage(0);
    setQuantity(1);
    api
      .get<ProductDetail>(`/app/products/${id}`)
      .then((result) => {
        setData(result);
        const firstSku =
          result.product.skuList?.find((s) => s.stock > 0) ?? result.product.skuList?.[0];
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
    window.scrollTo(0, 0);
  }, [id, locale]);

  const product = data?.product;

  const matchedSku: Sku | null = useMemo(() => {
    if (!product?.skuList?.length) return null;
    if (!product.specList?.length) return product.skuList[0];
    if (product.specList.some((spec) => !selection[spec.name])) return null;
    const wanted = product.specList.map((spec) => selection[spec.name]).join(',');
    return product.skuList.find((sku) => sku.specValues === wanted) ?? null;
  }, [product, selection]);

  const maxQuantity = matchedSku?.stock ?? product?.stock ?? 0;
  const images = product?.imageList?.length ? product.imageList : [product?.cover ?? ''];

  const valueAvailable = (specName: string, value: string) => {
    if (!product?.skuList?.length || !product.specList) return true;
    const index = product.specList.findIndex((s) => s.name === specName);
    return product.skuList.some((sku) => {
      const parts = sku.specValues?.split(',') ?? [];
      return parts[index] === value && sku.stock > 0;
    });
  };

  const requireSpec = () => {
    if (!matchedSku) {
      attempt(() => Promise.reject(new Error(t('product.chooseSpecFirst'))));
      return false;
    }
    return true;
  };

  const addToCart = async () => {
    if (!token) return navigate('/login', { state: { from: `/product/${id}` } });
    if (!requireSpec()) return;
    setBusy(true);
    const ok = await attempt(
      () => api.post('/app/cart', { skuId: matchedSku!.id, quantity }),
      t('product.addedToCart'),
    );
    setBusy(false);
    if (ok) refreshCart();
  };

  const buyNow = () => {
    if (!token) return navigate('/login', { state: { from: `/product/${id}` } });
    if (!requireSpec()) return;
    navigate('/checkout', {
      state: { source: 'buy_now', items: [{ skuId: matchedSku!.id, quantity }] },
    });
  };

  if (loading) {
    return (
      <div className="shell">
        <div className="pdp">
          <div className="skel" style={{ aspectRatio: '4 / 5' }} />
          <div>
            <div className="skel" style={{ height: 34, marginBottom: 14 }} />
            <div className="skel" style={{ height: 16, width: '70%', marginBottom: 30 }} />
            <div className="skel" style={{ height: 62, marginBottom: 24 }} />
            <div className="skel" style={{ height: 44, marginBottom: 10 }} />
            <div className="skel" style={{ height: 44 }} />
          </div>
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="shell">
        <Empty
          glyph="◎"
          title={t('product.notFound')}
          description={error ?? t('product.notFoundHint')}
          action={
            <button className="btn btn-primary" onClick={() => navigate('/shop')}>
              {t('product.backToList')}
            </button>
          }
        />
      </div>
    );
  }

  return (
    <div className="shell">
      <div className="pdp">
        {/* ------------------------------------------------------- gallery */}
        <div className="gallery">
          <div className="gallery-rail">
            {images.map((src, i) => (
              <button
                key={`${src}-${i}`}
                className={i === activeImage ? 'on' : ''}
                onClick={() => setActiveImage(i)}
                aria-label={t('product.galleryLabel', { index: i + 1 })}
              >
                <Photo src={src} />
              </button>
            ))}
          </div>
          <div className="gallery-main">
            <Photo src={images[activeImage]} alt={product.name} />
          </div>
        </div>

        {/* -------------------------------------------------------- buybox */}
        <div className="buybox">
          {product.brandName && <div className="brand-line">{product.brandName}</div>}
          <h1>{product.name}</h1>
          {product.subtitle && <p className="subtitle">{product.subtitle}</p>}

          <div className="price-line">
            <Price value={matchedSku?.price ?? product.price} size={28} accent />
            {product.originalPrice && Number(product.originalPrice) > Number(product.price) && (
              <span className="price-was" style={{ fontSize: 14 }}>
                ¥{Number(product.originalPrice).toFixed(2)}
              </span>
            )}
            <span className="stock-note">
              {maxQuantity > 0
                ? t('product.inStock', { count: maxQuantity })
                : t('product.soldOut')}
            </span>
          </div>

          {product.specList?.map((spec) => (
            <div className="spec-block" key={spec.name}>
              <div className="spec-head">
                <strong>{spec.name}</strong>
                <span>{selection[spec.name] ?? t('product.chooseSpec')}</span>
              </div>
              <div className="chips">
                {spec.values.map((value) => (
                  <button
                    key={value}
                    className={`chip ${selection[spec.name] === value ? 'on' : ''}`}
                    disabled={!valueAvailable(spec.name, value)}
                    onClick={() => setSelection({ ...selection, [spec.name]: value })}
                  >
                    {value}
                  </button>
                ))}
              </div>
            </div>
          ))}

          <div className="spec-block">
            <div className="spec-head">
              <strong>{t('product.quantity')}</strong>
              {maxQuantity > 0 && (
                <span>{t('product.maxQuantity', { count: maxQuantity })}</span>
              )}
            </div>
            <div className="stepper">
              <button onClick={() => setQuantity(Math.max(1, quantity - 1))} disabled={quantity <= 1}>
                −
              </button>
              <div className="value">{quantity}</div>
              <button
                onClick={() => setQuantity(Math.min(maxQuantity || 1, quantity + 1))}
                disabled={quantity >= maxQuantity}
              >
                +
              </button>
            </div>
          </div>

          <div className="buy-actions">
            <button
              className="btn btn-outline"
              onClick={addToCart}
              disabled={busy || maxQuantity === 0}
            >
              {t('product.addToCart')}
            </button>
            <button
              className="btn btn-accent"
              onClick={buyNow}
              disabled={busy || maxQuantity === 0}
            >
              {maxQuantity === 0 ? t('product.soldOutAction') : t('product.buyNow')}
              <IconArrowRight />
            </button>
          </div>

          <div className="buy-meta">
            <div className="row-item">
              <IconTruck /> {t('product.shipping')}
            </div>
            <div className="row-item">
              <IconRefresh /> {t('product.returns')}
            </div>
            <div className="row-item">
              <IconShield /> {t('product.authorised')}
            </div>
          </div>
        </div>
      </div>

      {/* ------------------------------------------------------- description */}
      {product.detail && (
        <div className="pdp-detail">
          <h3>{t('product.detail')}</h3>
          <div className="prose">{product.detail}</div>
        </div>
      )}

      {/* ----------------------------------------------------------- related */}
      {data?.related?.length ? (
        <section style={{ marginTop: 72 }}>
          <SectionHead title={t('product.related')} />
          <div className="related-grid">
            {data.related.slice(0, 4).map((item) => (
              <ProductCard key={item.id} product={item} />
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
