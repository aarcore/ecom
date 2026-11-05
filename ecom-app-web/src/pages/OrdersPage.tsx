import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, attempt, type PageResult } from '@/api/client';
import { payApi, submitGatewayForm } from '@/api/pay';
import AccountLayout from '@/components/AccountLayout';
import { Photo } from '@/components/Photo';
import { Empty, Price } from '@/components/ui';
import { ORDER_STATUS_KEY } from '@/i18n/messages';
import type { MessageKey } from '@/i18n/messages';
import { formatDate } from '@/i18n/locale';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { Order } from '@/types';

const TABS: { key: string; label: MessageKey }[] = [
  { key: 'all', label: 'orders.tab.all' },
  { key: '0', label: 'orders.status.pendingPayment' },
  { key: '1', label: 'orders.status.pendingShipment' },
  { key: '2', label: 'orders.status.shipped' },
  { key: '3', label: 'orders.status.completed' },
];

export default function OrdersPage() {
  const { t, locale } = useT();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const status = params.get('status') ?? 'all';
  const [rows, setRows] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  // the order list is refetched on a language change because the line names
  // are translated server-side
  const activeLocale = useLocale((s) => s.locale);

  const load = async () => {
    setLoading(true);
    const result = await attempt(() =>
      api.get<PageResult<Order>>('/app/orders', {
        status: status === 'all' ? undefined : Number(status),
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
  }, [status, activeLocale]);

  /**
   * Pays an existing unpaid order.
   *
   * Uses whatever the storefront's default channel is rather than asking: the
   * order list is not the place for a payment-method picker, and the detail page
   * offers the full choice. A redirect or QR just hands off and the list refreshes
   * when the customer comes back.
   */
  const pay = async (order: Order) => {
    const channels = await attempt(() => payApi.channels());
    const channel = channels?.[0]?.code;
    if (!channel) return;

    const launch = await attempt(() => payApi.launch(order.orderNo, channel));
    if (!launch) return;

    if (launch.mode === 'REDIRECT' && launch.url) {
      if (launch.url.trimStart().startsWith('<')) {
        submitGatewayForm(launch.url);
        return;
      }
      window.location.assign(launch.url);
      return;
    }
    if (launch.mode === 'SYNC') {
      load();
      return;
    }
    // QR / SDK need a dedicated screen; send them to the order detail.
    navigate(`/orders/${order.orderNo}`);
  };

  const cancel = async (order: Order) => {
    const ok = await attempt(
      () =>
        api.post(`/app/orders/${order.orderNo}/cancel`, { reason: t('orders.cancelReason') }),
      t('orders.cancelled'),
    );
    if (ok) load();
  };

  const confirm = async (order: Order) => {
    const ok = await attempt(
      () => api.post(`/app/orders/${order.orderNo}/confirm`),
      t('orders.confirmed'),
    );
    if (ok) load();
  };

  const statusLabel = (order: Order) => {
    const key = ORDER_STATUS_KEY[order.status];
    // fall back to the server string for a status this build does not know
    return key ? t(key) : order.statusText;
  };

  return (
    <AccountLayout
      title={t('orders.title')}
      subtitle={t('orders.subtitle')}
      activePath="/orders"
      action={
        <div className="pills">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              className={`pill ${status === tab.key ? 'on' : ''}`}
              onClick={() => {
                const next = new URLSearchParams(params);
                if (tab.key === 'all') next.delete('status');
                else next.set('status', tab.key);
                setParams(next);
              }}
            >
              {t(tab.label)}
            </button>
          ))}
        </div>
      }
    >
      {loading ? (
        <div className="stack">
          {[0, 1].map((i) => (
            <div key={i} className="skel" style={{ height: 190 }} />
          ))}
        </div>
      ) : rows.length === 0 ? (
        <Empty
          glyph="◎"
          title={t('orders.empty')}
          description={t('orders.emptyHint')}
          action={
            <Link to="/shop" className="btn btn-primary">
              {t('account.browse')}
            </Link>
          }
        />
      ) : (
        <div>
          {rows.map((order) => {
            const canPay = order.status === 0;
            const canConfirm = order.status === 2;
            return (
              <article className="order-card" key={order.id}>
                <div className="head">
                  <span>
                    <span className="no">{order.orderNo}</span>
                    <span style={{ margin: '0 10px', opacity: 0.4 }}>·</span>
                    {formatDate(order.createdAt, locale)}
                  </span>
                  <span
                    className={`tag ${order.status === 3 ? 'ok' : order.status === 0 ? 'accent' : ''}`}
                  >
                    {statusLabel(order)}
                  </span>
                </div>

                <div className="lines">
                  {order.items.map((item) => (
                    <div className="line" key={item.id}>
                      <Link to={`/product/${item.productId}`} className="media">
                        <Photo src={item.productCover} />
                      </Link>
                      <div style={{ minWidth: 0 }}>
                        <Link to={`/product/${item.productId}`} style={{ fontSize: 14 }}>
                          {item.productName}
                        </Link>
                        <div className="muted" style={{ marginTop: 3 }}>
                          {item.specValues} · {t('orders.quantity', { count: item.quantity })}
                        </div>
                      </div>
                      <Price value={item.price} size={14} />
                    </div>
                  ))}
                </div>

                <div className="foot">
                  <span className="total">
                    <span className="muted">{t('orders.paid')}</span>
                    <Price value={order.payAmount} size={17} />
                    {Number(order.discountAmount) > 0 && (
                      <span className="muted" style={{ fontSize: 11.5 }}>
                        {t('orders.saved', {
                          amount: `¥${Number(order.discountAmount).toFixed(2)}`,
                        })}
                      </span>
                    )}
                  </span>
                  <div className="actions">
                    <Link to={`/orders/${order.orderNo}`} className="btn btn-outline btn-sm">
                      {t('orders.viewDetail')}
                    </Link>
                    {canPay && (
                      <>
                        <button className="btn btn-ghost btn-sm" onClick={() => cancel(order)}>
                          {t('orders.cancel')}
                        </button>
                        <button className="btn btn-accent btn-sm" onClick={() => pay(order)}>
                          {t('orders.pay')}
                        </button>
                      </>
                    )}
                    {canConfirm && (
                      <button className="btn btn-primary btn-sm" onClick={() => confirm(order)}>
                        {t('orders.confirm')}
                      </button>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </AccountLayout>
  );
}
