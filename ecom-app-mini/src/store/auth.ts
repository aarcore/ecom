import { create } from 'zustand';
import { api, tokenStorage } from '@/api/client';
import type { Member } from '@/types';

interface LoginResult {
  token: string;
  expiresIn: number;
  userId: number;
  username: string;
  nickname: string;
  avatar?: string;
  newAccount?: boolean;
}

export interface MineData {
  profile: Member;
  orderCounts: Record<string, number>;
  cartCount: number;
  couponCount: number;
  addressCount: number;
}

interface AuthState {
  token: string | null;
  profile: Member | null;
  cartCount: number;
  orderCounts: Record<string, number>;
  couponCount: number;
  login: (mobile: string, password: string) => Promise<LoginResult>;
  loginWithCode: (mobile: string, smsCode: string) => Promise<LoginResult>;
  loginWithWechat: (code: string, nickname?: string) => Promise<LoginResult>;
  refresh: () => Promise<void>;
  refreshCart: () => Promise<void>;
  updateProfile: (patch: Partial<Member>) => Promise<void>;
  logout: () => void;
}

export const useAuth = create<AuthState>((set, get) => ({
  token: tokenStorage.get(),
  profile: null,
  cartCount: 0,
  orderCounts: {},
  couponCount: 0,

  login: async (mobile, password) => {
    const result = await api.post<LoginResult>('/app/auth/login', { mobile, password }, { });
    tokenStorage.set(result.token);
    set({ token: result.token });
    await get().refresh();
    return result;
  },

  loginWithCode: async (mobile, smsCode) => {
    const result = await api.post<LoginResult>('/app/auth/login', { mobile, smsCode });
    tokenStorage.set(result.token);
    set({ token: result.token });
    await get().refresh();
    return result;
  },

  loginWithWechat: async (code, nickname) => {
    const result = await api.post<LoginResult>('/app/auth/login/mp', { code, nickname });
    tokenStorage.set(result.token);
    set({ token: result.token });
    await get().refresh();
    return result;
  },

  refresh: async () => {
    if (!tokenStorage.get()) return;
    try {
      const mine = await api.get<MineData>('/app/auth/mine');
      set({
        profile: mine.profile,
        cartCount: mine.cartCount,
        orderCounts: mine.orderCounts ?? {},
        couponCount: mine.couponCount,
      });
    } catch {
      set({ profile: null });
    }
  },

  refreshCart: async () => {
    if (!tokenStorage.get()) return;
    try {
      const count = await api.get<number>('/app/cart/count');
      set({ cartCount: count });
    } catch {
      /* badge is decorative; ignore */
    }
  },

  updateProfile: async (patch) => {
    const profile = await api.put<Member>('/app/auth/profile', patch);
    set({ profile });
  },

  logout: () => {
    tokenStorage.clear();
    set({ token: null, profile: null, cartCount: 0, orderCounts: {}, couponCount: 0 });
  },
}));
