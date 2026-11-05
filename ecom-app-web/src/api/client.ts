import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import { useToast } from '@/store/toast';
import { translate, useLocale } from '@/store/locale';
import type { MessageKey } from '@/i18n/messages';

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  page: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}

const TOKEN_KEY = 'ecom_web_token';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/**
 * This client is the desktop web surface, so it always reports `web`. The
 * backend stores the value on orders, letting operations tell browser orders
 * apart from mini-program ones.
 */
const platform = 'web';

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
});

http.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers['X-Client-Platform'] = platform;

  const locale = useLocale.getState().locale;
  // Send the language as a header AND, for reads, as a query parameter.
  //
  // The header alone is not enough: the catalogue GETs are cacheable, and a
  // browser that stored the Chinese response before this feature existed will
  // happily reuse it for a Japanese request because that old entry carries no
  // `Vary`. Putting the language in the URL makes every variant a distinct
  // cache entry, which is the only thing that holds for entries already on
  // disk. Chinese is the source language and needs no marker, so the common
  // path keeps its clean URLs.
  config.headers['X-Lang'] = locale;
  config.headers['Accept-Language'] = locale;
  if (locale !== 'zh-CN' && (config.method ?? 'get').toLowerCase() === 'get') {
    config.params = { ...(config.params ?? {}), lang: locale };
  }
  return config;
});

const AUTH_EVENT = 'ecom:web:unauthorized';

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status;
    const code = error.response?.data?.code;
    if (status === 401 || code === 401) {
      tokenStorage.clear();
      window.dispatchEvent(new CustomEvent(AUTH_EVENT));
    }
    return Promise.reject(error);
  },
);

export const onUnauthorized = (handler: () => void) => {
  const listener = () => handler();
  window.addEventListener(AUTH_EVENT, listener);
  return () => window.removeEventListener(AUTH_EVENT, listener);
};

/**
 * Business codes that the storefront can hit. Server messages are Chinese
 * because the source language of this project is Chinese, so the client keeps
 * its own mapping and only falls back to the server text for codes it does not
 * know (which are admin-only anyway).
 */
const ERROR_KEYS: Record<number, MessageKey> = {
  401: 'error.unauthorized',
  1001: 'error.accountNotFound',
  1002: 'error.passwordError',
  1003: 'error.accountDisabled',
  1005: 'error.phoneExists',
  1006: 'error.smsCodeError',
  2001: 'error.productNotFound',
  2002: 'error.productOffShelf',
  2003: 'error.stockNotEnough',
  2004: 'error.skuNotFound',
  3001: 'error.cartEmpty',
  3002: 'error.orderNotFound',
  3003: 'error.orderStatus',
  3004: 'error.addressNotFound',
  3005: 'error.couponNotAvailable',
  3006: 'error.couponUsed',
  3007: 'error.payFailed',
  3008: 'error.cartItemNotFound',
};

function localizeError(code: number | undefined, fallback: string | undefined): string {
  const locale = useLocale.getState().locale;
  const key = code == null ? undefined : ERROR_KEYS[code];
  if (key) return translate(locale, key);
  return fallback || translate(locale, 'common.requestFailed');
}

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiEnvelope<T>>(config);
  const body = response.data;
  if (body && typeof body.code === 'number' && body.code !== 200) {
    if (body.code === 401) {
      tokenStorage.clear();
      window.dispatchEvent(new CustomEvent(AUTH_EVENT));
    }
    throw new Error(localizeError(body.code, body.message));
  }
  return body?.data as T;
}

export const api = {
  get: <T>(url: string, params?: Record<string, unknown>) => request<T>({ url, method: 'GET', params }),
  post: <T>(url: string, data?: unknown, params?: Record<string, unknown>) =>
    request<T>({ url, method: 'POST', data, params }),
  put: <T>(url: string, data?: unknown, params?: Record<string, unknown>) =>
    request<T>({ url, method: 'PUT', data, params }),
  delete: <T>(url: string) => request<T>({ url, method: 'DELETE' }),
};

/** Runs `fn`, toasting on failure. Returns null when it throws. */
export async function attempt<T>(fn: () => Promise<T>, successText?: string): Promise<T | null> {
  try {
    const result = await fn();
    if (successText) useToast.getState().show(successText);
    return result;
  } catch (error) {
    const locale = useLocale.getState().locale;
    useToast
      .getState()
      .show(error instanceof Error ? error.message : translate(locale, 'common.networkError'), 'error');
    return null;
  }
}
