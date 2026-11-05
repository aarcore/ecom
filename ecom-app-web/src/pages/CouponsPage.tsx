import { useEffect, useState } from 'react';
import { api, attempt } from '@/api/client';
import AccountLayout from '@/components/AccountLayout';
import { Empty } from '@/components/ui';
import { COUPON_STATUS_KEY } from '@/i18n/messages';
import { formatDate } from '@/i18n/locale';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { MemberCoupon } from '@/types';

interface CouponTemplate {
  id: number;
  name: string;
  type: string;
  amount: number;
  discount: number;
  minAmount: number;
  validDays: number;
  total: number;
  taken: number;
}

/**
 * A discount coupon reads as "90% of the price" in Chinese (9折), as "10% off"
 * in English and as "10%OFF" in Japanese. Showing the Chinese notation in
 * English would be meaningless, so the wording follows the locale while the
 * underlying rate stays the same.
 */
function faceValue(
  coupon: { type: string; amount: number; discount: number },
  locale: 'zh-CN' | 'en' | 'ja',
) {
  if (coupon.type === 'discount') {
    const rate = Number(coupon.discount ?? 10);
    const tenths = rate / 10;
    const off = Math.round((1 - tenths) * 100);
    if (locale === 'zh-CN') {
      return `${tenths.toFixed(rate % 1 === 0 ? 0 : 1)}折`;
    }
    return locale === 'ja' ? `${off}%OFF` : `${off}% off`;
  }
  return `¥${Number(coupon.amount).toFixed(0)}`;
}

export default function CouponsPage() {
  const { t, locale } = useT();
  const activeLocale = useLocale((s) => s.locale);
  const [tab, setTab] = useState<'mine' | 'center'>('mine');
  const [mine, setMine] = useState<MemberCoupon[]>([]);
  const [templates, setTemplates] = useState<CouponTemplate[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    const [wallet, available] = await Promise.all([
      attempt(() => api.get<MemberCoupon[]>('/app/coupons/mine')),
      attempt(() => api.get<CouponTemplate[]>('/app/coupons/available')),
    ]);
    if (wallet) setMine(wallet);
    if (available) setTemplates(available);
    setLoading(false);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeLocale]);

  const receive = async (couponId: number) => {
    const ok = await attempt(
      () => api.post(`/app/coupons/${couponId}/receive`),
      t('coupon.received'),
    );
    if (ok) load();
  };

  const usableCount = mine.filter((c) => c.status === 0).length;

  return (
    <AccountLayout
      title={t('coupon.title')}
      subtitle={t('coupon.subtitle', { count: usableCount })}
      activePath="/coupons"
      action={
        <div className="pills">
          <button className={`pill ${tab === 'mine' ? 'on' : ''}`} onClick={() => setTab('mine')}>
            {t('coupon.mine')}
          </button>
          <button className={`pill ${tab === 'center' ? 'on' : ''}`} onClick={() => setTab('center')}>
            {t('coupon.centre')}
          </button>
        </div>
      }
    >
      {loading ? (
        <div className="coupon-grid">
          {[0, 1, 2].map((i) => (
            <div key={i} className="skel" style={{ height: 104 }} />
          ))}
        </div>
      ) : tab === 'mine' ? (
        mine.length === 0 ? (
          <Empty
            glyph="◎"
            title={t('coupon.empty')}
            description={t('coupon.emptyHint')}
            action={
              <button className="btn btn-primary" onClick={() => setTab('center')}>
                {t('coupon.goGet')}
              </button>
            }
          />
        ) : (
          <div className="coupon-grid">
            {mine.map((coupon) => (
              <div
                key={coupon.id}
                className={`coupon-card ${coupon.status !== 0 ? 'used' : ''}`}
              >
                <div className="face">
                  <div>
                    <div className="amount">{faceValue(coupon, locale)}</div>
                    <div className="cond">
                      {t('coupon.minSpend', {
                        amount: `¥${Number(coupon.minAmount).toFixed(0)}`,
                      })}
                    </div>
                  </div>
                </div>
                <div className="info">
                  <div className="name">{coupon.name}</div>
                  <div className="expiry">
                    {coupon.endAt
                      ? t('coupon.validUntil', {
                          date: formatDate(coupon.endAt, locale, false),
                        })
                      : t('coupon.longTerm')}
                  </div>
                  <div className="foot">
                    <span
                      className={`tag ${
                        coupon.status === 0 ? 'accent' : coupon.status === 1 ? '' : 'warn'
                      }`}
                    >
                      {COUPON_STATUS_KEY[coupon.status]
                        ? t(COUPON_STATUS_KEY[coupon.status])
                        : coupon.statusText}
                    </span>
                    {coupon.usable !== undefined && coupon.status === 0 && (
                      <span className="muted" style={{ fontSize: 11.5 }}>
                        {coupon.usable
                          ? t('coupon.canSave', {
                              amount: `¥${Number(coupon.discountValue ?? 0).toFixed(2)}`,
                            })
                          : t('coupon.notUsable')}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )
      ) : templates.length === 0 ? (
        <Empty glyph="◎" title={t('coupon.none')} />
      ) : (
        <div className="coupon-grid">
          {templates.map((coupon) => {
            const remaining = coupon.total - coupon.taken;
            return (
              <div className="coupon-card" key={coupon.id}>
                <div className="face">
                  <div>
                    <div className="amount">{faceValue(coupon, locale)}</div>
                    <div className="cond">
                      {t('coupon.minSpend', {
                        amount: `¥${Number(coupon.minAmount).toFixed(0)}`,
                      })}
                    </div>
                  </div>
                </div>
                <div className="info">
                  <div className="name">{coupon.name}</div>
                  <div className="expiry">
                    {t('coupon.validDays', { days: coupon.validDays })}
                  </div>
                  <div className="foot">
                    <span className="muted" style={{ fontSize: 11.5 }}>
                      {remaining > 0
                        ? t('coupon.remaining', { count: remaining })
                        : t('coupon.soldOut')}
                    </span>
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={remaining <= 0}
                      onClick={() => receive(coupon.id)}
                    >
                      {t('coupon.receive')}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </AccountLayout>
  );
}
