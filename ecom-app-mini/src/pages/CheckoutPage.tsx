import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { Price } from '@/components/ui';
import { useAuth } from '@/store/auth';
import type { Address, CheckoutPreview, MemberCoupon, Order } from '@/types';

const PAY_OPTIONS = [
  { key: 'mock', label: '模拟支付', hint: '演示环境即时到账', glyph: '🧪' },
  { key: 'wechat', label: '微信支付', hint: '需配置商户号', glyph: '💚' },
  { key: 'alipay', label: '支付宝', hint: '需配置应用密钥', glyph: '💙' },
  { key: 'balance', label: '余额支付', hint: '使用账户余额', glyph: '💰' },
];

export default function CheckoutPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const refresh = useAuth((s) => s.refresh);
  const refreshCart = useAuth((s) => s.refreshCart);

  const state = (location.state ?? {}) as {
    source?: 'cart' | 'buy_now';
    items?: { skuId: number; quantity: number }[];
  };
  const source = state.source ?? 'cart';

  const [preview, setPreview] = useState<CheckoutPreview | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [couponId, setCouponId] = useState<number | null>(null);
  const [couponOptOut, setCouponOptOut] = useState(false);
  const [addressId, setAddressId] = useState<number | null>(null);
  const [payType, setPayType] = useState('mock');
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showAddress, setShowAddress] = useState(false);
  const [showCoupon, setShowCoupon] = useState(false);

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
  };

  useEffect(() => {
    loadPreview();
    api.get<Address[]>('/app/addresses').then(setAddresses).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const chooseAddress = async (id: number) => {
    setAddressId(id);
    setShowAddress(false);
    await loadPreview(id, couponId);
  };

  const chooseCoupon = async (id: number | null) => {
    setCouponId(id);
    setCouponOptOut(id === null);
    setShowCoupon(false);
    await loadPreview(addressId, id, id === null);
  };

  const submit = async () => {
    if (!preview || !addressId) {
      await attempt(() => Promise.reject(new Error('请先选择收货地址')));
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
      '下单成功',
    );

    if (order) {
      // pay immediately for the demo flow so the customer lands on a success page
      await attempt(() => api.post(`/app/orders/${order.orderNo}/pay`, { payType }));
      refresh();
      refreshCart();
      navigate(`/order/${order.orderNo}`, { replace: true });
    }
    setSubmitting(false);
  };

  const selectedAddress = addresses.find((a) => a.id === addressId) ?? preview?.address ?? null;
  const usableCoupons = preview?.availableCoupons.filter((c) => c.usable) ?? [];
  const appliedCoupon = usableCoupons.find((c) => c.couponId === couponId);

  return (
    <div style={{ paddingBottom: 80 }}>
      <div className="topbar">
        <button className="icon-btn" onClick={() => navigate(-1)}>
          ‹
        </button>
        <div className="title">确认订单</div>
      </div>

      <div className="page">
        {/* address */}
        <div className="list-card" onClick={() => setShowAddress(true)}>
          {selectedAddress ? (
            <div className="row">
              <span style={{ fontSize: 17 }}>📍</span>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 600 }}>
                  {selectedAddress.receiver}
                  <span style={{ marginLeft: 10, fontWeight: 400, color: 'var(--ink-2)', fontSize: 13 }}>
                    {selectedAddress.mobile}
                  </span>
                </div>
                <div className="muted" style={{ marginTop: 3 }}>
                  {selectedAddress.fullAddress ??
                    `${selectedAddress.province ?? ''}${selectedAddress.city ?? ''}${selectedAddress.district ?? ''}${selectedAddress.detail}`}
                </div>
              </div>
              <span className="arrow">›</span>
            </div>
          ) : (
            <div className="row">
              <span style={{ fontSize: 17 }}>📍</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 600 }}>请选择收货地址</div>
                <div className="muted">还没有地址？点击新增</div>
              </div>
              <span className="arrow">›</span>
            </div>
          )}
        </div>

        {/* items */}
        <div className="list-card" style={{ marginTop: 10 }}>
          {preview?.items.map((item) => (
            <div key={item.skuId} style={{ display: 'flex', gap: 11, padding: 12, borderBottom: '1px solid var(--line)' }}>
              <img
                src={item.cover}
                alt={item.productName}
                style={{ width: 62, height: 62, borderRadius: 9, objectFit: 'cover', background: '#f2f4f8' }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, lineHeight: 1.4 }}>{item.productName}</div>
                <div className="muted" style={{ marginTop: 2 }}>{item.specValues}</div>
                <div className="row-between" style={{ marginTop: 6 }}>
                  <Price value={item.price} size="sm" />
                  <span className="muted">×{item.quantity}</span>
                </div>
              </div>
            </div>
          ))}
          {!preview && (
            <div className="stack" style={{ padding: 12 }}>
              <div className="skel" style={{ height: 62 }} />
            </div>
          )}
        </div>

        {/* coupon */}
        <div className="list-card" style={{ marginTop: 10 }}>
          <div
            className="row"
            onClick={() => {
              if (usableCoupons.length) setShowCoupon(true);
            }}
            style={{ cursor: usableCoupons.length ? 'pointer' : 'default' }}
          >
            <span className="label">优惠券</span>
            <span className="value" style={{ color: appliedCoupon ? 'var(--brand)' : 'var(--ink-3)' }}>
              {appliedCoupon
                ? `-¥${Number(preview?.discountAmount ?? 0).toFixed(2)}`
                : usableCoupons.length
                  ? `${usableCoupons.length} 张可用`
                  : '暂无可用'}
            </span>
            <span className="arrow">›</span>
          </div>

          <div className="row">
            <span className="label">配送</span>
            <span className="value muted">
              {Number(preview?.freightAmount ?? 0) === 0 ? '包邮' : `¥${Number(preview?.freightAmount).toFixed(2)}`}
            </span>
          </div>

          <div className="row">
            <span className="label">备注</span>
            <input
              value={remark}
              onChange={(event) => setRemark(event.target.value)}
              placeholder="选填，例如 请放快递柜"
              style={{
                flex: 1,
                border: 'none',
                outline: 'none',
                textAlign: 'right',
                fontSize: 13,
                background: 'transparent',
              }}
            />
          </div>
        </div>

        {/* payment */}
        <div className="list-card" style={{ marginTop: 10 }}>
          <div className="row" style={{ borderBottom: '1px solid var(--line)' }}>
            <span style={{ fontSize: 13.5, fontWeight: 600 }}>支付方式</span>
          </div>
          {PAY_OPTIONS.map((option) => (
            <div
              key={option.key}
              className="row"
              onClick={() => setPayType(option.key)}
              style={{ cursor: 'pointer' }}
            >
              <span style={{ fontSize: 17 }}>{option.glyph}</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13.5 }}>{option.label}</div>
                <div className="muted">{option.hint}</div>
              </div>
              <div className={`checkbox ${payType === option.key ? 'on' : ''}`}>
                {payType === option.key ? '✓' : ''}
              </div>
            </div>
          ))}
        </div>

        {/* totals */}
        <div className="list-card" style={{ marginTop: 10, padding: '12px 14px' }}>
          <div className="row-between" style={{ fontSize: 13, marginBottom: 6 }}>
            <span className="muted">商品金额</span>
            <span>¥{Number(preview?.totalAmount ?? 0).toFixed(2)}</span>
          </div>
          <div className="row-between" style={{ fontSize: 13, marginBottom: 6 }}>
            <span className="muted">运费</span>
            <span>¥{Number(preview?.freightAmount ?? 0).toFixed(2)}</span>
          </div>
          <div className="row-between" style={{ fontSize: 13, marginBottom: 10 }}>
            <span className="muted">优惠</span>
            <span style={{ color: 'var(--brand)' }}>-¥{Number(preview?.discountAmount ?? 0).toFixed(2)}</span>
          </div>
          <div className="row-between" style={{ paddingTop: 10, borderTop: '1px solid var(--line)' }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>应付金额</span>
            <Price value={preview?.payAmount ?? 0} size="md" />
          </div>
        </div>
      </div>

      <div
        className="bottom-bar"
        style={{ position: 'fixed', left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: 480 }}
      >
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>合计</div>
          <Price value={preview?.payAmount ?? 0} />
        </div>
        <button className="btn btn-primary" style={{ minWidth: 132 }} disabled={submitting || !addressId} onClick={submit}>
          {submitting ? '提交中…' : '提交订单'}
        </button>
      </div>

      {/* address picker */}
      {showAddress && (
        <div className="sheet-mask" onClick={() => setShowAddress(false)}>
          <div className="sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-title">选择收货地址</div>
            <div className="stack">
              {addresses.map((address) => (
                <div
                  key={address.id}
                  className="row"
                  style={{
                    border: `1px solid ${address.id === addressId ? 'var(--brand)' : 'var(--line)'}`,
                    borderRadius: 12,
                    marginBottom: 8,
                  }}
                  onClick={() => chooseAddress(address.id)}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600 }}>
                      {address.receiver}
                      <span style={{ marginLeft: 10, fontWeight: 400, fontSize: 13, color: 'var(--ink-2)' }}>
                        {address.mobile}
                      </span>
                    </div>
                    <div className="muted" style={{ marginTop: 3 }}>{address.fullAddress}</div>
                  </div>
                  {address.isDefault === 1 && <span className="pill" style={{ position: 'static' }}>默认</span>}
                </div>
              ))}
              {!addresses.length && <div className="empty" style={{ padding: 24 }}>还没有收货地址</div>}
            </div>
            <button className="btn btn-outline btn-block" style={{ marginTop: 12 }} onClick={() => navigate('/address')}>
              管理收货地址
            </button>
          </div>
        </div>
      )}

      {/* coupon picker */}
      {showCoupon && (
        <div className="sheet-mask" onClick={() => setShowCoupon(false)}>
          <div className="sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-title">选择优惠券</div>
            <div className="stack">
              {usableCoupons.map((coupon: MemberCoupon) => (
                <div
                  key={coupon.id}
                  className="row"
                  style={{
                    border: `1px solid ${coupon.couponId === couponId ? 'var(--brand)' : 'var(--line)'}`,
                    borderRadius: 12,
                  }}
                  onClick={() => chooseCoupon(coupon.couponId)}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, fontSize: 13.5 }}>{coupon.name}</div>
                    <div className="muted">
                      满 {Number(coupon.minAmount).toFixed(0)} 可用 · 可减 ¥
                      {Number(coupon.discountValue ?? 0).toFixed(2)}
                    </div>
                  </div>
                  <div className={`checkbox ${coupon.couponId === couponId ? 'on' : ''}`}>
                    {coupon.couponId === couponId ? '✓' : ''}
                  </div>
                </div>
              ))}
            </div>
            <button
              className="btn btn-outline btn-block"
              style={{ marginTop: 12 }}
              disabled={couponId === null}
              onClick={() => chooseCoupon(null)}
            >
              不使用优惠券
            </button>
            <p className="muted" style={{ textAlign: 'center', marginTop: 10 }}>
              不选择时系统会自动匹配最优惠的一张，可在此关闭
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
