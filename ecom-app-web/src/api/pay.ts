import { api } from '@/api/client';
import type { PayChannelOption, PayLaunch } from '@/types/pay';

/**
 * Payment calls used by the checkout and order screens.
 *
 * One place so the three call sites (checkout, order list, order detail) cannot
 * drift on the endpoint shape.
 */
export const payApi = {
  /** Channels an operator has switched on and that are fully configured. */
  channels: () => api.get<PayChannelOption[]>('/app/pay/channels'),

  /**
   * Opens a payment for an existing order.
   *
   * The caller must have created the order first: this deliberately does not
   * create one, so a failed payment never leaves a stray order behind.
   */
  launch: (orderNo: string, channel: string) =>
    api.post<PayLaunch>(`/app/pay/orders/${orderNo}`, { channel }),
};

/**
 * Submits an auto-posting HTML form to a third-party gateway.
 *
 * Alipay returns a complete `<form>` that must be posted, not a URL. Writing it
 * into the document and submitting is the documented flow; there is no way to
 * express it as a plain navigation. The form is parsed in a detached document
 * first so a malformed response cannot inject script into our origin.
 */
export function submitGatewayForm(html: string) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const form = doc.querySelector('form');
  if (!form) {
    throw new Error('支付网关返回的表单无法解析');
  }

  const target = document.createElement('form');
  target.method = form.getAttribute('method') ?? 'POST';
  target.action = form.getAttribute('action') ?? '';
  target.style.display = 'none';

  // Only carry over form fields, never scripts or event handlers.
  form.querySelectorAll('input').forEach((input) => {
    const field = document.createElement('input');
    field.type = 'hidden';
    field.name = input.getAttribute('name') ?? '';
    field.value = input.getAttribute('value') ?? '';
    target.appendChild(field);
  });

  document.body.appendChild(target);
  target.submit();
}
