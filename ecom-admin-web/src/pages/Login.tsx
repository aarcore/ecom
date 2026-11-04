import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Form, Input, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/store/auth';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login, loading } = useAuthStore();
  const [error, setError] = useState<string | null>(null);

  const onFinish = async (values: { username: string; password: string }) => {
    setError(null);
    try {
      await login(values.username, values.password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败，请稍后重试');
    }
  };

  return (
    <div className="login-shell">
      <div className="login-art">
        <div className="brand">
          <span
            style={{
              width: 34,
              height: 34,
              borderRadius: 10,
              display: 'grid',
              placeItems: 'center',
              background: 'linear-gradient(135deg, #4f7cff, #8a5cff)',
              fontWeight: 700,
            }}
          >
            E
          </span>
          ECOM PLATFORM
        </div>

        <div className="headline">
          <h2>
            一套后端，
            <br />
            同时服务小程序与 Web 商城
          </h2>
          <p>
            商品、库存、订单、会员、营销全部收敛到同一个单体服务，运营在这里完成日常决策，
            顾客在 H5 与微信小程序之间无缝切换。
          </p>
        </div>

        <div className="points">
          <div className="point">
            <div className="k">2 端</div>
            <div className="v">Web H5 + 微信小程序</div>
          </div>
          <div className="point">
            <div className="k">1 套</div>
            <div className="v">统一的 API 与鉴权体系</div>
          </div>
          <div className="point">
            <div className="k">PostgreSQL</div>
            <div className="v">迁移脚本托管全部表结构</div>
          </div>
          <div className="point">
            <div className="k">Docker</div>
            <div className="v">一条命令拉起全栈环境</div>
          </div>
        </div>
      </div>

      <div className="login-panel">
        <div className="login-form">
          <h1>登录运营后台</h1>
          <div className="hint">使用管理员账号进入控制台</div>

          {error && (
            <Alert
              type="error"
              showIcon
              message={error}
              style={{ marginBottom: 16, borderRadius: 10 }}
            />
          )}

          <Form layout="vertical" onFinish={onFinish} requiredMark={false} size="large">
            <Form.Item
              name="username"
              label="账号"
              rules={[{ required: true, message: '请输入账号' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="admin" autoComplete="username" />
            </Form.Item>

            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="••••••••"
                autoComplete="current-password"
              />
            </Form.Item>

            <Form.Item style={{ marginTop: 24, marginBottom: 0 }}>
              <Button type="primary" htmlType="submit" loading={loading} block size="large">
                进入后台
              </Button>
            </Form.Item>
          </Form>

          <div className="login-hint">
            <Typography.Text strong>演示账号</Typography.Text>
            <br />
            admin / admin123
            <br />
            <span style={{ color: '#7a8499' }}>
              数据由 Flyway 迁移脚本初始化，首次启动即带有商品、订单与会员样例。
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
