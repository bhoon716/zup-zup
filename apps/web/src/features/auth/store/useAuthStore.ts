import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import * as userApi from '@/features/user/api/user.api';
import type { User } from '@/shared/types/api';
import { deleteCookie, IS_LOGGED_IN_COOKIE_NAME } from '@/shared/lib/cookie';

const getSafeStorage = (): Storage => {
  if (typeof window !== 'undefined' && typeof window.localStorage !== 'undefined') {
    try {
      const testKey = '__storage_test__';
      window.localStorage.setItem(testKey, testKey);
      window.localStorage.removeItem(testKey);
      return window.localStorage;
    } catch {
      // ignore and fallback
    }
  }
  
  const store = new Map<string, string>();
  return {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => store.set(key, value),
    removeItem: (key) => store.delete(key),
    clear: () => store.clear(),
    key: (index) => Array.from(store.keys())[index] ?? null,
    get length() { return store.size; }
  };
};

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isLoginModalOpen: boolean;
  setUser: (user: User | null) => void;
  checkSession: () => Promise<void>;
  logout: () => void;
  setLoginModalOpen: (open: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      isLoading: true,

      setUser: (user) => set({ 
        user, 
        isAuthenticated: !!user, 
        isLoading: false 
      }),
      
      checkSession: async () => {
        const { isAuthenticated } = useAuthStore.getState();
        if (sessionCheckPromise) {
          return sessionCheckPromise;
        }

        if (isAuthenticated) {
          set({ isLoading: false });
          return Promise.resolve();
        }

        set({ isLoading: true });
        const requestToken = ++sessionCheckToken;
        sessionCheckPromise = (async () => {
          try {
            const response = await userApi.getMyProfile();
            if (requestToken !== sessionCheckToken) {
              return;
            }
            set({
              user: response.data,
              isAuthenticated: true,
              isLoading: false,
            });
          } catch {
            if (requestToken !== sessionCheckToken) {
              return;
            }
            set({
              user: null,
              isAuthenticated: false,
              isLoading: false,
            });
          } finally {
            if (requestToken === sessionCheckToken) {
              sessionCheckPromise = null;
            }
          }
        })();

        return sessionCheckPromise;
      },

      logout: () => {
        sessionCheckToken += 1;
        sessionCheckPromise = null;
        userApi.clearMyProfileRequestCache();
        deleteCookie(IS_LOGGED_IN_COOKIE_NAME);
        set({ user: null, isAuthenticated: false, isLoading: false });
      },

      isLoginModalOpen: false,
      setLoginModalOpen: (open) => set({ isLoginModalOpen: open }),
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(getSafeStorage),
      partialize: (state) => ({ user: state.user }),
      skipHydration: true,
    }
  )
);

if (typeof window !== 'undefined') {
  useAuthStore.persist.rehydrate();
}

let sessionCheckPromise: Promise<void> | null = null;
let sessionCheckToken = 0;
