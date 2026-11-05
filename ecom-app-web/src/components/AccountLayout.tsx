import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';
import { IconArrowRight } from './icons';
import type { MessageKey } from '@/i18n/messages';
import type { ReactNode } from 'react';

const LINKS: { to: string; label: MessageKey }[] = [
  { to: '/account', label: 'account.overview' },
  { to: '/orders', label: 'account.orders' },
  { to: '/coupons', label: 'account.coupons' },
  { to: '/account/addresses', label: 'account.addresses' },
  { to: '/account/profile', label: 'account.profile' },
];

/** Two-column shell shared by every page under /account and /orders. */
export default function AccountLayout({
  title,
  subtitle,
  action,
  children,
  activePath,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
  activePath: string;
}) {
  const navigate = useNavigate();
  const { token, profile, logout } = useAuth();
  const { t } = useT();

  if (!token) {
    return (
      <div className="shell">
        <div className="empty">
          <div className="glyph">◎</div>
          <h3>{t('account.loginRequired')}</h3>
          <p>{t('account.loginHint')}</p>
          <button className="btn btn-primary" onClick={() => navigate('/login')}>
            {t('account.goLogin')}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="shell">
      <div className="account-layout">
        <aside className="account-nav">
          <div className="who">
            <img src={profile?.avatar} alt="" />
            <div>
              <div className="name">{profile?.nickname ?? t('account.member')}</div>
              <div className="muted" style={{ fontSize: 11.5 }}>
                {profile?.levelName} · {t('common.points', { count: profile?.points ?? 0 })}
              </div>
            </div>
          </div>

          <div className="links">
            {LINKS.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className={activePath === link.to ? 'active' : ''}
              >
                {t(link.label)}
              </Link>
            ))}
          </div>

          <button
            className="signout"
            onClick={() => {
              logout();
              navigate('/');
            }}
          >
            {t('account.logout')}
          </button>
        </aside>

        <main>
          <div className="section-head">
            <div>
              <h2>{title}</h2>
              {subtitle && <div className="muted" style={{ marginTop: 6 }}>{subtitle}</div>}
            </div>
            {action}
          </div>
          {children}
        </main>
      </div>
    </div>
  );
}

/** Small reused action: continue shopping. */
export function ContinueShopping() {
  const { t } = useT();
  return (
    <Link to="/shop" className="btn-link row" style={{ gap: 6, alignSelf: 'center' }}>
      {t('cart.continue')} <IconArrowRight size={13} />
    </Link>
  );
}
