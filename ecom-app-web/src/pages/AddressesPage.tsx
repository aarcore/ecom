import { useEffect, useState } from 'react';
import { api, attempt } from '@/api/client';
import AccountLayout from '@/components/AccountLayout';
import AddressModal from '@/components/AddressModal';
import { Empty } from '@/components/ui';
import { IconTrash } from '@/components/icons';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { Address } from '@/types';

export default function AddressesPage() {
  const { t } = useT();
  const locale = useLocale((s) => s.locale);
  const [rows, setRows] = useState<Address[]>([]);
  /** `null` = closed, `{}` = create, a row = edit. */
  const [editing, setEditing] = useState<Partial<Address> | null>(null);

  const load = () => {
    api.get<Address[]>('/app/addresses').then(setRows).catch(() => undefined);
  };

  useEffect(load, [locale]);

  const remove = async (address: Address) => {
    const ok = await attempt(() => api.delete(`/app/addresses/${address.id}`), t('address.deleted'));
    if (ok !== null) load();
  };

  const setDefault = async (address: Address) => {
    const ok = await attempt(
      () => api.put(`/app/addresses/${address.id}/default`),
      t('address.setDefaultDone'),
    );
    if (ok !== null) load();
  };

  return (
    <AccountLayout
      title={t('account.addresses')}
      subtitle={t('address.subtitle')}
      activePath="/account/addresses"
      action={
        <button className="btn btn-primary btn-sm" onClick={() => setEditing({})}>
          {t('checkout.addAddress')}
        </button>
      }
    >
      {rows.length === 0 ? (
        <Empty
          glyph="◎"
          title={t('checkout.noAddress')}
          description={t('address.emptyHint')}
          action={
            <button className="btn btn-primary" onClick={() => setEditing({})}>
              {t('checkout.addAddress')}
            </button>
          }
        />
      ) : (
        <div style={{ display: 'grid', gap: 12 }}>
          {rows.map((address) => (
            <div
              key={address.id}
              style={{
                border: '1px solid var(--rule)',
                padding: '18px 20px',
                display: 'flex',
                gap: 18,
                alignItems: 'flex-start',
                flexWrap: 'wrap',
              }}
            >
              <div style={{ flex: 1, minWidth: 220 }}>
                <div style={{ fontSize: 14.5, fontWeight: 600 }}>
                  {address.receiver}
                  <span style={{ marginLeft: 10, fontWeight: 400, fontSize: 12.5, color: 'var(--text-3)' }}>
                    {address.mobile}
                  </span>
                  {address.isDefault === 1 && (
                    <span className="tag" style={{ marginLeft: 10 }}>{t('checkout.default')}</span>
                  )}
                </div>
                <div className="muted" style={{ marginTop: 4, lineHeight: 1.7 }}>
                  {address.fullAddress}
                </div>
              </div>
              <div className="row" style={{ gap: 14 }}>
                {address.isDefault !== 1 && (
                  <button className="btn-link" onClick={() => setDefault(address)}>
                    {t('address.setDefaultShort')}
                  </button>
                )}
                <button className="btn-link" onClick={() => setEditing(address)}>
                  {t('common.edit')}
                </button>
                <button
                  className="btn-link row"
                  style={{ gap: 5, color: 'var(--flare)', borderColor: 'transparent' }}
                  onClick={() => remove(address)}
                >
                  <IconTrash size={13} /> {t('common.delete')}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {editing && (
        <AddressModal
          address={(editing as Address).id ? (editing as Address) : undefined}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load();
          }}
        />
      )}
    </AccountLayout>
  );
}
