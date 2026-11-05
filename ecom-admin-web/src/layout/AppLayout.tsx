import { useEffect, useMemo, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Avatar,
  Breadcrumb,
  Button,
  Dropdown,
  Layout,
  Menu,
  Space,
  Tooltip,
  Typography,
  type MenuProps,
} from 'antd';
import {
  AppstoreOutlined,
  CreditCardOutlined,
  DashboardOutlined,
  FileTextOutlined,
  GiftOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/store/auth';

const { Header, Sider, Content } = Layout;

/**
 * Navigation model.
 *
 * `section` groups menu entries for the breadcrumb without repeating the
 * tree: adding a page means one entry here plus one route, not two edits in
 * two shapes.
 */
interface NavItem {
  key: string;
  label: string;
  icon?: React.ReactNode;
  section?: string;
  children?: NavItem[];
}

const NAV: NavItem[] = [
  { key: '/', label: '数据看板', icon: <DashboardOutlined /> },
  {
    key: 'goods',
    label: '商品',
    icon: <ShoppingOutlined />,
    children: [
      { key: '/products', label: '商品列表' },
      { key: '/categories', label: '分类' },
      { key: '/brands', label: '品牌' },
    ],
  },
  { key: '/orders', label: '订单', icon: <ShoppingCartOutlined /> },
  { key: '/members', label: '会员', icon: <TeamOutlined /> },
  { key: '/pay-channels', label: '支付渠道', icon: <CreditCardOutlined /> },
  {
    key: 'marketing',
    label: '营销',
    icon: <GiftOutlined />,
    children: [
      { key: '/coupons', label: '优惠券' },
      { key: '/banners', label: '轮播图' },
      { key: '/notices', label: '公告' },
    ],
  },
  {
    key: 'system',
    label: '系统',
    icon: <SettingOutlined />,
    children: [
      { key: '/logs', label: '操作日志' },
      { key: '/settings', label: '存储与配置' },
    ],
  },
];

/** Flat route → label + section, derived from NAV so the two cannot drift. */
const ROUTE_META: Record<string, { label: string; section?: string }> = (() => {
  const meta: Record<string, { label: string; section?: string }> = {};
  for (const item of NAV) {
    if (item.children?.length) {
      for (const child of item.children) {
        meta[child.key] = { label: child.label, section: item.label };
      }
    } else {
      meta[item.key] = { label: item.label };
    }
  }
  // routes that exist but are not first-class menu entries
  meta['/products/new'] = { label: '新增商品', section: '商品' };
  return meta;
})();

const SIDER_WIDTH = 208;
const SIDER_COLLAPSED = 60;

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { profile, logout, loadProfile } = useAuthStore();
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (!profile) loadProfile();
  }, [profile, loadProfile]);

  const path = location.pathname;

  /** Highlight the parent entry for nested routes like /products/12/edit. */
  const selectedKeys = useMemo(() => {
    if (/^\/products\/(new|\d+\/edit)$/.test(path)) return ['/products'];
    if (ROUTE_META[path]) return [path];
    return [path];
  }, [path]);

  /** Only the owning group is expanded; keeps the rail short. */
  const [openKeys, setOpenKeys] = useState<string[]>([]);
  useEffect(() => {
    const section = ROUTE_META[path]?.section;
    if (!section) return;
    const parent = NAV.find((item) => item.section === section)
      ?? NAV.find((item) => item.children?.some((c) => c.key === path));
    if (parent) setOpenKeys([parent.key]);
  }, [path]);

  const pageTitle = useMemo(() => {
    if (/^\/products\/\d+\/edit$/.test(path)) return '编辑商品';
    return ROUTE_META[path]?.label ?? '管理后台';
  }, [path]);

  const userMenu: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <AppstoreOutlined />,
      label: (
        <div style={{ lineHeight: 1.4 }}>
          <div style={{ fontWeight: 500 }}>{profile?.nickname || profile?.username || '管理员'}</div>
          <div style={{ fontSize: 11.5, opacity: 0.6 }}>{profile?.role ?? ''}</div>
        </div>
      ),
      disabled: true,
    },
    { type: 'divider' },
    { key: 'settings', icon: <SettingOutlined />, label: '存储与配置' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
  ];

  const breadcrumbItems = useMemo(() => {
    const meta = ROUTE_META[path];
    const items: { title: React.ReactNode }[] = [
      { title: <Link to="/">首页</Link> },
    ];
    if (meta?.section) items.push({ title: meta.section });
    items.push({ title: <span>{pageTitle}</span> });
    return items;
  }, [path, pageTitle]);

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        trigger={null}
        width={SIDER_WIDTH}
        collapsedWidth={SIDER_COLLAPSED}
        // Light rail matching the content canvas: the console is used all day,
        // and a dark slab on the left made the whole screen feel heavier than
        // the data it was showing.
        theme="light"
        style={{
          position: 'sticky',
          top: 0,
          height: '100vh',
          overflow: 'auto',
          borderRight: '1px solid var(--line)',
          background: '#fff',
        }}
      >
        <div className={`sider-brand ${collapsed ? 'is-collapsed' : ''}`}>
          <span className="mark">E</span>
          {!collapsed && <span className="name">Ecom</span>}
        </div>

        <Menu
          mode="inline"
          items={NAV.map(function toMenuItem(item): NonNullable<MenuProps['items']>[number] {
            return item.children?.length
              ? {
                  key: item.key,
                  icon: item.icon,
                  label: item.label,
                  children: item.children.map((child) => ({ key: child.key, label: child.label })),
                }
              : { key: item.key, icon: item.icon, label: item.label };
          })}
          selectedKeys={selectedKeys}
          openKeys={collapsed ? undefined : openKeys}
          onOpenChange={(keys) => setOpenKeys(keys as string[])}
          onClick={({ key }) => {
            if (key.startsWith('/')) navigate(key);
          }}
          style={{ borderInlineEnd: 'none', padding: '4px 8px 16px' }}
        />
      </Sider>

      <Layout>
        <Header className="app-header">
          <Space size={10} align="center">
            <Button
              type="text"
              size="small"
              aria-label={collapsed ? '展开菜单' : '收起菜单'}
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed((v) => !v)}
              style={{ color: 'var(--ink-soft)' }}
            />
            <Breadcrumb items={breadcrumbItems} />
          </Space>

          <Space size={6} align="center">
            <Tooltip title="服务运行正常">
              <span className="status-dot" />
            </Tooltip>
            <Dropdown
              menu={{
                items: userMenu,
                onClick: ({ key }) => {
                  if (key === 'logout') {
                    logout();
                    navigate('/login', { replace: true });
                  }
                  if (key === 'settings') navigate('/settings');
                },
              }}
              trigger={['click']}
              placement="bottomRight"
            >
              <Button type="text" className="header-user">
                <Avatar src={profile?.avatar} size={26} style={{ background: '#2f54eb', fontSize: 12 }}>
                  {(profile?.nickname || profile?.username || 'A').slice(0, 1)}
                </Avatar>
                <Typography.Text style={{ fontSize: 13 }}>
                  {profile?.nickname || profile?.username || '管理员'}
                </Typography.Text>
              </Button>
            </Dropdown>
          </Space>
        </Header>

        <Content style={{ padding: '18px 22px 28px' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
