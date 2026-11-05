import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';
import { useToast } from '@/store/toast';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, loginWithCode } = useAuth();
  const show = useToast((s) => s.show);
  const { t } = useT();

  const from = (location.state as { from?: string } | null)?.from ?? '/';
  const [mode, setMode] = useState<'password' | 'code'>('password');
  const [mobile, setMobile] = useState('13800138000');
  const [password, setPassword] = useState('123456');
  const [code, setCode] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [busy, setBusy] = useState(false);
  const timer = useRef<number | null>(null);

  useEffect(
    () => () => {
      if (timer.current) window.clearInterval(timer.current);
    },
    [],
  );

  const sendCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      show(t('login.invalidMobile'), 'error');
      return;
    }
    const result = await attempt(() =>
      api.post<{ demoCode: string }>('/app/auth/sms', null, { mobile }),
    );
    if (result) {
      // demo build echoes the code so the flow is testable without an SMS gateway
      setCode(result.demoCode);
      show(t('login.demoCode', { code: result.demoCode }), 'success');
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

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      show(t('login.invalidMobile'), 'error');
      return;
    }
    setBusy(true);
    try {
      if (mode === 'password') {
        await login(mobile, password);
      } else {
        if (!code) throw new Error(t('login.enterCode'));
        await loginWithCode(mobile, code);
      }
      show(t('login.success'), 'success');
      navigate(from, { replace: true });
    } catch (error) {
      show(error instanceof Error ? error.message : t('login.failed'), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="shell">
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: 'clamp(32px, 6vw, 96px)',
          alignItems: 'center',
          minHeight: 'calc(100vh - var(--header-h) - 220px)',
          padding: 'clamp(32px, 6vw, 80px) 0',
        }}
      >
        {/* -------------------------------------------------------- editorial */}
        <div className="hide-mobile">
          <div className="eyebrow" style={{ marginBottom: 18 }}>{t('login.heroEyebrow')}</div>
          <h1
            style={{
              fontSize: 'clamp(32px, 4.2vw, 54px)',
              lineHeight: 1.06,
              letterSpacing: '-0.03em',
              marginBottom: 22,
            }}
          >
            {t('login.heroTitle')
              .split('\n')
              .map((line, i, all) => (
                <span key={line}>
                  {line}
                  {i < all.length - 1 && <br />}
                </span>
              ))}
          </h1>
          <p style={{ color: 'var(--text-2)', lineHeight: 1.8, maxWidth: '40ch', margin: 0 }}>
            {t('login.heroBody')}
          </p>
        </div>

        {/* ------------------------------------------------------------ form */}
        <div style={{ maxWidth: 400, width: '100%' }}>
          <h1 style={{ fontSize: 26, marginBottom: 6 }}>
            {mode === 'password' ? t('login.title') : t('login.titleCode')}
          </h1>
          <p className="muted" style={{ marginBottom: 26 }}>
            {mode === 'password' ? t('login.subtitle') : t('login.subtitleCode')}
          </p>

          <div className="pills" style={{ marginBottom: 24 }}>
            <button
              className={`pill ${mode === 'password' ? 'on' : ''}`}
              onClick={() => setMode('password')}
            >
              {t('login.passwordMode')}
            </button>
            <button className={`pill ${mode === 'code' ? 'on' : ''}`} onClick={() => setMode('code')}>
              {t('login.codeMode')}
            </button>
          </div>

          <form onSubmit={submit} className="stack">
            <label className="field">
              <span className="field-label">{t('login.mobile')}</span>
              <input
                className="input"
                value={mobile}
                onChange={(e) => setMobile(e.target.value.replace(/\D/g, '').slice(0, 11))}
                inputMode="numeric"
                autoComplete="tel"
                placeholder={t('login.mobilePlaceholder')}
              />
            </label>

            {mode === 'password' ? (
              <label className="field">
                <span className="field-label">{t('login.password')}</span>
                <input
                  className="input"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  placeholder={t('login.passwordPlaceholder')}
                />
              </label>
            ) : (
              <label className="field">
                <span className="field-label">{t('login.code')}</span>
                <div style={{ display: 'flex', gap: 8 }}>
                  <input
                    className="input"
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    inputMode="numeric"
                    placeholder={t('login.codePlaceholder')}
                    style={{ flex: 1 }}
                  />
                  <button
                    type="button"
                    className="btn btn-outline"
                    disabled={countdown > 0}
                    onClick={sendCode}
                    style={{ minWidth: 108 }}
                  >
                    {countdown > 0 ? `${countdown}s` : t('login.getCode')}
                  </button>
                </div>
              </label>
            )}

            <button className="btn btn-primary btn-block" type="submit" disabled={busy} style={{ marginTop: 8 }}>
              {busy ? t('login.submitting') : t('login.submit')}
            </button>
          </form>

          <div
            style={{
              marginTop: 26,
              padding: '14px 16px',
              border: '1px dashed var(--rule-strong)',
              fontSize: 12,
              lineHeight: 1.9,
              color: 'var(--text-3)',
            }}
          >
            <strong style={{ color: 'var(--text-2)' }}>{t('login.demoTitle')}</strong>
            <br />
            {t('login.demoAccount')}
            <br />
            {t('login.demoNote')}
          </div>
        </div>
      </div>
    </div>
  );
}
