import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import AddressModal from '@/components/AddressModal';
import { PaymentQr } from '@/components/PaymentQr';
import { submitGatewayForm } from '@/api/pay';
import { payApi } from '@/api/pay';
import type { PayChannelOption, PayLaunch } from '@/types/pay';
import { Photo } from '@/components/Photo';
import { Empty, Price } from '@/components/ui';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { Address, CheckoutPreview, MemberCoupon, Order } from '@/types';

/**
 * Channel glyph, keyed by provider rather than by display name so an operator
 * renaming a channel in the console does not lose its icon.
 */
function channelGlyph(code: string): string {
  switch (code) {
    case 'stripe':
      return 'S';
    case 'wechat':
      return '💬';
    case 'alipay':
      return '🅐';
    case 'balance':
      return '◈';
    default:
      return '🧪';
  }
}

export default function CheckoutPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token, refresh, refreshCart } = useAuth();
  const { t } = useT();
  const locale = useLocale((s) => s.locale);

  const state = (location.state ?? {}) as {
    source?: 'cart' | 'buy_now';
    items?: { skuId: number; quantity: number }[];
  };
  const source = state.source ?? 'cart';

  const [preview, setPreview] = useState<CheckoutPreview | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [addressId, setAddressId] = useState<number | null>(null);
  const [couponId, setCouponId] = useState<number | null>(null);
  const [couponOptOut, setCouponOptOut] = useState(false);
  const [payType, setPayType] = useState('');
  const [channels, setChannels] = useState<PayChannelOption[]>([]);
  /** Set when a redirect or QR step is in progress. */
  const [launch, setLaunch] = useState<PayLaunch | null>(null);
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [newAddressOpen, setNewAddressOpen] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const loadPreview = async (
    nextAddressId?: number | null,
    nextCouponId?: number | null,
    optOut?: boolean,
  ) => {
    const skip = optOut ?? couponOptOut;
    const result = await attempt(() =>
      api.post<CheckoutPreview>('/app/orders/preview', {
        source,
        items: state.items,
        addressId: nextAddressId ?? undefined,
        couponId: nextCouponId ?? undefined,
        couponOptOut: skip,
      }),
    );
    if (result) {
      setPreview(result);
      if (result.address && !nextAddressId) setAddressId(result.address.id);
      setCouponId(result.selectedCouponId ?? null);
    }
    setLoaded(true);
  };

  useEffect(() => {
    if (!token) {
      navigate('/login', { state: { from: '/checkout' } });
      return;
    }
    loadPreview();
    api.get<Address[]>('/app/addresses').then(setAddresses).catch(() => undefined);
    // Channels are operator-managed, so they are fetched rather than hardcoded.
    payApi.channels().then((list) => {
      setChannels(list);
      // preselect the first available so the submit button is never dead
      setPayType((current) => current || list[0]?.code || '');
    }).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, locale]);

  /**
   * Creates the order, then opens a payment on the chosen channel.
   *
   * Order first, payment second, on purpose: if the channel rejects the request
   * the customer still has an order they can pay later, and the unpaid-order
   * scheduler closes it after 45 minutes. Creating a payment before the order
   * would risk charging for something that does not exist.
   */
  const submit = async () => {
    if (!preview || !addressId) {
      await attempt(() => Promise.reject(new Error(t('checkout.chooseAddressFirst'))));
      return;
    }
    setSubmitting(true);
    const order = await attempt(
      () =>
        api.post<Order>('/app/orders', {
          source,
          items: state.items,
          addressId,
          couponId: couponId ?? undefined,
          couponOptOut,
          remark,
          payType,
        }),
      t('checkout.success'),
    );
    if (!order) {
      setSubmitting(false);
      return;
    }

    refresh();
    refreshCart();

    const result = await attempt(() => payApi.launch(order.orderNo, payType));
    if (!result) {
      // The order exists but the channel failed: send them to it so they can
      // retry with a different method rather than losing the order.
      navigate(`/orders/${order.orderNo}`, { replace: true });
      return;
    }

    if (result.mode === 'SYNC') {
      navigate(`/orders/${order.orderNo}`, { replace: true });
      return;
    }

    if (result.mode === 'REDIRECT' && result.url) {
      // Alipay returns a full auto-submitting form; Stripe returns a plain URL.
      if (result.url.trimStart().startsWith('<')) {
        setLaunch(result);
        submitGatewayForm(result.url);
        return;
      }
      window.location.assign(result.url);
      return;
    }

    // QR and SDK both need an in-page step, so stay put and render it.
    setLaunch(result);
    setSubmitting(false);
  };

  if (!loaded || !preview) {
    return (
      <div className="shell">
        <div className="checkout-layout">
          <div>
            <div className="skel" style={{ height: 90, marginBottom: 20 }} />
            <div className="skel" style={{ height: 160 }} />
          </div>
          <div className="skel" style={{ height: 300 }} />
        </div>
      </div>
    );
  }

  const selectedAddress = addresses.find((a) => a.id === addressId) ?? preview.address;
  const usableCoupons = preview.availableCoupons.filter((c) => c.usable);
  const appliedCoupon = usableCoupons.find((c) => c.couponId === couponId);

  return (
    <div className="shell">
      <div style={{ paddingTop: 40 }}>
        <h1 style={{ fontSize: 'clamp(24px, 3vw, 32px)' }}>{t('checkout.title')}</h1>
      </div>

      <div className="checkout-layout">
        <div>
          {/* ------------------------------------------------------ address */}
          <section className="checkout-block">
            <h3>
              {t('checkout.address')}
              <button className="btn-link" onClick={() => setNewAddressOpen(true)}>
                {t('checkout.addAddress')}
              </button>
            </h3>

            {addresses.length === 0 ? (
              <Empty
                glyph="◎"
                title={t('checkout.noAddress')}
                description={t('checkout.noAddressHint')}
              />
            ) : (
              <div className="address-list">
                {addresses.map((address) => (
                  <button
                    key={address.id}
                    className={`address-card ${address.id === addressId ? 'on' : ''}`}
                    onClick={async () => {
                      setAddressId(address.id);
                      await loadPreview(address.id, couponId);
                    }}
                  >
                    <span className={`checkbox ${address.id === addressId ? 'on' : ''}`}>✓</span>
                    <span style={{ flex: 1, minWidth: 0 }}>
                      <div className="who">
                        {address.receiver}
                        <span style={{ fontWeight: 400, marginLeft: 10, color: 'var(--text-3)', fontSize: 12.5 }}>
                          {address.mobile}
                        </span>
                        {address.isDefault === 1 && (
                          <span className="tag" style={{ marginLeft: 10 }}>
                            {t('checkout.default')}
                          </span>
                        )}
                      </div>
                      <div className="where">{address.fullAddress}</div>
                    </span>
                  </button>
                ))}
              </div>
            )}
          </section>

          {/* -------------------------------------------------------- items */}
          <section className="checkout-block">
            <h3>{t('checkout.items', { count: preview.items.length })}</h3>
            <div>
              {preview.items.map((item) => (
                <div className="media-row" key={item.skuId} style={{ padding: '12px 0' }}>
                  <div className="media">
                    <Photo src={item.cover} />
                  </div>
                  <div className="body">
                    <div style={{ fontSize: 14, lineHeight: 1.45 }}>{item.productName}</div>
                    {item.specValues && <div className="spec muted" style={{ marginTop: 4 }}>{item.specValues}</div>}
                    <div className="row-between" style={{ marginTop: 'auto', paddingTop: 8 }}>
                      <Price value={item.price} size={14} />
                      <span className="muted">× {item.quantity}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* ------------------------------------------------------ payment */}
          <section className="checkout-block">
            <h3>{t('checkout.payment')}</h3>
            {channels.length === 0 ? (
              <div className="muted">{t('checkout.noChannels')}</div>
            ) : (
              <div className="pay-options">
                {channels.map((option) => (
                  <button
                    key={option.code}
                    className={`pay-option ${payType === option.code ? 'on' : ''}`}
                    onClick={() => setPayType(option.code)}
                    disabled={Boolean(launch)}
                  >
                    <span className="glyph">{channelGlyph(option.code)}</span>
                    <span className="label">
                      {option.name}
                      <div className="hint">
                        {option.redirect
                          ? t('checkout.willRedirect')
                          : t('checkout.inline')}
                      </div>
                    </span>
                    <span className="radio" />
                  </button>
                ))}
              </div>
            )}
          </section>

          {/* ------------------------------------------------------- remark */}
          <section className="checkout-block">
            <h3>{t('checkout.remark')}</h3>
            <textarea
              className="textarea"
              rows={3}
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              placeholder={t('checkout.remarkPlaceholder')}
            />
          </section>
        </div>

        {/* ---------------------------------------------------------- summary */}
        <aside className="summary">
          <h3>{t('checkout.summary')}</h3>

          <div style={{ marginBottom: 18 }}>
            <div className="muted" style={{ marginBottom: 8 }}>{t('checkout.coupon')}</div>
            {usableCoupons.length === 0 ? (
              <div className="muted">{t('checkout.noCoupon')}</div>
            ) : (
              <div>
                {usableCoupons.map((coupon: MemberCoupon) => (
                  <button
                    key={coupon.id}
                    className="coupon-row"
                    onClick={() => {
                      const next = couponId === coupon.couponId ? null : coupon.couponId;
                      setCouponId(next);
                      setCouponOptOut(next === null);
                      loadPreview(addressId, next, next === null);
                    }}
                  >
                    <span>
                      <span className={`checkbox ${couponId === coupon.couponId ? 'on' : ''}`} style={{ marginRight: 9 }}>
                        ✓
                      </span>
                      {coupon.name}
                    </span>
                    <span className="amt">-¥{Number(coupon.discountValue ?? 0).toFixed(2)}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="summary-line">
            <span>{t('checkout.goods')}</span>
            <span className="amt">¥{Number(preview.totalAmount).toFixed(2)}</span>
          </div>
          <div className="summary-line">
            <span>{t('checkout.freight')}</span>
            <span className="amt">
              {Number(preview.freightAmount) === 0
                ? t('checkout.freeShipping')
                : `¥${Number(preview.freightAmount).toFixed(2)}`}
            </span>
          </div>
          <div className="summary-line">
            <span>
              {t('checkout.discount')}
              {appliedCoupon ? ` (${appliedCoupon.name})` : ''}
            </span>
            <span className="amt" style={{ color: 'var(--flare)' }}>
              -¥{Number(preview.discountAmount).toFixed(2)}
            </span>
          </div>

          <div className="summary-total">
            <span>{t('checkout.payable')}</span>
            <Price value={preview.payAmount} size={22} accent />
          </div>
          <p className="summary-note">{t('checkout.submitNote')}</p>

          <button
            className="btn btn-accent btn-block"
            disabled={submitting || !addressId}
            onClick={submit}
          >
            {submitting ? t('common.submitting') : t('checkout.submit')}
          </button>
          <button
            className="btn btn-ghost btn-block"
            style={{ marginTop: 6 }}
            onClick={() => navigate(-1)}
          >
            {t('common.back')}
          </button>
        </aside>
      </div>

      {launch && launch.mode === 'QR_CODE' && (
        <PayInProgress
          launch={launch}
          onDone={(orderNo) => navigate(`/orders/${orderNo}`, { replace: true })}
        />
      )}

      {newAddressOpen && (
        <AddressModal
          onClose={() => setNewAddressOpen(false)}
          onSaved={async (saved) => {
            const list = await api.get<Address[]>('/app/addresses');
            setAddresses(list);
            setAddressId(saved.id);
            setNewAddressOpen(false);
            await loadPreview(saved.id, couponId);
          }}
        />
      )}
    </div>
  );
}

/**
 * Waiting screen for QR payments.
 *
 * Polls the order rather than trusting the customer to come back: they paid on
 * their phone with the browser still open here, so this page is the only place
 * that will notice. Polling stops on the first non-pending status, so a cancelled
 * or already-paid order resolves immediately instead of after the full timeout.
 */
function PayInProgress({
  launch,
  onDone,
}: {
  launch: Extract<PayLaunch, { mode: 'QR_CODE' }>;
  onDone: (orderNo: string) => void;
}) {
  const { t } = useT();
  const [seconds, setSeconds] = useState(0);
  const [expired, setExpired] = useState(false);

  const { orderNo } = launch;

  useEffect(() => {
    if (!orderNo) {
      // Without an order reference there is nothing to poll; fall back to a
      // timed prompt so the customer is not stranded on a dead panel.
      const timer = window.setInterval(() => {
        setSeconds((prev) => {
          if (prev >= 120) {
            window.clearInterval(timer);
            setExpired(true);
          }
          return prev + 1;
        });
      }, 1000);
      return () => window.clearInterval(timer);
    }

    let stopped = false;
    const timer = window.setInterval(async () => {
      try {
        const order = await api.get<{ status: number }>(`/app/orders/${orderNo}`);
        if (stopped) return;
        if (order.status !== 0) {
          window.clearInterval(timer);
          onDone(orderNo);
        }
      } catch {
        /* transient network errors simply retry on the next tick */
      }
      setSeconds((prev) => prev + 1);
    }, 2500);

    return () => {
      stopped = true;
      window.clearInterval(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderNo]);

  return (
    <div className="modal-mask">
      <div className="modal pay-pending">
        <div className="modal-title">{t('checkout.scanTitle')}</div>
        <p className="muted" style={{ marginTop: 0 }}>{t('checkout.scanHint')}</p>
        <PaymentQr content={launch.qrContent} size={232} />
        <div className="pay-pending-foot">
          <span className="muted">
            {t('checkout.awaitingPayment', { seconds })}
          </span>
        </div>
        {expired && <div className="warn">{t('checkout.qrExpired')}</div>}
      </div>
    </div>
  );
}
