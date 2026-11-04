import { useEffect, useState } from 'react';
import { api, attempt } from '@/api/client';
import AccountLayout from '@/components/AccountLayout';
import { useAuth } from '@/store/auth';
import { useT } from '@/store/locale';

export default function ProfilePage() {
  const { profile, refresh } = useAuth();
  const { t } = useT();
  const [nickname, setNickname] = useState('');
  const [gender, setGender] = useState(0);
  const [avatar, setAvatar] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (profile) {
      setNickname(profile.nickname ?? '');
      setGender(profile.gender ?? 0);
      setAvatar(profile.avatar ?? '');
    }
  }, [profile]);

  const save = async () => {
    setBusy(true);
    const ok = await attempt(
      () => api.put('/app/auth/profile', { nickname, gender, avatar }),
      t('profile.saved'),
    );
    if (ok) await refresh();
    setBusy(false);
  };

  return (
    <AccountLayout title={t('account.profile')} activePath="/account/profile">
      <div style={{ maxWidth: 560 }}>
        <div className="stack">
          <div className="row" style={{ gap: 16, marginBottom: 8 }}>
            <img
              src={avatar}
              alt=""
              style={{
                width: 72,
                height: 72,
                borderRadius: 999,
                objectFit: 'cover',
                background: 'var(--plate-2)',
              }}
            />
            <div>
              <div style={{ fontSize: 14.5, fontWeight: 600 }}>{profile?.nickname}</div>
              <div className="muted">
                {profile?.mobileMasked ?? profile?.mobile} · {profile?.levelName}
              </div>
            </div>
          </div>

          <label className="field">
            <span className="field-label">{t('profile.nickname')}</span>
            <input
              className="input"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder={t('profile.nicknamePlaceholder')}
            />
          </label>

          <div className="field">
            <span className="field-label">{t('profile.gender')}</span>
            <div className="chips">
              {[
                { value: 0, label: t('profile.genderPrivate') },
                { value: 1, label: t('profile.genderMale') },
                { value: 2, label: t('profile.genderFemale') },
              ].map((option) => (
                <button
                  key={option.value}
                  className={`chip ${gender === option.value ? 'on' : ''}`}
                  onClick={() => setGender(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <label className="field">
            <span className="field-label">{t('profile.avatar')}</span>
            <input
              className="input"
              value={avatar}
              onChange={(e) => setAvatar(e.target.value)}
              placeholder={t('profile.avatarPlaceholder')}
            />
          </label>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
              gap: 12,
              padding: '18px 0',
              borderTop: '1px solid var(--rule)',
              marginTop: 8,
            }}
          >
            {[
              { label: t('profile.totalSpent'), value: `¥${Number(profile?.totalSpent ?? 0).toFixed(2)}` },
              { label: t('profile.orderCount'), value: profile?.orderCount ?? 0 },
              { label: t('profile.points'), value: profile?.points ?? 0 },
              { label: t('profile.balance'), value: `¥${Number(profile?.balance ?? 0).toFixed(2)}` },
            ].map((item) => (
              <div key={item.label}>
                <div className="muted" style={{ fontSize: 11.5 }}>{item.label}</div>
                <div style={{ fontSize: 15, fontWeight: 600, marginTop: 2 }}>{item.value}</div>
              </div>
            ))}
          </div>

          <button className="btn btn-primary" disabled={busy} onClick={save} style={{ marginTop: 8 }}>
            {busy ? t('common.saving') : t('profile.save')}
          </button>
        </div>
      </div>
    </AccountLayout>
  );
}
