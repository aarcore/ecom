import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import { message } from 'antd';

/** Shape of every backend response. */
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

const TOKEN_KEY = 'ecom_admin_token';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
});

http.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Broadcast auth failures so the shell can redirect without prop drilling. */
const AUTH_EVENT = 'ecom:unauthorized';

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status;
    const code = error.response?.data?.code;
    if (status === 401 || code === 401 || code === 1007) {
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
 * Unwraps the envelope: resolves with `data` on success, rejects with the
 * business message otherwise.
 */
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

/** Calls `fn` and surfaces any failure as a toast; returns null on error. */
export async function withToast<T>(fn: () => Promise<T>, successText?: string): Promise<T | null> {
  try {
    const result = await fn();
    if (successText) message.success(successText);
    return result;
  } catch (error) {
    const text = error instanceof Error ? error.message : '网络异常，请稍后重试';
    message.error(text);
    return null;
  }
}

export const api = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    request<T>({ url, method: 'GET', params }),
  post: <T>(url: string, data?: unknown) => request<T>({ url, method: 'POST', data }),
  put: <T>(url: string, data?: unknown, params?: Record<string, unknown>) =>
    request<T>({ url, method: 'PUT', data, params }),
  delete: <T>(url: string, params?: Record<string, unknown>) =>
    request<T>({ url, method: 'DELETE', params }),
  upload: <T>(url: string, file: File, folder = 'common') => {
    const form = new FormData();
    form.append('file', file);
    form.append('folder', folder);
    return request<T>({
      url,
      method: 'POST',
      data: form,
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export { http };
