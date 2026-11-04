import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { Empty } from '@/components/ui';
import { useAuth } from '@/store/auth';
import type { Address } from '@/types';

const EMPTY_FORM = {
  receiver: '',
  mobile: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0,
};

export default function AddressPage() {
  const navigate = useNavigate();
  const token = useAuth((s) => s.token);
  const [rows, setRows] = useState<Address[]>([]);
  const [editing, setEditing] = useState<Partial<Address> | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [busy, setBusy] = useState(false);

  const load = () => {
    if (!token) return;
    api.get<Address[]>('/app/addresses').then(setRows).catch(() => undefined);
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const openEditor = (address?: Address) => {
    setEditing(address ?? {});
    setForm(
      address
        ? {
            receiver: address.receiver,
            mobile: address.mobile,
            province: address.province ?? '',
            city: address.city ?? '',
            district: address.district ?? '',
            detail: address.detail,
            isDefault: address.isDefault ?? 0,
          }
        : EMPTY_FORM,
    );
  };

  const save = async () => {
    if (!form.receiver || !form.mobile || !form.detail) {
      await attempt(() => Promise.reject(new Error('请填写收货人、手机号与详细地址')));
      return;
    }
    setBusy(true);
    const payload = { ...form, id: (editing as Address)?.id };
    const result = await attempt(() =>
      payload.id
        ? api.put(`/app/addresses/${payload.id}`, payload)
        : api.post('/app/addresses', payload),
      '地址已保存',
    );
    setBusy(false);
    if (result) {
      setEditing(null);
      load();
    }
  };

  const remove = async (address: Address) => {
    const result = await attempt(() => api.delete(`/app/addresses/${address.id}`), '已删除');
    if (result !== null) load();
  };

  const setDefault = async (address: Address) => {
    const result = await attempt(() => api.put(`/app/addresses/${address.id}/default`), '已设为默认');
    if (result !== null) load();
  };

  if (!token) {
    return (
      <div className="page">
        <Empty
          glyph="🔐"
          text="登录后管理收货地址"
          action={
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/login')}>
              去登录
            </button>
          }
        />
      </div>
    );
  }

  return (
    <div style={{ paddingBottom: editing ? 0 : 90 }}>
      <div className="topbar">
        <button className="icon-btn" onClick={() => navigate('/mine')}>
          ‹
        </button>
        <div className="title">收货地址</div>
      </div>

      <div className="page">
        {rows.length === 0 ? (
          <Empty glyph="📍" text="还没有收货地址" />
        ) : (
          <div className="stack">
            {rows.map((address) => (
              <div key={address.id} className="list-card" style={{ padding: 14 }}>
                <div className="row-between" style={{ marginBottom: 4 }}>
                  <div style={{ fontSize: 14.5, fontWeight: 600 }}>
                    {address.receiver}
                    <span style={{ marginLeft: 10, fontWeight: 400, fontSize: 13, color: 'var(--ink-2)' }}>
                      {address.mobile}
                    </span>
                  </div>
                  {address.isDefault === 1 && (
                    <span
                      style={{
                        fontSize: 10,
                        padding: '2px 7px',
                        borderRadius: 6,
                        background: 'var(--brand-soft)',
                        color: 'var(--brand)',
                        fontWeight: 600,
                      }}
                    >
                      默认
                    </span>
                  )}
                </div>
                <div className="muted" style={{ marginBottom: 12 }}>{address.fullAddress}</div>
                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  {address.isDefault !== 1 && (
                    <button className="btn btn-ghost btn-sm" onClick={() => setDefault(address)}>
                      设为默认
                    </button>
                  )}
                  <button className="btn btn-outline btn-sm" onClick={() => openEditor(address)}>
                    编辑
                  </button>
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ color: '#d94848' }}
                    onClick={() => remove(address)}
                  >
                    删除
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {!editing && (
        <div
          className="bottom-bar"
          style={{ position: 'fixed', left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: 480 }}
        >
          <button className="btn btn-primary btn-block" onClick={() => openEditor()}>
            新增收货地址
          </button>
        </div>
      )}

      {editing && (
        <div className="sheet-mask" onClick={() => setEditing(null)}>
          <div className="sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-title">{(editing as Address).id ? '编辑地址' : '新增地址'}</div>
            <div className="stack">
              <input
                value={form.receiver}
                onChange={(event) => setForm({ ...form, receiver: event.target.value })}
                placeholder="收货人姓名"
                style={inputStyle}
              />
              <input
                value={form.mobile}
                onChange={(event) =>
                  setForm({ ...form, mobile: event.target.value.replace(/\D/g, '').slice(0, 11) })
                }
                placeholder="手机号"
                inputMode="numeric"
                style={inputStyle}
              />
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  value={form.province}
                  onChange={(event) => setForm({ ...form, province: event.target.value })}
                  placeholder="省份"
                  style={inputStyle}
                />
                <input
                  value={form.city}
                  onChange={(event) => setForm({ ...form, city: event.target.value })}
                  placeholder="城市"
                  style={inputStyle}
                />
                <input
                  value={form.district}
                  onChange={(event) => setForm({ ...form, district: event.target.value })}
                  placeholder="区县"
                  style={inputStyle}
                />
              </div>
              <textarea
                value={form.detail}
                onChange={(event) => setForm({ ...form, detail: event.target.value })}
                placeholder="详细地址（街道、门牌号）"
                rows={3}
                style={{ ...inputStyle, height: 'auto', padding: '14px 16px', resize: 'none' }}
              />
              <div
                className="row-between"
                style={{ padding: '4px 2px', fontSize: 13, color: 'var(--ink-2)' }}
              >
                <span>设为默认地址</span>
                <div
                  className={`checkbox ${form.isDefault === 1 ? 'on' : ''}`}
                  onClick={() => setForm({ ...form, isDefault: form.isDefault === 1 ? 0 : 1 })}
                >
                  {form.isDefault === 1 ? '✓' : ''}
                </div>
              </div>
            </div>
            <button
              className="btn btn-primary btn-block"
              style={{ marginTop: 18 }}
              disabled={busy}
              onClick={save}
            >
              {busy ? '保存中…' : '保存地址'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  height: 48,
  borderRadius: 12,
  border: '1px solid var(--line)',
  background: '#fff',
  padding: '0 14px',
  fontSize: 14,
  outline: 'none',
  width: '100%',
  minWidth: 0,
};
