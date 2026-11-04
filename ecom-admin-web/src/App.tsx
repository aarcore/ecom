import { useEffect } from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { Spin } from 'antd';
import AppLayout from '@/layout/AppLayout';
import LoginPage from '@/pages/Login';
import DashboardPage from '@/pages/Dashboard';
import ProductListPage from '@/pages/products/ProductList';
import ProductEditPage from '@/pages/products/ProductEdit';
import CategoryPage from '@/pages/catalog/CategoryPage';
import BrandPage from '@/pages/catalog/BrandPage';
import BannerPage from '@/pages/catalog/BannerPage';
import NoticePage from '@/pages/catalog/NoticePage';
import OrderListPage from '@/pages/orders/OrderList';
import MemberListPage from '@/pages/members/MemberList';
import CouponPage from '@/pages/marketing/CouponPage';
import PayChannelPage from '@/pages/pay/PayChannelPage';
import LogPage from '@/pages/system/LogPage';
import SettingsPage from '@/pages/system/SettingsPage';
import { onUnauthorized } from '@/api/client';
import { useAuthStore } from '@/store/auth';

export default function App() {
  const { token, ready, loadProfile, logout } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (token && !ready) {
      loadProfile();
    }
  }, [token, ready, loadProfile]);

  useEffect(
    () =>
      onUnauthorized(() => {
        logout();
        navigate('/login', { replace: true });
      }),
    [logout, navigate],
  );

  if (token && !ready) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'grid',
          placeItems: 'center',
          gap: 12,
          color: '#8b93a7',
          fontSize: 13,
        }}
      >
        <Spin size="large" />
        <span>正在加载工作台…</span>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={token ? <AppLayout /> : <Navigate to="/login" replace />}
      >
        <Route index element={<DashboardPage />} />
        <Route path="products" element={<ProductListPage />} />
        <Route path="products/new" element={<ProductEditPage />} />
        <Route path="products/:id/edit" element={<ProductEditPage />} />
        <Route path="categories" element={<CategoryPage />} />
        <Route path="brands" element={<BrandPage />} />
        <Route path="banners" element={<BannerPage />} />
        <Route path="notices" element={<NoticePage />} />
        <Route path="orders" element={<OrderListPage />} />
        <Route path="members" element={<MemberListPage />} />
        <Route path="coupons" element={<CouponPage />} />
        <Route path="pay-channels" element={<PayChannelPage />} />
        <Route path="logs" element={<LogPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
