import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { useAuth } from '@/store/auth';
import type { MemberCoupon } from '@/types';

const ORDER_SHORTCUTS = [
  { key: '0', label: '待付款', glyph: '💳' },
  { key: '1', label: '待发货', glyph: '📦' },
  { key: '2', label: '待收货', glyph: '🚚' },
  { key: '3', label: '已完成', glyph: '✅' },
];

export default function MinePage() {
  const navigate = useNavigate();
  const { token, profile, orderCounts, couponCount, refresh, logout } = useAuth();
  const [coupons, setCoupons] = useState<MemberCoupon[]>([]);

  useEffect(() => {
    if (!token) return;
    refresh();
    api.get<MemberCoupon[]>('/app/coupons/mine').then(setCoupons).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  if (!token) {
    return (
      <div style={{ paddingBottom: 24 }}>
        <div
          style={{
            background: 'linear-gradient(150deg, #ff6b4a, #ff2f4f)',
            padding: '34px 20px 40px',
            color: '#fff',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div
              style={{
                width: 58,
                height: 58,
                borderRadius: 999,
                background: 'rgba(255,255,255,0.24)',
                display: 'grid',
                placeItems: 'center',
                fontSize: 24,
              }}
            >
              👤
            </div>
            <div>
              <div style={{ fontSize: 18, fontWeight: 650 }}>未登录</div>
              <div style={{ fontSize: 12.5, opacity: 0.86, marginTop: 2 }}>
                登录后同步订单与优惠券
              </div>
            </div>
          </div>
          <button
            className="btn"
            style={{ background: '#fff', color: 'var(--brand)', marginTop: 18, height: 40 }}
            onClick={() => navigate('/login')}
          >
            立即登录
          </button>
        </div>
        <div className="page">
          <div className="list-card">
            {[
              { label: '我的订单', glyph: '📋', to: '/orders' },
              { label: '收货地址', glyph: '📍', to: '/address' },
              { label: '优惠券', glyph: '🎟️', to: '/coupons' },
            ].map((row) => (
              <div key={row.label} className="row" onClick={() => navigate(row.to)}>
                <span style={{ fontSize: 16 }}>{row.glyph}</span>
                <span style={{ flex: 1 }}>{row.label}</span>
                <span className="arrow">›</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  const usableCoupons = coupons.filter((c) => c.status === 0).length;

  return (
    <div style={{ paddingBottom: 24 }}>
      {/* profile hero */}
      <div
        style={{
          background: 'linear-gradient(150deg, #ff6b4a 0%, #ff2f4f 62%, #ff3d71 100%)',
          padding: '30px 20px 54px',
          color: '#fff',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            position: 'absolute',
            right: -40,
            top: -50,
            width: 180,
            height: 180,
            borderRadius: '50%',
            background: 'rgba(255,255,255,0.12)',
          }}
        />
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, position: 'relative' }}>
          <img
            src={profile?.avatar}
            alt=""
            style={{
              width: 60,
              height: 60,
              borderRadius: 999,
              objectFit: 'cover',
              border: '2px solid rgba(255,255,255,0.5)',
              background: 'rgba(255,255,255,0.2)',
            }}
          />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 18, fontWeight: 650 }}>
              {profile?.nickname ?? '会员'}
              <span
                style={{
                  fontSize: 10.5,
                  marginLeft: 8,
                  padding: '2px 8px',
                  borderRadius: 999,
                  background: 'rgba(255,255,255,0.24)',
                  verticalAlign: 'middle',
                  fontWeight: 600,
                }}
              >
                {profile?.levelName ?? '普通会员'}
              </span>
            </div>
            <div style={{ fontSize: 12.5, opacity: 0.86, marginTop: 3 }}>
              {profile?.mobileMasked ?? profile?.mobile}
            </div>
          </div>
          <button
            className="icon-btn"
            style={{ color: '#fff', background: 'rgba(255,255,255,0.18)' }}
            onClick={() => navigate('/settings/profile')}
          >
            ⚙️
          </button>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            marginTop: 22,
            position: 'relative',
          }}
        >
          {[
            { label: '积分', value: profile?.points ?? 0 },
            { label: '余额', value: `¥${Number(profile?.balance ?? 0).toFixed(0)}` },
            { label: '优惠券', value: usableCoupons },
          ].map((stat) => (
            <div key={stat.label} style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 19, fontWeight: 680 }}>{stat.value}</div>
              <div style={{ fontSize: 11.5, opacity: 0.82, marginTop: 1 }}>{stat.label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* order card */}
      <div className="page" style={{ marginTop: -34 }}>
        <div className="list-card" style={{ padding: '14px 6px 8px' }}>
          <div
            className="row-between"
            style={{ padding: '0 10px 12px' }}
            onClick={() => navigate('/orders')}
          >
            <span style={{ fontSize: 14.5, fontWeight: 620 }}>我的订单</span>
            <span className="muted">全部订单 ›</span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', paddingBottom: 10 }}>
            {ORDER_SHORTCUTS.map((item) => {
              const map: Record<string, string> = {
                '0': 'pendingPayment',
                '1': 'pendingShipment',
                '2': 'shipped',
                '3': 'completed',
              };
              const count = orderCounts[map[item.key]] ?? 0;
              return (
                <button
                  key={item.key}
                  onClick={() => navigate(`/orders?status=${item.key}`)}
                  style={{
                    border: 'none',
                    background: 'transparent',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 5,
                    cursor: 'pointer',
                    position: 'relative',
                    padding: '4px 0',
                  }}
                >
                  <span style={{ fontSize: 21 }}>{item.glyph}</span>
                  <span style={{ fontSize: 11.5, color: 'var(--ink-2)' }}>{item.label}</span>
                  {count > 0 && (
                    <span
                      style={{
                        position: 'absolute',
                        top: 0,
                        right: '22%',
                        minWidth: 15,
                        height: 15,
                        padding: '0 4px',
                        borderRadius: 999,
                        background: 'var(--brand)',
                        color: '#fff',
                        fontSize: 9.5,
                        display: 'grid',
                        placeItems: 'center',
                        fontWeight: 600,
                      }}
                    >
                      {count}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* coupon strip */}
        {usableCoupons > 0 && (
          <div
            className="list-card"
            style={{
              marginTop: 10,
              padding: '12px 14px',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              background: 'linear-gradient(100deg, #fff7f2, #fff)',
            }}
            onClick={() => navigate('/coupons')}
          >
            <span style={{ fontSize: 18 }}>🎟️</span>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13.5, fontWeight: 600 }}>
                你有 {usableCoupons} 张优惠券待使用
              </div>
              <div className="muted">下单时自动匹配最优优惠</div>
            </div>
            <span className="arrow">›</span>
          </div>
        )}

        {/* menu */}
        <div className="list-card" style={{ marginTop: 10 }}>
          {[
            { label: '收货地址', glyph: '📍', to: '/address' },
            { label: '我的优惠券', glyph: '🎟️', to: '/coupons' },
            { label: '个人资料', glyph: '🧑', to: '/settings/profile' },
          ].map((row) => (
            <div key={row.label} className="row" onClick={() => navigate(row.to)}>
              <span style={{ fontSize: 16 }}>{row.glyph}</span>
              <span style={{ flex: 1 }}>{row.label}</span>
              <span className="arrow">›</span>
            </div>
          ))}
        </div>

        <button
          className="btn btn-outline btn-block"
          style={{ marginTop: 18, color: '#d94848' }}
          onClick={async () => {
            await attempt(() => api.post('/app/auth/logout'));
            logout();
          }}
        >
          退出登录
        </button>

        <div style={{ textAlign: 'center', marginTop: 22 }} className="muted">
          山脊单车 · 山地车商城演示系统
        </div>
      </div>
    </div>
  );
}
