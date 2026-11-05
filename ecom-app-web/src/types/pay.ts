/**
 * Types mirroring the payment API.
 *
 * Kept separate from `types/index.ts` because the payment surface is shared by
 * two client projects and because the launch payload encodes three genuinely
 * different flows (redirect, QR, SDK) that deserve their own named shape.
 */

/** A channel the storefront may offer. */
export interface PayChannelOption {
  code: string;
  name: string;
  provider: string;
  /** True when paying sends the browser to another site. */
  redirect: boolean;
}

/**
 * What to do after the order is created.
 *
 * `mode` is a discriminated union at the edge: the UI switches on it and each
 * branch reads only the field it needs, so a QR flow can never accidentally
 * render a redirect button.
 */
export type PayLaunch =
  | { mode: 'SYNC'; orderNo: string; channelCode: string; channelName: string; tradeNo: string; paid: true; orderStatus: number; amount: number; currency: string; message?: string }
  | { mode: 'REDIRECT'; orderNo: string; channelCode: string; channelName: string; tradeNo: string; url: string; paid: false; orderStatus: number; amount: number; currency: string }
  | { mode: 'QR_CODE'; orderNo: string; channelCode: string; channelName: string; tradeNo: string; qrContent: string; paid: false; orderStatus: number; amount: number; currency: string }
  | { mode: 'SDK'; orderNo: string; channelCode: string; channelName: string; tradeNo: string; sdkParams: Record<string, unknown>; paid: false; orderStatus: number; amount: number; currency: string };
