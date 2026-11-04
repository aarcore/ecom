import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import { useToast } from '@/store/toast';

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

const TOKEN_KEY = 'ecom_app_token';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/**
 * Detects whether we are running inside the WeChat webview. The mini program
 * passes its own token via `?token=` on first load, which we absorb here.
 */
export const platform = (() => {
  const ua = navigator.userAgent.toLowerCase();
  if (ua.includes('miniprogram') || ua.includes('micromessenger')) return 'mp';
  if (new URLSearchParams(window.location.search).get('platform') === 'mp') return 'mp';
  return 'web';
})();

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
});

http.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers['X-Client-Platform'] = platform;
  return config;
});

const AUTH_EVENT = 'ecom:app:unauthorized';

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

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiEnvelope<T>>(config);
  const body = response.data;
  if (body && typeof body.code === 'number' && body.code !== 200) {
    if (body.code === 401) {
      tokenStorage.clear();
      window.dispatchEvent(new CustomEvent(AUTH_EVENT));
    }
    throw new Error(body.message || '请求失败');
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

/** Runs `fn`, showing a toast on failure. Returns null when it throws. */
export async function attempt<T>(fn: () => Promise<T>, successText?: string): Promise<T | null> {
  try {
    const result = await fn();
    if (successText) useToast.getState().show(successText);
    return result;
  } catch (error) {
    useToast.getState().show(error instanceof Error ? error.message : '网络异常，请稍后再试', 'error');
    return null;
  }
}

/** Absorbs a token handed over by the mini program container. */
export function absorbTokenFromQuery() {
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token');
  if (token) {
    tokenStorage.set(token);
    params.delete('token');
    const query = params.toString();
    window.history.replaceState({}, '', `${window.location.pathname}${query ? `?${query}` : ''}`);
  }
}
