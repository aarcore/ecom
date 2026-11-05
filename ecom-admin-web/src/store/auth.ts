import { create } from 'zustand';
import { api, tokenStorage } from '@/api/client';

export interface AdminProfile {
  id: number;
  username: string;
  nickname: string;
  avatar?: string;
  phone?: string;
  email?: string;
  role: string;
  lastLoginAt?: string;
}

interface LoginResponse {
  token: string;
  expiresIn: number;
  userId: number;
  username: string;
  nickname: string;
  avatar?: string;
  role: string;
}

interface AuthState {
  token: string | null;
  profile: AdminProfile | null;
  loading: boolean;
  /** True once the first profile fetch settled, successfully or not. */
  ready: boolean;
  login: (username: string, password: string) => Promise<void>;
  loadProfile: () => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: tokenStorage.get(),
  profile: null,
  loading: false,
  ready: false,

  login: async (username, password) => {
    set({ loading: true });
    try {
      const result = await api.post<LoginResponse>('/admin/auth/login', { username, password });
      tokenStorage.set(result.token);
      set({ token: result.token, ready: false, profile: null });
    } finally {
      set({ loading: false });
    }
  },

  loadProfile: async () => {
    // avoid firing the same request on every render of a mounted component
    if (get().loading) return;
    const token = tokenStorage.get();
    if (!token) {
      set({ profile: null, ready: true });
      return;
    }
    set({ loading: true });
    try {
      const profile = await api.get<AdminProfile>('/admin/auth/profile');
      set({ profile, ready: true });
    } catch {
      // the axios interceptor already cleared a rejected token; either way we
      // must stop blocking the UI or the shell would spin forever
      set({ profile: null, token: tokenStorage.get(), ready: true });
    } finally {
      set({ loading: false });
    }
  },

  logout: () => {
    tokenStorage.clear();
    set({ token: null, profile: null, ready: true });
  },
}));
