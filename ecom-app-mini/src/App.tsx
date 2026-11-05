import { useEffect } from 'react';
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { absorbTokenFromQuery, onUnauthorized, tokenStorage } from '@/api/client';
import { Toasts } from '@/components/ui';
import TabBar from '@/components/TabBar';
import { useAuth } from '@/store/auth';
import { useToast } from '@/store/toast';
import HomePage from '@/pages/HomePage';
import CategoryPage from '@/pages/CategoryPage';
import SearchPage from '@/pages/SearchPage';
import ProductDetailPage from '@/pages/ProductDetailPage';
import CartPage from '@/pages/CartPage';
import CheckoutPage from '@/pages/CheckoutPage';
import OrderListPage from '@/pages/OrderListPage';
import OrderDetailPage from '@/pages/OrderDetailPage';
import MinePage from '@/pages/MinePage';
import LoginPage from '@/pages/LoginPage';
import AddressPage from '@/pages/AddressPage';
import CouponsPage from '@/pages/CouponsPage';
import ProfileSettingsPage from '@/pages/ProfileSettingsPage';

/** Routes that render the bottom tab bar. */
const TAB_ROUTES = ['/', '/category', '/cart', '/mine'];

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const toasts = useToast((s) => s.toasts);
  const show = useToast((s) => s.show);
  const refresh = useAuth((s) => s.refresh);
  const logout = useAuth((s) => s.logout);

  useEffect(() => {
    absorbTokenFromQuery();
    if (tokenStorage.get()) refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(
    () =>
      onUnauthorized(() => {
        logout();
        show('登录状态已失效', 'error');
        navigate('/login', { replace: true });
      }),
    [logout, navigate, show],
  );

  const showTabs = TAB_ROUTES.includes(location.pathname);

  return (
    <div className="app-frame" style={{ paddingBottom: showTabs ? undefined : 0 }}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/category" element={<CategoryPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/product/:id" element={<ProductDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/orders" element={<OrderListPage />} />
        <Route path="/order/:orderNo" element={<OrderDetailPage />} />
        <Route path="/mine" element={<MinePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/address" element={<AddressPage />} />
        <Route path="/coupons" element={<CouponsPage />} />
        <Route path="/settings/profile" element={<ProfileSettingsPage />} />
        <Route path="*" element={<HomePage />} />
      </Routes>

      {showTabs && <TabBar />}
      <Toasts items={toasts} />
    </div>
  );
}
