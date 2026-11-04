import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { Price } from '@/components/ui';
import type { Order } from '@/types';

export default function OrderDetailPage() {
  const { orderNo } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [busy, setBusy] = useState(false);

  const load = () => {
    if (!orderNo) return;
    api.get<Order>(`/app/orders/${orderNo}`).then(setOrder).catch(() => undefined);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderNo]);

  const pay = async () => {
    if (!order) return;
    setBusy(true);
    const result = await attempt(
      () => api.post(`/app/orders/${order.orderNo}/pay`, { payType: 'mock' }),
      '支付成功',
    );
    if (result) load();
    setBusy(false);
  };

  const cancel = async () => {
    if (!order) return;
    const result = await attempt(
      () => api.post(`/app/orders/${order.orderNo}/cancel`, { reason: '用户取消' }),
      '订单已取消',
    );
    if (result) load();
  };

  const confirm = async () => {
    if (!order) return;
    const result = await attempt(
      () => api.post(`/app/orders/${order.orderNo}/confirm`),
      '已确认收货',
    );
    if (result) load();
  };

  if (!order) {
    return (
      <div className="page">
        <div className="skel" style={{ height: 120, marginBottom: 12 }} />
        <div className="skel" style={{ height: 200 }} />
      </div>
    );
  }

  const isPaid = [1, 2, 3].includes(order.status);

  return (
    <div style={{ paddingBottom: 90 }}>
      <div className="topbar">
        <button className="icon-btn" onClick={() => navigate('/orders')}>
          ‹
        </button>
        <div className="title">订单详情</div>
      </div>

      {/* status hero */}
      <div
        style={{
          background: isPaid
            ? 'linear-gradient(135deg, #ff5c4d, #ff2f4f)'
            : 'linear-gradient(135deg, #5b6478, #3b414c)',
          color: '#fff',
          padding: '22px 20px 20px',
        }}
      >
        <div style={{ fontSize: 21, fontWeight: 650 }}>{order.statusText}</div>
        <div style={{ fontSize: 12.5, opacity: 0.85, marginTop: 4 }}>
          {order.status === 0
            ? '请在 45 分钟内完成支付，超时订单将自动关闭'
            : order.status === 1
              ? '商家正在备货，请耐心等待'
              : order.status === 2
                ? '商品已发出，注意查收'
                : order.status === 3
                  ? '交易已完成，感谢您的支持'
                  : ''}
        </div>
      </div>

      <div className="page">
        {/* address */}
        <div className="list-card">
          <div className="row">
            <span style={{ fontSize: 17 }}>📍</span>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>
                {order.receiver}
                <span style={{ marginLeft: 10, fontWeight: 400, fontSize: 13, color: 'var(--ink-2)' }}>
                  {order.receiverMobile}
                </span>
              </div>
              <div className="muted" style={{ marginTop: 3 }}>{order.receiverAddress}</div>
            </div>
          </div>
        </div>

        {/* items */}
        <div className="list-card" style={{ marginTop: 10 }}>
          {order.items.map((item) => (
            <div key={item.id} style={{ display: 'flex', gap: 11, padding: 12, borderBottom: '1px solid var(--line)' }}>
              <img
                src={item.productCover}
                alt={item.productName}
                style={{ width: 66, height: 66, borderRadius: 9, objectFit: 'cover', background: '#f2f4f8' }}
                onClick={() => navigate(`/product/${item.productId}`)}
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
        </div>

        {/* amounts */}
        <div className="list-card" style={{ marginTop: 10, padding: '12px 14px' }}>
          <Line label="商品金额" value={`¥${Number(order.totalAmount).toFixed(2)}`} />
          <Line label="运费" value={`¥${Number(order.freightAmount).toFixed(2)}`} />
          {Number(order.discountAmount) > 0 && (
            <Line label="优惠" value={`-¥${Number(order.discountAmount).toFixed(2)}`} accent />
          )}
          <div className="row-between" style={{ paddingTop: 10, borderTop: '1px solid var(--line)' }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>实付金额</span>
            <Price value={order.payAmount} size="md" />
          </div>
        </div>

        {/* meta */}
        <div className="list-card" style={{ marginTop: 10, padding: '12px 14px' }}>
          <Line label="订单编号" value={order.orderNo} mono />
          <Line label="下单时间" value={new Date(order.createdAt).toLocaleString('zh-CN')} />
          {order.payType && <Line label="支付方式" value={order.payType} />}
          {order.expressNo && (
            <Line label="物流信息" value={`${order.expressCompany} ${order.expressNo}`} />
          )}
          {order.remark && <Line label="买家备注" value={order.remark} />}
        </div>
      </div>

      <div
        className="bottom-bar"
        style={{ position: 'fixed', left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: 480 }}
      >
        <button className="btn btn-outline btn-sm" onClick={() => navigate('/')}>
          继续逛逛
        </button>
        <div style={{ flex: 1 }} />
        {order.status === 0 && (
          <>
            <button className="btn btn-outline btn-sm" onClick={cancel}>
              取消订单
            </button>
            <button className="btn btn-primary btn-sm" disabled={busy} onClick={pay}>
              立即支付 ¥{Number(order.payAmount).toFixed(2)}
            </button>
          </>
        )}
        {order.status === 2 && (
          <button className="btn btn-primary btn-sm" onClick={confirm}>
            确认收货
          </button>
        )}
      </div>
    </div>
  );
}

function Line({
  label,
  value,
  mono,
  accent,
}: {
  label: string;
  value: string;
  mono?: boolean;
  accent?: boolean;
}) {
  return (
    <div className="row-between" style={{ fontSize: 13, marginBottom: 7 }}>
      <span className="muted">{label}</span>
      <span
        style={{
          fontFamily: mono ? 'ui-monospace, monospace' : undefined,
          color: accent ? 'var(--brand)' : undefined,
          maxWidth: '65%',
          textAlign: 'right',
          wordBreak: 'break-all',
        }}
      >
        {value}
      </span>
    </div>
  );
}
