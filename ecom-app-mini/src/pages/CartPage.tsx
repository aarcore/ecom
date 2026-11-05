import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { Empty, Price } from '@/components/ui';
import { useAuth } from '@/store/auth';
import type { CartData, CartItem } from '@/types';

export default function CartPage() {
  const navigate = useNavigate();
  const token = useAuth((s) => s.token);
  const refreshCart = useAuth((s) => s.refreshCart);
  const [data, setData] = useState<CartData | null>(null);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(false);

  const load = async () => {
    if (!token) return;
    setLoading(true);
    const result = await attempt(() => api.get<CartData>('/app/cart'));
    if (result) setData(result);
    setLoading(false);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  if (!token) {
    return (
      <div className="page">
        <div className="topbar" style={{ margin: '-12px -14px 12px' }}>
          <div className="title">购物车</div>
        </div>
        <Empty
          glyph="🛒"
          text="登录后查看购物车"
          action={
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/login')}>
              去登录
            </button>
          }
        />
      </div>
    );
  }

  const items = data?.items ?? [];

  const updateQuantity = async (item: CartItem, delta: number) => {
    const next = item.quantity + delta;
    if (next < 1 || next > item.stock) return;
    const result = await attempt(() =>
      api.put(`/app/cart/${item.id}/quantity`, null, { quantity: next }),
    );
    if (result) {
      load();
      refreshCart();
    }
  };

  const toggleItem = async (item: CartItem) => {
    await attempt(() => api.put(`/app/cart/${item.id}/checked`, null, { checked: item.checked !== 1 }));
    load();
  };

  const toggleAll = async () => {
    await attempt(() => api.put('/app/cart/checked', null, { checked: !data?.allChecked }));
    load();
  };

  const removeItem = async (item: CartItem) => {
    const result = await attempt(() => api.delete(`/app/cart/${item.id}`), '已移除');
    if (result !== null) {
      load();
      refreshCart();
    }
  };

  const checkedItems = items.filter((i) => i.checked === 1);

  const checkout = () => {
    if (!checkedItems.length) return;
    navigate('/checkout', { state: { source: 'cart' } });
  };

  return (
    <div style={{ paddingBottom: checkedItems.length ? 78 : 24 }}>
      <div className="topbar">
        <div className="title">购物车{items.length ? `（${items.length}）` : ''}</div>
        {items.length > 0 && (
          <button className="icon-btn" style={{ width: 'auto', fontSize: 13, color: '#5f6672' }} onClick={() => setEditing(!editing)}>
            {editing ? '完成' : '管理'}
          </button>
        )}
      </div>

      <div className="page">
        {loading && !data ? (
          <div className="stack">
            {[0, 1, 2].map((i) => (
              <div key={i} className="skel" style={{ height: 104, borderRadius: 14 }} />
            ))}
          </div>
        ) : items.length === 0 ? (
          <Empty
            glyph="🛒"
            text="购物车还是空的，去挑点好东西吧"
            action={
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/')}>
                去逛逛
              </button>
            }
          />
        ) : (
          <div className="stack">
            {items.map((item) => (
              <div
                key={item.id}
                style={{
                  display: 'flex',
                  gap: 11,
                  background: '#fff',
                  borderRadius: 14,
                  padding: 12,
                  opacity: item.available ? 1 : 0.62,
                }}
              >
                <div
                  className={`checkbox ${item.checked === 1 ? 'on' : ''}`}
                  onClick={() => toggleItem(item)}
                  style={{ alignSelf: 'center' }}
                >
                  {item.checked === 1 ? '✓' : ''}
                </div>

                <img
                  src={item.cover}
                  alt={item.productName}
                  style={{ width: 76, height: 76, borderRadius: 10, objectFit: 'cover', background: '#f2f4f8' }}
                  onClick={() => navigate(`/product/${item.productId}`)}
                />

                <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
                  <div
                    style={{
                      fontSize: 13,
                      lineHeight: 1.4,
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                    }}
                    onClick={() => navigate(`/product/${item.productId}`)}
                  >
                    {item.productName}
                  </div>
                  <div className="muted" style={{ marginTop: 3 }}>{item.specValues}</div>
                  {!item.available && (
                    <div style={{ fontSize: 11, color: 'var(--brand)', marginTop: 2 }}>
                      {item.stock === 0 ? '已售罄' : '库存不足'}
                    </div>
                  )}

                  <div className="row-between" style={{ marginTop: 'auto', paddingTop: 6 }}>
                    <Price value={item.price} size="sm" />
                    {editing ? (
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ color: 'var(--brand)', padding: 0, height: 'auto' }}
                        onClick={() => removeItem(item)}
                      >
                        删除
                      </button>
                    ) : (
                      <div className="stepper">
                        <button onClick={() => updateQuantity(item, -1)}>−</button>
                        <input value={item.quantity} readOnly />
                        <button onClick={() => updateQuantity(item, 1)}>+</button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {items.length > 0 && (
        <div
          className="bottom-bar"
          style={{ position: 'fixed', left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: 480, bottom: 'calc(var(--tabbar-h) + var(--safe-bottom))' }}
        >
          <div
            className={`checkbox ${data?.allChecked ? 'on' : ''}`}
            onClick={toggleAll}
          >
            {data?.allChecked ? '✓' : ''}
          </div>
          <span style={{ fontSize: 12.5, color: 'var(--ink-2)' }}>全选</span>
          <div style={{ flex: 1, textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>合计</div>
            <Price value={data?.checkedAmount ?? 0} />
          </div>
          <button className="btn btn-primary" style={{ minWidth: 108 }} disabled={!checkedItems.length} onClick={checkout}>
            结算{checkedItems.length ? `(${data?.checkedCount})` : ''}
          </button>
        </div>
      )}
    </div>
  );
}
