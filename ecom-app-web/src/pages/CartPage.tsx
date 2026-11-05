import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { Photo } from '@/components/Photo';
import { Empty, Price } from '@/components/ui';
import { IconTrash } from '@/components/icons';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { CartData, CartItem } from '@/types';

export default function CartPage() {
  const navigate = useNavigate();
  const { token, refreshCart } = useAuth();
  const { t } = useT();
  const locale = useLocale((s) => s.locale);
  const [data, setData] = useState<CartData | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!token) {
      setLoading(false);
      return;
    }
    const result = await attempt(() => api.get<CartData>('/app/cart'));
    if (result) setData(result);
    setLoading(false);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, locale]);

  if (!token) {
    return (
      <div className="shell">
        <Empty
          glyph="◫"
          title={t('cart.loginTitle')}
          description={t('cart.loginHint')}
          action={
            <button className="btn btn-primary" onClick={() => navigate('/login')}>
              {t('cart.loginAction')}
            </button>
          }
        />
      </div>
    );
  }

  const items = data?.items ?? [];
  const checked = items.filter((i) => i.checked === 1);

  const changeQty = async (item: CartItem, next: number) => {
    if (next < 1 || next > item.stock) return;
    const ok = await attempt(() =>
      api.put(`/app/cart/${item.id}/quantity`, null, { quantity: next }),
    );
    if (ok !== null) {
      load();
      refreshCart();
    }
  };

  const toggle = async (item: CartItem) => {
    await attempt(() => api.put(`/app/cart/${item.id}/checked`, null, { checked: item.checked !== 1 }));
    load();
  };

  const toggleAll = async () => {
    await attempt(() => api.put('/app/cart/checked', null, { checked: !data?.allChecked }));
    load();
  };

  const remove = async (item: CartItem) => {
    const ok = await attempt(() => api.delete(`/app/cart/${item.id}`), t('cart.removed'));
    if (ok !== null) {
      load();
      refreshCart();
    }
  };

  return (
    <div className="shell">
      <div style={{ paddingTop: 40, paddingBottom: 4 }}>
        <h1 style={{ fontSize: 'clamp(24px, 3vw, 32px)' }}>{t('cart.title')}</h1>
        <div className="muted" style={{ marginTop: 6 }}>
          {items.length > 0 ? t('common.pieces', { count: items.length }) : ''}
        </div>
      </div>

      {loading ? (
        <div className="cart-layout">
          <div className="cart-list">
            {[0, 1, 2].map((i) => (
              <div key={i} style={{ padding: '22px 0' }}>
                <div className="skel" style={{ height: 104 }} />
              </div>
            ))}
          </div>
        </div>
      ) : items.length === 0 ? (
        <Empty
          glyph="◫"
          title={t('cart.empty')}
          description={t('cart.emptyHint')}
          action={
            <button className="btn btn-primary" onClick={() => navigate('/shop')}>
              {t('cart.browse')}
            </button>
          }
        />
      ) : (
        <div className="cart-layout">
          <div>
            <div className="cart-toolbar">
              <button className="row" style={{ gap: 8 }} onClick={toggleAll}>
                <span className={`checkbox ${data?.allChecked ? 'on' : ''}`}>✓</span>
                <span>{t('cart.selectAll')}</span>
              </button>
              <div className="spacer" />
              <span>{t('cart.selectedCount', { count: checked.length })}</span>
            </div>

            <div className="cart-list">
              {items.map((item) => (
                <div className="cart-line" key={item.id}>
                  <span
                    className={`checkbox ${item.checked === 1 ? 'on' : ''}`}
                    onClick={() => toggle(item)}
                    role="checkbox"
                    aria-checked={item.checked === 1}
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        toggle(item);
                      }
                    }}
                  >
                    ✓
                  </span>

                  <Link to={`/product/${item.productId}`} className="media">
                    <Photo src={item.cover} />
                  </Link>

                  <div className="info">
                    <div className="title">
                      <Link to={`/product/${item.productId}`}>{item.productName}</Link>
                    </div>
                    {item.specValues && <div className="spec">{item.specValues}</div>}
                    {!item.available && (
                      <div className="warn">
                        {item.stock === 0
                          ? t('cart.specSoldOut')
                          : t('cart.stockLow', { count: item.stock })}
                      </div>
                    )}
                    <div className="row" style={{ gap: 12, marginTop: 6 }}>
                      <Price value={item.price} size={14} />
                      {item.originalPrice && Number(item.originalPrice) > Number(item.price) && (
                        <span className="price-was">¥{Number(item.originalPrice).toFixed(2)}</span>
                      )}
                    </div>
                  </div>

                  <div className="tail">
                    <div className="stepper">
                      <button onClick={() => changeQty(item, item.quantity - 1)} disabled={item.quantity <= 1}>
                        −
                      </button>
                      <div className="value">{item.quantity}</div>
                      <button
                        onClick={() => changeQty(item, item.quantity + 1)}
                        disabled={item.quantity >= item.stock}
                      >
                        +
                      </button>
                    </div>
                    <div className="line-total tabular">
                      ¥{Number(item.price * item.quantity).toFixed(2)}
                    </div>
                    <button className="remove row" style={{ gap: 5 }} onClick={() => remove(item)}>
                      <IconTrash size={13} /> {t('common.remove')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <aside className="summary">
            <h3>{t('cart.summary')}</h3>
            <div className="summary-line">
              <span>{t('cart.summarySelected')}</span>
              <span className="amt">{t('common.pieces', { count: data?.checkedCount ?? 0 })}</span>
            </div>
            <div className="summary-line">
              <span>{t('cart.summaryGoods')}</span>
              <span className="amt">¥{Number(data?.checkedAmount ?? 0).toFixed(2)}</span>
            </div>
            <div className="summary-line">
              <span>{t('cart.summaryFreight')}</span>
              <span className="amt">{t('cart.freightAtCheckout')}</span>
            </div>
            <div className="summary-total">
              <span>{t('cart.summaryTotal')}</span>
              <Price value={data?.checkedAmount ?? 0} size={22} />
            </div>
            <p className="summary-note">{t('cart.summaryNote')}</p>
            <button
              className="btn btn-primary btn-block"
              disabled={checked.length === 0}
              onClick={() => navigate('/checkout', { state: { source: 'cart' } })}
            >
              {checked.length
                ? t('cart.checkoutWithCount', { count: checked.length })
                : t('cart.checkout')}
            </button>
            <button
              className="btn btn-ghost btn-block"
              style={{ marginTop: 6 }}
              onClick={() => navigate('/shop')}
            >
              {t('cart.continue')}
            </button>
          </aside>
        </div>
      )}
    </div>
  );
}
