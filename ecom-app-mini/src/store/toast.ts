import { create } from 'zustand';

export type ToastKind = 'default' | 'success' | 'error';

interface Toast {
  id: number;
  text: string;
  kind: ToastKind;
}

interface ToastState {
  toasts: Toast[];
  show: (text: string, kind?: ToastKind) => void;
  dismiss: (id: number) => void;
}

let seq = 0;

export const useToast = create<ToastState>((set, get) => ({
  toasts: [],
  show: (text, kind = 'default') => {
    const id = ++seq;
    set({ toasts: [...get().toasts, { id, text, kind }] });
    window.setTimeout(() => get().dismiss(id), 2200);
  },
  dismiss: (id) => set({ toasts: get().toasts.filter((t) => t.id !== id) }),
}));
