import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { useAuth } from '@/store/auth';

export default function ProfileSettingsPage() {
  const navigate = useNavigate();
  const { profile, refresh } = useAuth();
  const [nickname, setNickname] = useState(profile?.nickname ?? '');
  const [gender, setGender] = useState(profile?.gender ?? 0);
  const [avatar, setAvatar] = useState(profile?.avatar ?? '');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (profile) {
      setNickname(profile.nickname ?? '');
      setGender(profile.gender ?? 0);
      setAvatar(profile.avatar ?? '');
    }
  }, [profile]);

  const save = async () => {
    setBusy(true);
    const result = await attempt(
      () => api.put('/app/auth/profile', { nickname, gender, avatar }),
      '资料已更新',
    );
    if (result) await refresh();
    setBusy(false);
  };

  return (
    <div style={{ paddingBottom: 100 }}>
      <div className="topbar">
        <button className="icon-btn" onClick={() => navigate('/mine')}>
          ‹
        </button>
        <div className="title">个人资料</div>
      </div>

      <div className="page">
        <div
          style={{
            background: '#fff',
            borderRadius: 16,
            padding: '20px 16px',
            textAlign: 'center',
            marginBottom: 12,
          }}
        >
          <img
            src={avatar}
            alt=""
            style={{
              width: 76,
              height: 76,
              borderRadius: 999,
              objectFit: 'cover',
              margin: '0 auto',
              background: '#f2f4f8',
            }}
          />
          <div className="muted" style={{ marginTop: 8 }}>头像由演示数据提供</div>
        </div>

        <div className="list-card">
          <div className="row">
            <span className="label">昵称</span>
            <input
              value={nickname}
              onChange={(event) => setNickname(event.target.value)}
              style={{
                flex: 1,
                border: 'none',
                outline: 'none',
                textAlign: 'right',
                fontSize: 14,
                background: 'transparent',
              }}
            />
          </div>
          <div className="row">
            <span className="label">性别</span>
            <div style={{ flex: 1, display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              {[
                { value: 0, label: '保密' },
                { value: 1, label: '男' },
                { value: 2, label: '女' },
              ].map((option) => (
                <button
                  key={option.value}
                  className={`chip ${gender === option.value ? 'on' : ''}`}
                  style={{ padding: '4px 12px', fontSize: 12 }}
                  onClick={() => setGender(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>
          <div className="row">
            <span className="label">手机号</span>
            <span className="value muted">{profile?.mobileMasked ?? profile?.mobile}</span>
          </div>
          <div className="row">
            <span className="label">会员等级</span>
            <span className="value">{profile?.levelName}</span>
          </div>
          <div className="row">
            <span className="label">累计消费</span>
            <span className="value">¥{Number(profile?.totalSpent ?? 0).toFixed(2)}</span>
          </div>
        </div>
      </div>

      <div
        className="bottom-bar"
        style={{ position: 'fixed', left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: 480 }}
      >
        <button className="btn btn-primary btn-block" disabled={busy} onClick={save}>
          {busy ? '保存中…' : '保存资料'}
        </button>
      </div>
    </div>
  );
}
