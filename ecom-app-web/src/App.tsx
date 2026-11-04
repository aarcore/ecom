import { useEffect } from 'react';
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { onUnauthorized, tokenStorage } from '@/api/client';
import SiteHeader from '@/components/SiteHeader';
import SiteFooter from '@/components/SiteFooter';
import { Toasts } from '@/components/ui';
import { useAuth } from '@/store/auth';
import { useToast } from '@/store/toast';
import { useT } from '@/store/locale';
import HomePage from '@/pages/HomePage';
import ShopPage from '@/pages/ShopPage';
import ProductPage from '@/pages/ProductPage';
import CartPage from '@/pages/CartPage';
import CheckoutPage from '@/pages/CheckoutPage';
import LoginPage from '@/pages/LoginPage';
import AccountPage from '@/pages/AccountPage';
import OrdersPage from '@/pages/OrdersPage';
import OrderDetailPage from '@/pages/OrderDetailPage';
import AddressesPage from '@/pages/AddressesPage';
import CouponsPage from '@/pages/CouponsPage';
import ProfilePage from '@/pages/ProfilePage';

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const toasts = useToast((s) => s.toasts);
  const show = useToast((s) => s.show);
  const { t } = useT();
  const refresh = useAuth((s) => s.refresh);
  const logout = useAuth((s) => s.logout);

  useEffect(() => {
    if (tokenStorage.get()) refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(
    () =>
      onUnauthorized(() => {
        logout();
        show(t('error.unauthorized'), 'error');
        navigate('/login', { replace: true });
      }),
    [logout, navigate, show, t],
  );

  // desktop web scrolls to the top on navigation, unlike an app with a back stack
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior });
  }, [location.pathname]);

  return (
    <>
      <SiteHeader />
      <main style={{ minHeight: '60vh' }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/shop" element={<ShopPage />} />
          <Route path="/product/:id" element={<ProductPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/account/addresses" element={<AddressesPage />} />
          <Route path="/account/profile" element={<ProfilePage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/orders/:orderNo" element={<OrderDetailPage />} />
          <Route path="/coupons" element={<CouponsPage />} />
          <Route path="*" element={<HomePage />} />
        </Routes>
      </main>
      <SiteFooter />
      <Toasts items={toasts} />
    </>
  );
}
