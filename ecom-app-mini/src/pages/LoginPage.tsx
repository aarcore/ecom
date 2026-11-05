import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api, attempt, platform } from '@/api/client';
import { useAuth } from '@/store/auth';
import { useToast } from '@/store/toast';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, loginWithCode, loginWithWechat } = useAuth();
  const show = useToast((s) => s.show);

  const from = (location.state as { from?: string } | null)?.from ?? '/';
  const [mode, setMode] = useState<'password' | 'code'>('password');
  const [mobile, setMobile] = useState('13800138000');
  const [password, setPassword] = useState('123456');
  const [code, setCode] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [busy, setBusy] = useState(false);
  const timer = useRef<number | null>(null);

  useEffect(() => () => {
    if (timer.current) window.clearInterval(timer.current);
  }, []);

  const sendCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      show('请输入正确的手机号', 'error');
      return;
    }
    const result = await attempt(() => api.post<{ demoCode: string }>('/app/auth/sms', null, { mobile }));
    if (result) {
      // demo build echoes the code back so the flow is testable without SMS
      setCode(result.demoCode);
      show(`验证码已发送：${result.demoCode}`, 'success');
      setCountdown(60);
      timer.current = window.setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1 && timer.current) {
            window.clearInterval(timer.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
  };

  const submit = async () => {
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      show('请输入正确的手机号', 'error');
      return;
    }
    setBusy(true);
    try {
      if (mode === 'password') {
        await login(mobile, password);
      } else {
        if (!code) {
          show('请输入验证码', 'error');
          setBusy(false);
          return;
        }
        await loginWithCode(mobile, code);
      }
      show('登录成功', 'success');
      navigate(from, { replace: true });
    } catch (error) {
      show(error instanceof Error ? error.message : '登录失败', 'error');
    } finally {
      setBusy(false);
    }
  };

  /** Simulated wx.login() so the mini-program path is demonstrable in H5. */
  const wechatLogin = async () => {
    setBusy(true);
    try {
      const code = `${platform}-${Date.now()}`;
      await loginWithWechat(code, '微信用户');
      show('微信登录成功', 'success');
      navigate(from, { replace: true });
    } catch (error) {
      show(error instanceof Error ? error.message : '登录失败', 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'linear-gradient(170deg, #fff5f2 0%, #f6f7f9 42%)' }}>
      <div className="topbar" style={{ background: 'transparent', borderBottom: 'none' }}>
        <button className="icon-btn" onClick={() => navigate('/')}>
          ✕
        </button>
      </div>

      <div style={{ padding: '8px 26px 0' }}>
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: 18,
            display: 'grid',
            placeItems: 'center',
            background: 'linear-gradient(135deg, #ff6b4a, #ff2f4f)',
            fontSize: 26,
            boxShadow: '0 10px 24px rgba(255,70,80,0.26)',
          }}
        >
          🛍️
        </div>
        <h1 style={{ fontSize: 26, fontWeight: 680, letterSpacing: '-0.02em', margin: '18px 0 6px' }}>
          欢迎回到 山脊单车
        </h1>
        <p style={{ color: 'var(--ink-2)', fontSize: 13.5, margin: 0 }}>
          同一账号，H5 与微信小程序数据互通
        </p>

        <div style={{ display: 'flex', gap: 20, margin: '26px 0 18px' }}>
          {[
            { key: 'password' as const, label: '密码登录' },
            { key: 'code' as const, label: '验证码登录' },
          ].map((item) => (
            <button
              key={item.key}
              onClick={() => setMode(item.key)}
              style={{
                border: 'none',
                background: 'transparent',
                padding: '4px 0',
                fontSize: 14.5,
                fontWeight: mode === item.key ? 650 : 400,
                color: mode === item.key ? 'var(--ink)' : 'var(--ink-3)',
                cursor: 'pointer',
                position: 'relative',
              }}
            >
              {item.label}
              {mode === item.key && (
                <span
                  style={{
                    position: 'absolute',
                    bottom: -2,
                    left: 0,
                    width: 18,
                    height: 3,
                    borderRadius: 3,
                    background: 'var(--brand)',
                  }}
                />
              )}
            </button>
          ))}
        </div>

        <div className="stack">
          <input
            value={mobile}
            onChange={(event) => setMobile(event.target.value.replace(/\D/g, '').slice(0, 11))}
            placeholder="手机号"
            inputMode="numeric"
            style={inputStyle}
          />

          {mode === 'password' ? (
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="密码"
              type="password"
              style={inputStyle}
            />
          ) : (
            <div style={{ display: 'flex', gap: 10 }}>
              <input
                value={code}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="验证码"
                inputMode="numeric"
                style={{ ...inputStyle, flex: 1 }}
              />
              <button
                className="btn btn-outline"
                style={{ height: 52, minWidth: 108, fontSize: 13 }}
                disabled={countdown > 0}
                onClick={sendCode}
              >
                {countdown > 0 ? `${countdown}s` : '获取验证码'}
              </button>
            </div>
          )}
        </div>

        <button
          className="btn btn-primary btn-block"
          style={{ marginTop: 22, height: 50 }}
          disabled={busy}
          onClick={submit}
        >
          {busy ? '登录中…' : '登录 / 注册'}
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '26px 0' }}>
          <div style={{ flex: 1, height: 1, background: 'var(--line)' }} />
          <span className="muted">其他方式</span>
          <div style={{ flex: 1, height: 1, background: 'var(--line)' }} />
        </div>

        <button
          className="btn btn-outline btn-block"
          style={{ height: 48 }}
          disabled={busy}
          onClick={wechatLogin}
        >
          <span style={{ color: '#0fbb56', fontSize: 16 }}>💬</span> 微信一键登录
        </button>

        <div
          style={{
            marginTop: 24,
            padding: '12px 14px',
            borderRadius: 12,
            background: '#fff',
            border: '1px dashed #ffd6cf',
            fontSize: 12,
            lineHeight: 1.8,
            color: '#8a5a2b',
          }}
        >
          <strong>演示账号</strong>
          <br />
          手机号 13800138000<br />
          密码 123456
          <br />
          <span style={{ color: 'var(--ink-3)' }}>
            验证码登录会自动创建账号；微信登录在演示环境用本地模拟 code 换取身份。
          </span>
        </div>
      </div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  height: 52,
  borderRadius: 14,
  border: '1px solid var(--line)',
  background: '#fff',
  padding: '0 16px',
  fontSize: 15,
  outline: 'none',
  width: '100%',
};
