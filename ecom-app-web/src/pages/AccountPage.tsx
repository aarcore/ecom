import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, type PageResult } from '@/api/client';
import AccountLayout from '@/components/AccountLayout';
import { Empty, Price } from '@/components/ui';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import { ORDER_STATUS_KEY } from '@/i18n/messages';
import type { MessageKey } from '@/i18n/messages';
import type { Order } from '@/types';

export default function AccountPage() {
  const navigate = useNavigate();
  const { profile, orderCounts, couponCount, refresh } = useAuth();
  const { t } = useT();
  const locale = useLocale((s) => s.locale);
  const [recent, setRecent] = useState<Order[]>([]);
  const [recentLoaded, setRecentLoaded] = useState(false);

  useEffect(() => {
    refresh();
    api
      .get<PageResult<Order>>('/app/orders', { page: 1, limit: 3 })
      .then((result) => setRecent(result.records ?? []))
      .catch(() => undefined)
      .finally(() => setRecentLoaded(true));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [locale]);

  const badges: { key: string; label: MessageKey; status: string }[] = [
    { key: 'pendingPayment', label: 'orders.status.pendingPayment', status: '0' },
    { key: 'pendingShipment', label: 'orders.status.pendingShipment', status: '1' },
    { key: 'shipped', label: 'orders.status.shipped', status: '2' },
    { key: 'completed', label: 'orders.status.completed', status: '3' },
  ];

  return (
    <AccountLayout
      title={t('account.overview')}
      subtitle={
        profile
          ? `${profile.levelName} · ${t('account.totalSpent', {
              amount: `¥${Number(profile.totalSpent).toFixed(2)}`,
            })}`
          : undefined
      }
      activePath="/account"
    >
      {/* ----------------------------------------------------------- stats */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
          borderTop: '1px solid var(--rule)',
          borderBottom: '1px solid var(--rule)',
          marginBottom: 36,
        }}
      >
        {[
          { label: t('account.points'), value: profile?.points ?? 0 },
          { label: t('account.balance'), value: `¥${Number(profile?.balance ?? 0).toFixed(0)}` },
          { label: t('account.orderCount'), value: profile?.orderCount ?? 0 },
          { label: t('account.availableCoupons'), value: couponCount },
        ].map((stat, i) => (
          <div
            key={stat.label}
            style={{
              padding: '22px 20px',
              borderRight: i < 3 ? '1px solid var(--rule)' : 'none',
            }}
          >
            <div
              style={{
                fontFamily: 'var(--font-display)',
                fontSize: 26,
                lineHeight: 1.1,
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {stat.value}
            </div>
            <div className="muted" style={{ marginTop: 4 }}>{stat.label}</div>
          </div>
        ))}
      </div>

      {/* --------------------------------------------------------- shortcuts */}
      <section style={{ marginBottom: 40 }}>
        <div className="row-between" style={{ marginBottom: 16 }}>
          <h3 style={{ fontSize: 13, letterSpacing: '0.12em', textTransform: 'uppercase', color: 'var(--text-3)', fontWeight: 600 }}>
            {t('account.orderStatus')}
          </h3>
          <Link to="/orders" className="btn-link">
            {t('account.allOrders')}
          </Link>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 10 }}>
          {badges.map((badge) => {
            const count = orderCounts[badge.key] ?? 0;
            return (
              <button
                key={badge.key}
                className="address-card"
                style={{ alignItems: 'center', gap: 10 }}
                onClick={() => navigate(`/orders?status=${badge.status}`)}
              >
                <span style={{ flex: 1, textAlign: 'left', fontSize: 13.5 }}>{t(badge.label)}</span>
                <span
                  style={{
                    fontFamily: 'var(--font-display)',
                    fontSize: 20,
                    color: count > 0 ? 'var(--text)' : 'var(--text-4)',
                  }}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>
      </section>

      {/* ------------------------------------------------------ recent orders */}
      <section>
        <div className="row-between" style={{ marginBottom: 16 }}>
          <h3 style={{ fontSize: 13, letterSpacing: '0.12em', textTransform: 'uppercase', color: 'var(--text-3)', fontWeight: 600 }}>
            {t('account.recentOrders')}
          </h3>
        </div>

        {recent.length === 0 ? (
          <Empty
            glyph="◎"
            title={t('account.noOrders')}
            description={t('account.noOrdersHint')}
            action={
              <Link to="/shop" className="btn btn-primary">
                {t('account.browse')}
              </Link>
            }
          />
        ) : (
          recent.map((order) => (
            <article className="order-card" key={order.id}>
              <div className="head">
                <span className="no">{order.orderNo}</span>
                <span className={`tag ${order.status === 3 ? 'ok' : ''}`}>
                  {ORDER_STATUS_KEY[order.status]
                    ? t(ORDER_STATUS_KEY[order.status])
                    : order.statusText}
                </span>
              </div>
              <div className="foot">
                <span className="total">
                  <span className="muted">
                    {t('account.orderLines', { count: order.items.length })}
                  </span>
                  <Price value={order.payAmount} size={15} />
                </span>
                <Link to={`/orders/${order.orderNo}`} className="btn btn-outline btn-sm">
                  {t('account.viewDetail')}
                </Link>
              </div>
            </article>
          ))
        )}
      </section>
    </AccountLayout>
  );
}
