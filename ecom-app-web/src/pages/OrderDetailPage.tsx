import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { payApi, submitGatewayForm } from '@/api/pay';
import { PaymentQr } from '@/components/PaymentQr';
import type { PayChannelOption, PayLaunch } from '@/types/pay';
import AccountLayout from '@/components/AccountLayout';
import { Photo } from '@/components/Photo';
import { Price } from '@/components/ui';
import { ORDER_STATUS_HINT_KEY, ORDER_STATUS_KEY } from '@/i18n/messages';
import { formatDate } from '@/i18n/locale';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { Order } from '@/types';

export default function OrderDetailPage() {
  const { orderNo } = useParams();
  const navigate = useNavigate();
  const { refresh } = useAuth();
  const { t, locale } = useT();
  const activeLocale = useLocale((s) => s.locale);
  const [order, setOrder] = useState<Order | null>(null);
  const [busy, setBusy] = useState(false);
  const [channels, setChannels] = useState<PayChannelOption[]>([]);
  const [channel, setChannel] = useState('');
  const [launch, setLaunch] = useState<PayLaunch | null>(null);

  const load = () => {
    if (!orderNo) return;
    api.get<Order>(`/app/orders/${orderNo}`).then(setOrder).catch(() => undefined);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderNo, activeLocale]);

  useEffect(() => {
    // only worth fetching when there is something to pay
    if (order?.status !== 0) return;
    payApi
      .channels()
      .then((list) => {
        setChannels(list);
        setChannel((current) => current || list[0]?.code || '');
      })
      .catch(() => undefined);
  }, [order?.status]);

  if (!order) {
    return (
      <AccountLayout title={t('orders.detailTitle')} activePath="/orders">
        <div className="skel" style={{ height: 300 }} />
      </AccountLayout>
    );
  }

  const statusKey = ORDER_STATUS_KEY[order.status];
  const hintKey = ORDER_STATUS_HINT_KEY[order.status];

  const pay = async () => {
    if (!channel) return;
    setBusy(true);
    const result = await attempt(() => payApi.launch(order.orderNo, channel));
    setBusy(false);
    if (!result) return;

    if (result.mode === 'SYNC') {
      load();
      refresh();
      return;
    }
    if (result.mode === 'REDIRECT' && result.url) {
      if (result.url.trimStart().startsWith('<')) {
        submitGatewayForm(result.url);
        return;
      }
      window.location.assign(result.url);
      return;
    }
    // QR / SDK: show it here and poll for the result
    setLaunch(result);
  };

  const cancel = async () => {
    const ok = await attempt(
      () =>
        api.post(`/app/orders/${order.orderNo}/cancel`, { reason: t('orders.cancelReason') }),
      t('orders.cancelled'),
    );
    if (ok) load();
  };

  const confirm = async () => {
    const ok = await attempt(
      () => api.post(`/app/orders/${order.orderNo}/confirm`),
      t('orders.confirmed'),
    );
    if (ok) load();
  };

  const money = (value: number | string) => `¥${Number(value).toFixed(2)}`;

  return (
    <AccountLayout
      title={t('orders.detailTitle')}
      activePath="/orders"
      action={
        <button className="btn-link" onClick={() => navigate('/orders')}>
          {t('orders.backToList')}
        </button>
      }
    >
      <div className="order-status-hero">
        <div>
          <h1>{statusKey ? t(statusKey) : order.statusText}</h1>
          <div className="muted">{hintKey ? t(hintKey) : ''}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="muted" style={{ marginBottom: 4 }}>{t('orders.paidAmount')}</div>
          <Price value={order.payAmount} size={26} accent />
        </div>
      </div>

      <div className="order-detail-grid">
        <div>
          <section className="checkout-block" style={{ borderTop: 'none', paddingTop: 0 }}>
            <h3>{t('orders.goods')}</h3>
            {order.items.map((item) => (
              <div className="media-row" key={item.id} style={{ padding: '10px 0' }}>
                <Link to={`/product/${item.productId}`} className="media">
                  <Photo src={item.productCover} />
                </Link>
                <div className="body">
                  <Link to={`/product/${item.productId}`} style={{ fontSize: 14 }}>
                    {item.productName}
                  </Link>
                  <div className="muted" style={{ marginTop: 3 }}>
                    {item.specValues} · × {item.quantity}
                  </div>
                  <div className="row-between" style={{ marginTop: 'auto', paddingTop: 8 }}>
                    <Price value={item.price} size={14} />
                    <span className="tabular" style={{ fontSize: 13 }}>
                      {money(item.totalAmount)}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </section>

          <section className="checkout-block">
            <h3>{t('orders.receiverInfo')}</h3>
            <div style={{ fontSize: 13.5, lineHeight: 1.75 }}>
              <div style={{ fontWeight: 600 }}>
                {order.receiver}
                <span style={{ fontWeight: 400, color: 'var(--text-3)', marginLeft: 10, fontSize: 12.5 }}>
                  {order.receiverMobile}
                </span>
              </div>
              <div className="muted">{order.receiverAddress}</div>
              {order.remark && (
                <div className="muted" style={{ marginTop: 6 }}>
                  {t('orders.remark', { remark: order.remark })}
                </div>
              )}
            </div>
          </section>

          <section className="checkout-block">
            <h3>{t('orders.info')}</h3>
            <dl className="meta-list">
              <div className="row-item">
                <dt>{t('orders.orderNo')}</dt>
                <dd style={{ fontFamily: 'var(--font-mono)', fontSize: 12.5 }}>{order.orderNo}</dd>
              </div>
              <div className="row-item">
                <dt>{t('orders.createdAt')}</dt>
                <dd>{formatDate(order.createdAt, locale)}</dd>
              </div>
              {order.payType && (
                <div className="row-item">
                  <dt>{t('orders.payType')}</dt>
                  <dd>{order.payType}</dd>
                </div>
              )}
              {order.expressNo && (
                <div className="row-item">
                  <dt>{t('orders.logistics')}</dt>
                  <dd>
                    {order.expressCompany} · {order.expressNo}
                  </dd>
                </div>
              )}
            </dl>
          </section>
        </div>

        <aside>
          <div className="summary" style={{ position: 'static' }}>
            <h3>{t('orders.amountDetail')}</h3>
            <div className="summary-line">
              <span>{t('orders.goodsAmount')}</span>
              <span className="amt">{money(order.totalAmount)}</span>
            </div>
            <div className="summary-line">
              <span>{t('orders.freight')}</span>
              <span className="amt">{money(order.freightAmount)}</span>
            </div>
            <div className="summary-line">
              <span>{t('orders.discount')}</span>
              <span className="amt" style={{ color: 'var(--flare)' }}>
                -{money(order.discountAmount)}
              </span>
            </div>
            <div className="summary-total">
              <span>{t('orders.paid')}</span>
              <Price value={order.payAmount} size={20} accent />
            </div>

            <div className="stack-sm" style={{ marginTop: 22 }}>
              {order.status === 0 && (
                <>
                  {channels.length > 0 && (
                    <div className="pay-pick">
                      {channels.map((option) => (
                        <button
                          key={option.code}
                          className={`chip ${channel === option.code ? 'on' : ''}`}
                          onClick={() => setChannel(option.code)}
                          disabled={busy || Boolean(launch)}
                        >
                          {option.name}
                        </button>
                      ))}
                    </div>
                  )}
                  <button
                    className="btn btn-accent btn-block"
                    disabled={busy || !channel || Boolean(launch)}
                    onClick={pay}
                  >
                    {t('orders.payNow', { amount: money(order.payAmount) })}
                  </button>
                  <button className="btn btn-outline btn-block" onClick={cancel}>
                    {t('orders.cancel')}
                  </button>
                </>
              )}
              {order.status === 2 && (
                <button className="btn btn-primary btn-block" onClick={confirm}>
                  {t('orders.confirm')}
                </button>
              )}
              <Link to="/shop" className="btn btn-outline btn-block">
                {t('orders.continue')}
              </Link>
            </div>
          </div>
        </aside>
      </div>

      {launch && launch.mode === 'QR_CODE' && (
        <LaunchQr launch={launch} onPaid={() => {
          setLaunch(null);
          load();
          refresh();
        }} />
      )}
    </AccountLayout>
  );
}

/**
 * QR waiting panel with polling.
 *
 * Polls the order rather than the payment: the order status is the single source
 * of truth, so this works the same for any channel that settles asynchronously.
 */
function LaunchQr({
  launch,
  onPaid,
}: {
  launch: Extract<PayLaunch, { mode: 'QR_CODE' }>;
  onPaid: () => void;
}) {
  const { t } = useT();
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    let stopped = false;
    const timer = window.setInterval(async () => {
      try {
        const order = await api.get<{ status: number }>(`/app/orders/${launch.orderNo}`);
        if (!stopped && order.status !== 0) {
          window.clearInterval(timer);
          onPaid();
        }
      } catch {
        /* retry on the next tick */
      }
      setSeconds((prev) => prev + 1);
    }, 2500);

    return () => {
      stopped = true;
      window.clearInterval(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [launch.orderNo]);

  return (
    <div className="modal-mask" onClick={() => undefined}>
      <div className="modal pay-pending">
        <div className="modal-title">{t('checkout.scanTitle')}</div>
        <p className="muted" style={{ marginTop: 0 }}>{t('checkout.scanHint')}</p>
        <PaymentQr content={launch.qrContent} size={232} />
        <div className="pay-pending-foot">
          <span className="muted">{t('checkout.awaitingPayment', { seconds })}</span>
        </div>
      </div>
    </div>
  );
}
