import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { Empty } from '@/components/ui';
import { useAuth } from '@/store/auth';
import type { MemberCoupon } from '@/types';

function faceValue(coupon: MemberCoupon) {
  if (coupon.type === 'discount') {
    const rate = Number(coupon.discount ?? 10);
    return `${(rate / 10).toFixed(rate % 1 === 0 ? 0 : 1)}折`;
  }
  return `¥${Number(coupon.amount).toFixed(0)}`;
}

export default function CouponsPage() {
  const navigate = useNavigate();
  const token = useAuth((s) => s.token);
  const [rows, setRows] = useState<MemberCoupon[]>([]);
  const [available, setAvailable] = useState<
    { id: number; name: string; type: string; amount: number; discount: number; minAmount: number; validDays: number }[]
  >([]);
  const [tab, setTab] = useState<'mine' | 'center'>('mine');

  const load = () => {
    if (!token) return;
    api.get<MemberCoupon[]>('/app/coupons/mine').then(setRows).catch(() => undefined);
    api.get<typeof available>('/app/coupons/available').then(setAvailable).catch(() => undefined);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const receive = async (couponId: number) => {
    const result = await attempt(() => api.post(`/app/coupons/${couponId}/receive`), '领取成功');
    if (result) load();
  };

  if (!token) {
    return (
      <div className="page">
        <Empty
          glyph="🎟️"
          text="登录后查看优惠券"
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
        <div className="title">优惠券</div>
      </div>

      <div style={{ display: 'flex', gap: 6, padding: '10px 14px' }}>
        {[
          { key: 'mine' as const, label: `我的 (${rows.filter((r) => r.status === 0).length})` },
          { key: 'center' as const, label: '领券中心' },
        ].map((item) => (
          <button
            key={item.key}
            className={`chip ${tab === item.key ? 'on' : ''}`}
            style={{ flex: 1, textAlign: 'center', padding: '8px 0' }}
            onClick={() => setTab(item.key)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="page" style={{ paddingTop: 0 }}>
        {tab === 'mine' ? (
          rows.length === 0 ? (
            <Empty glyph="🎟️" text="还没有优惠券" />
          ) : (
            <div className="stack">
              {rows.map((coupon) => {
                const expired = coupon.status !== 0;
                return (
                  <div
                    key={coupon.id}
                    style={{
                      display: 'flex',
                      borderRadius: 14,
                      overflow: 'hidden',
                      background: '#fff',
                      opacity: expired ? 0.55 : 1,
                    }}
                  >
                    <div
                      style={{
                        width: 104,
                        background: expired
                          ? '#c8cdd6'
                          : 'linear-gradient(135deg, #ff6b4a, #ff2f4f)',
                        color: '#fff',
                        display: 'grid',
                        placeItems: 'center',
                        padding: 12,
                        textAlign: 'center',
                      }}
                    >
                      <div>
                        <div style={{ fontSize: 22, fontWeight: 700 }}>{faceValue(coupon)}</div>
                        <div style={{ fontSize: 10.5, opacity: 0.88, marginTop: 2 }}>
                          满 {Number(coupon.minAmount).toFixed(0)} 可用
                        </div>
                      </div>
                    </div>
                    <div style={{ flex: 1, padding: '12px 14px', minWidth: 0 }}>
                      <div style={{ fontSize: 13.5, fontWeight: 600 }}>{coupon.name}</div>
                      <div className="muted" style={{ marginTop: 3 }}>
                        {coupon.endAt ? `有效期至 ${new Date(coupon.endAt).toLocaleDateString('zh-CN')}` : ''}
                      </div>
                      <div style={{ marginTop: 8 }}>
                        <span
                          style={{
                            fontSize: 10.5,
                            padding: '2px 7px',
                            borderRadius: 6,
                            background: expired ? '#f2f3f7' : 'var(--brand-soft)',
                            color: expired ? '#8b93a7' : 'var(--brand)',
                            fontWeight: 600,
                          }}
                        >
                          {coupon.statusText}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )
        ) : available.length === 0 ? (
          <Empty glyph="🎁" text="暂无可领取的优惠券" />
        ) : (
          <div className="stack">
            {available.map((coupon) => (
              <div
                key={coupon.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  borderRadius: 14,
                  background: '#fff',
                  padding: '14px 16px',
                }}
              >
                <div
                  style={{
                    width: 58,
                    height: 58,
                    borderRadius: 12,
                    background: 'var(--brand-soft)',
                    color: 'var(--brand)',
                    display: 'grid',
                    placeItems: 'center',
                    fontWeight: 700,
                    fontSize: coupon.type === 'discount' ? 15 : 18,
                    flexShrink: 0,
                  }}
                >
                  {coupon.type === 'discount'
                    ? `${(Number(coupon.discount) / 10).toFixed(1)}折`
                    : `¥${Number(coupon.amount).toFixed(0)}`}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13.5, fontWeight: 600 }}>{coupon.name}</div>
                  <div className="muted" style={{ marginTop: 2 }}>
                    满 {Number(coupon.minAmount).toFixed(0)} 可用 · 领取后 {coupon.validDays} 天有效
                  </div>
                </div>
                <button className="btn btn-primary btn-sm" onClick={() => receive(coupon.id)}>
                  领取
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
