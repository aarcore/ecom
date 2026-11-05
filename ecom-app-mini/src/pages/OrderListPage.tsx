import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, attempt, type PageResult } from '@/api/client';
import { Empty, Price } from '@/components/ui';
import { useAuth } from '@/store/auth';
import { ORDER_STATUS, type Order } from '@/types';

const TABS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待付款' },
  { key: '1', label: '待发货' },
  { key: '2', label: '待收货' },
  { key: '3', label: '已完成' },
];

export default function OrderListPage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const token = useAuth((s) => s.token);
  const [tab, setTab] = useState(params.get('status') ?? 'all');
  const [rows, setRows] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    if (!token) return;
    setLoading(true);
    const result = await attempt(() =>
      api.get<PageResult<Order>>('/app/orders', {
        status: tab === 'all' ? undefined : Number(tab),
        page: 1,
        limit: 30,
      }),
    );
    setRows(result?.records ?? []);
    setLoading(false);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, token]);

  if (!token) {
    return (
      <div className="page">
        <Empty
          glyph="🔐"
          text="登录后查看订单"
          action={
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/login')}>
              去登录
            </button>
          }
        />
      </div>
    );
  }

  return (
    <div style={{ paddingBottom: 24 }}>
      <div className="topbar">
        <button className="icon-btn" onClick={() => navigate('/mine')}>
          ‹
        </button>
        <div className="title">我的订单</div>
      </div>

      <div
        style={{
          display: 'flex',
          gap: 4,
          padding: '10px 14px',
          background: '#fff',
          borderBottom: '1px solid var(--line)',
          position: 'sticky',
          top: 50,
          zIndex: 20,
        }}
      >
        {TABS.map((item) => (
          <button
            key={item.key}
            onClick={() => {
              setTab(item.key);
              const next = new URLSearchParams(params);
              if (item.key === 'all') next.delete('status');
              else next.set('status', item.key);
              setParams(next);
            }}
            style={{
              flex: 1,
              border: 'none',
              background: 'transparent',
              padding: '7px 0',
              fontSize: 13,
              fontWeight: tab === item.key ? 600 : 400,
              color: tab === item.key ? 'var(--brand)' : 'var(--ink-2)',
              cursor: 'pointer',
              position: 'relative',
            }}
          >
            {item.label}
            {tab === item.key && (
              <span
                style={{
                  position: 'absolute',
                  bottom: 0,
                  left: '50%',
                  transform: 'translateX(-50%)',
                  width: 18,
                  height: 2.5,
                  borderRadius: 3,
                  background: 'var(--brand)',
                }}
              />
            )}
          </button>
        ))}
      </div>

      <div className="page">
        {loading ? (
          <div className="stack">
            {[0, 1, 2].map((i) => (
              <div key={i} className="skel" style={{ height: 150, borderRadius: 14 }} />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <Empty
            glyph="📦"
            text="还没有相关订单"
            action={
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/')}>
                去下单
              </button>
            }
          />
        ) : (
          <div className="stack">
            {rows.map((order) => (
              <div
                key={order.id}
                className="list-card"
                style={{ padding: 13 }}
                onClick={() => navigate(`/order/${order.orderNo}`)}
              >
                <div className="row-between" style={{ marginBottom: 10 }}>
                  <span className="muted" style={{ fontFamily: 'ui-monospace, monospace' }}>
                    {order.orderNo}
                  </span>
                  <span
                    style={{
                      fontSize: 12.5,
                      fontWeight: 600,
                      color: order.status === 0 ? 'var(--brand)' : order.status === 3 ? '#0f9d58' : 'var(--ink-2)',
                    }}
                  >
                    {ORDER_STATUS[order.status]?.text ?? order.statusText}
                  </span>
                </div>

                {order.items.map((item) => (
                  <div key={item.id} style={{ display: 'flex', gap: 10, marginBottom: 8 }}>
                    <img
                      src={item.productCover}
                      alt={item.productName}
                      style={{ width: 58, height: 58, borderRadius: 8, objectFit: 'cover', background: '#f2f4f8' }}
                    />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, lineHeight: 1.4 }}>{item.productName}</div>
                      <div className="muted" style={{ marginTop: 2 }}>
                        {item.specValues} · ×{item.quantity}
                      </div>
                    </div>
                    <span className="muted">¥{Number(item.price).toFixed(2)}</span>
                  </div>
                ))}

                <div
                  className="row-between"
                  style={{ paddingTop: 9, borderTop: '1px solid var(--line)', marginTop: 4 }}
                >
                  <span className="muted">{new Date(order.createdAt).toLocaleDateString('zh-CN')}</span>
                  <span style={{ fontSize: 12.5 }}>
                    实付 <Price value={order.payAmount} size="sm" />
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
