import { useState } from 'react';
import { api, attempt } from '@/api/client';
import { useT } from '@/store/locale';
import type { Address } from '@/types';

const BLANK = {
  receiver: '',
  mobile: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0,
};

type FormState = typeof BLANK;

/**
 * Create/edit form for a shipping address.
 *
 * Shared by the checkout page and the address book, which had grown two copies
 * of the same markup. Keeping one copy is what makes "the address form is
 * translated" a single fact to verify rather than two.
 */
export default function AddressModal({
  address,
  onClose,
  onSaved,
}: {
  /** Omit to create, pass a row to edit. */
  address?: Address;
  onClose: () => void;
  onSaved: (address: Address) => void;
}) {
  const { t } = useT();
  const [form, setForm] = useState<FormState>(
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
      : BLANK,
  );
  const [busy, setBusy] = useState(false);

  const save = async () => {
    if (!form.receiver || !form.mobile || !form.detail) {
      await attempt(() => Promise.reject(new Error(t('address.required'))));
      return;
    }
    setBusy(true);
    const saved = await attempt(
      () =>
        address
          ? api.put<Address>(`/app/addresses/${address.id}`, form)
          : api.post<Address>('/app/addresses', form),
      t('address.saved'),
    );
    setBusy(false);
    if (saved) onSaved(saved);
  };

  return (
    <div className="modal-mask" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-title">{address ? t('address.edit') : t('address.new')}</div>
        <div className="stack">
          <label className="field">
            <span className="field-label">{t('address.receiver')}</span>
            <input
              className="input"
              value={form.receiver}
              onChange={(e) => setForm({ ...form, receiver: e.target.value })}
              placeholder={t('address.receiverPlaceholder')}
            />
          </label>
          <label className="field">
            <span className="field-label">{t('address.mobile')}</span>
            <input
              className="input"
              value={form.mobile}
              inputMode="numeric"
              onChange={(e) =>
                setForm({ ...form, mobile: e.target.value.replace(/\D/g, '').slice(0, 11) })
              }
              placeholder={t('address.mobilePlaceholder')}
            />
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
            <input
              className="input"
              value={form.province}
              onChange={(e) => setForm({ ...form, province: e.target.value })}
              placeholder={t('address.province')}
              aria-label={t('address.province')}
            />
            <input
              className="input"
              value={form.city}
              onChange={(e) => setForm({ ...form, city: e.target.value })}
              placeholder={t('address.city')}
              aria-label={t('address.city')}
            />
            <input
              className="input"
              value={form.district}
              onChange={(e) => setForm({ ...form, district: e.target.value })}
              placeholder={t('address.district')}
              aria-label={t('address.district')}
            />
          </div>
          <label className="field">
            <span className="field-label">{t('address.detail')}</span>
            <textarea
              className="textarea"
              rows={3}
              value={form.detail}
              onChange={(e) => setForm({ ...form, detail: e.target.value })}
              placeholder={t('address.detailPlaceholder')}
            />
          </label>
          <button
            className="row"
            style={{ gap: 9, fontSize: 13 }}
            onClick={() => setForm({ ...form, isDefault: form.isDefault ? 0 : 1 })}
          >
            <span className={`checkbox ${form.isDefault ? 'on' : ''}`}>✓</span>
            {t('address.setDefault')}
          </button>
        </div>
        <div className="row" style={{ gap: 10, marginTop: 22 }}>
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onClose}>
            {t('common.cancel')}
          </button>
          <button className="btn btn-primary" style={{ flex: 1 }} disabled={busy} onClick={save}>
            {busy ? t('common.saving') : t('address.save')}
          </button>
        </div>
      </div>
    </div>
  );
}
