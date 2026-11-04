import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/store/auth';

const TABS = [
  { key: '/', label: '首页', glyph: '🏠' },
  { key: '/category', label: '分类', glyph: '🗂️' },
  { key: '/cart', label: '购物车', glyph: '🛒' },
  { key: '/mine', label: '我的', glyph: '👤' },
];

export default function TabBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const cartCount = useAuth((s) => s.cartCount);
  const refreshCart = useAuth((s) => s.refreshCart);
  const lastPath = useRef(location.pathname);

  useEffect(() => {
    if (lastPath.current !== location.pathname) {
      lastPath.current = location.pathname;
      refreshCart();
    }
  }, [location.pathname, refreshCart]);

  return (
    <nav className="tabbar">
      {TABS.map((tab) => {
        const active = tab.key === '/' ? location.pathname === '/' : location.pathname.startsWith(tab.key);
        return (
          <button
            key={tab.key}
            className={active ? 'on' : ''}
            onClick={() => navigate(tab.key)}
            aria-current={active ? 'page' : undefined}
          >
            <span className="glyph">{tab.glyph}</span>
            {tab.label}
            {tab.key === '/cart' && cartCount > 0 && (
              <span className="badge">{cartCount > 99 ? '99+' : cartCount}</span>
            )}
          </button>
        );
      })}
    </nav>
  );
}
