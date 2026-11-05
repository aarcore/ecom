import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import type { MessageKey } from '@/i18n/messages';
import { useAuth } from '@/store/auth';
import { useTheme } from '@/store/theme';
import { useT } from '@/store/locale';
import { LocaleSwitch } from './LocaleSwitch';
import {
  IconBag,
  IconClose,
  IconMenu,
  IconMoon,
  IconSearch,
  IconSun,
  IconUser,
} from './icons';

/**
 * Nav items carry a dictionary key rather than a label, so the array is defined
 * once and the language switch simply changes what `t` returns.
 */
const NAV: { to: string; label: MessageKey; end?: boolean }[] = [
  { to: '/', label: 'nav.home', end: true },
  { to: '/shop', label: 'nav.shop' },
  { to: '/shop?categoryId=1', label: 'nav.bikes' },
  { to: '/shop?categoryId=2', label: 'nav.gear' },
  { to: '/shop?categoryId=5', label: 'nav.wheels' },
];

/**
 * NavLink only compares the pathname, so every `?categoryId=` variant of
 * /shop would light up at once. Compare the full location instead.
 */
function isNavActive(item: (typeof NAV)[number], pathname: string, search: string): boolean {
  if (item.end) return pathname === '/';
  const [itemPath, itemQuery] = item.to.split('?');
  if (pathname !== itemPath) return false;
  if (!itemQuery) {
    return !new URLSearchParams(search).has('categoryId');
  }
  const wanted = new URLSearchParams(itemQuery);
  const actual = new URLSearchParams(search);
  for (const [key, value] of wanted.entries()) {
    if (actual.get(key) !== value) return false;
  }
  return true;
}

export default function SiteHeader() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token, profile, cartCount, logout, refresh, refreshCart } = useAuth();
  const { theme, setTheme } = useTheme();
  const { t } = useT();

  const [keyword, setKeyword] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (token) {
      refresh();
      refreshCart();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, location.pathname]);

  useEffect(() => {
    setMenuOpen(false);
    setMobileOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    if (!menuOpen) return;
    const onClick = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [menuOpen]);

  const submitSearch = (event: React.FormEvent) => {
    event.preventDefault();
    const q = keyword.trim();
    navigate(q ? `/shop?keyword=${encodeURIComponent(q)}` : '/shop');
  };

  return (
    <header className="site-header">
      <div className="shell">
        <div className="bar">
          <button
            className="icon-btn mobile-menu-btn"
            onClick={() => setMobileOpen((v) => !v)}
            aria-label={mobileOpen ? t('nav.closeMenu') : t('nav.openMenu')}
            aria-expanded={mobileOpen}
          >
            {mobileOpen ? <IconClose size={20} /> : <IconMenu size={22} />}
          </button>

          <Link to="/" className="brand" aria-label={t('brand.home')}>
            <span className="mark">
              {/* the glyph stays the same character in every language */}
              <span>山</span>
            </span>
            <span className="hide-mobile">{t('brand.name')}</span>
          </Link>

          <nav className="nav">
            {NAV.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className={isNavActive(item, location.pathname, location.search) ? 'active' : ''}
              >
                {t(item.label)}
              </Link>
            ))}
          </nav>

          <form className="header-search" onSubmit={submitSearch} role="search">
            <div className="search-field">
              <IconSearch size={16} />
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder={t('nav.search')}
                aria-label={t('nav.searchLabel')}
              />
              <button type="submit" aria-label={t('nav.searchAction')}>
                <IconArrowSubmit />
              </button>
            </div>
          </form>

          <div className="header-actions">
            <LocaleSwitch />

            <div className="theme-toggle" role="group" aria-label={t('nav.theme')}>
              <button
                type="button"
                className={theme === 'light' ? 'on' : ''}
                aria-pressed={theme === 'light'}
                aria-label={t('nav.themeLight')}
                title={t('nav.themeLight')}
                onClick={() => setTheme('light')}
              >
                <IconSun />
              </button>
              <button
                type="button"
                className={theme === 'dark' ? 'on' : ''}
                aria-pressed={theme === 'dark'}
                aria-label={t('nav.themeDark')}
                title={t('nav.themeDark')}
                onClick={() => setTheme('dark')}
              >
                <IconMoon />
              </button>
            </div>

            <Link
              to="/cart"
              className="icon-btn"
              aria-label={
                cartCount ? t('nav.cartWithCount', { count: cartCount }) : t('nav.cart')
              }
            >
              <IconBag size={21} />
              {cartCount > 0 && <span className="count">{cartCount > 99 ? '99+' : cartCount}</span>}
            </Link>

            <div style={{ position: 'relative' }} ref={menuRef}>
              {token ? (
                <button
                  className="account-btn"
                  onClick={() => setMenuOpen((v) => !v)}
                  aria-haspopup="menu"
                  aria-expanded={menuOpen}
                >
                  <img className="avatar" src={profile?.avatar} alt="" />
                  <span className="hide-mobile">{profile?.nickname ?? t('nav.mine')}</span>
                </button>
              ) : (
                <button className="account-btn" onClick={() => navigate('/login')}>
                  <IconUser size={19} />
                  <span className="hide-mobile">{t('nav.login')}</span>
                </button>
              )}

              {menuOpen && (
                <div className="account-menu" role="menu">
                  <Link to="/account" role="menuitem">
                    {t('nav.account')} <span className="muted">{profile?.levelName}</span>
                  </Link>
                  <Link to="/orders" role="menuitem">
                    {t('nav.orders')}
                  </Link>
                  <Link to="/account/addresses" role="menuitem">
                    {t('nav.addresses')}
                  </Link>
                  <Link to="/coupons" role="menuitem">
                    {t('nav.coupons')}{' '}
                    <span className="muted">
                      {profile ? t('common.points', { count: profile.points }) : ''}
                    </span>
                  </Link>
                  <div className="sep" />
                  <button
                    role="menuitem"
                    onClick={() => {
                      logout();
                      navigate('/');
                    }}
                  >
                    {t('nav.logout')}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {mobileOpen && (
          <nav className="mobile-nav">
            {NAV.map((item) => (
              <Link key={item.to} to={item.to}>
                {t(item.label)}
              </Link>
            ))}
            <Link to="/orders">{t('nav.orders')}</Link>
            <Link to="/coupons">{t('nav.coupons')}</Link>
            <div className="mobile-locale">
              <LocaleSwitch compact />
            </div>
          </nav>
        )}
      </div>
    </header>
  );
}

/** Tiny submit glyph — keeps the search field from importing a second icon. */
function IconArrowSubmit() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  );
}
