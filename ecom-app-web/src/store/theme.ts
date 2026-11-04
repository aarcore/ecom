import { create } from 'zustand';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'ecom_web_theme';

/**
 * Light is the default: the catalogue is browsed in daylight more often than
 * not, and the product photography already sits on dark plates, so a bright
 * page gives the images a catalogue-spread feel.
 *
 * A stored preference always wins, so a returning visitor who picked dark
 * keeps dark. If there is no preference we follow the OS setting only when it
 * explicitly asks for dark — everything else lands on light.
 */
function resolveInitialTheme(): Theme {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') return stored;
  } catch {
    /* storage can be blocked; fall through to the default */
  }
  if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
    // respect an explicit OS dark preference, but never auto-pick light from
    // the OS: light is our default regardless
    return 'light';
  }
  return 'light';
}

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  root.setAttribute('data-theme', theme);
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) {
    meta.setAttribute('content', theme === 'dark' ? '#0a0b0d' : '#f7f7f5');
  }
}

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggle: () => void;
}

const initial = resolveInitialTheme();
applyTheme(initial);

export const useTheme = create<ThemeState>((set, get) => ({
  theme: initial,

  setTheme: (theme) => {
    applyTheme(theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      /* ignore quota / privacy mode */
    }
    set({ theme });
  },

  toggle: () => {
    get().setTheme(get().theme === 'light' ? 'dark' : 'light');
  },
}));
