import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import * as userApi from '@/features/user/api/user.api';
import type { User } from '@/shared/types/api';
import { deleteCookie, IS_LOGGED_IN_COOKIE_NAME } from '@/shared/lib/cookie';
import { isDefinitiveAuthFailure } from '@/shared/api/auth-error';

const SESSION_CHECK_RETRY_DELAYS_MS = [250, 500] as const;

const waitForSessionRetry = (delayMs: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, delayMs));

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
    (set, get) => ({
      user: null,
      isAuthenticated: false,
      isLoading: true,

      setUser: (user) => set({ 
        user, 
        isAuthenticated: !!user, 
        isLoading: false 
      }),
      
      checkSession: async () => {
        if (sessionCheckPromise) {
          return sessionCheckPromise;
        }

        set({ isLoading: get().user === null });
        const requestToken = ++sessionCheckToken;
        sessionCheckPromise = (async () => {
          for (let attempt = 0; attempt <= SESSION_CHECK_RETRY_DELAYS_MS.length; attempt += 1) {
            try {
              const response = await userApi.getMyProfile({ silentAuthFailure: true });
              if (requestToken !== sessionCheckToken) {
                return;
              }
              set({
                user: response.data,
                isAuthenticated: true,
                isLoading: false,
              });
              return;
            } catch (error) {
              if (requestToken !== sessionCheckToken) {
                return;
              }
              if (isDefinitiveAuthFailure(error)) {
                set({
                  user: null,
                  isAuthenticated: false,
                  isLoading: false,
                });
                return;
              }

              const retryDelay = SESSION_CHECK_RETRY_DELAYS_MS[attempt];
              if (retryDelay === undefined) {
                set({ isLoading: false });
                return;
              }

              await waitForSessionRetry(retryDelay);
              if (requestToken !== sessionCheckToken) {
                return;
              }
            }
          }
        })().finally(() => {
          if (requestToken === sessionCheckToken) {
            sessionCheckPromise = null;
          }
        });

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
      merge: (persistedState, currentState) => {
        const restoredUser = (persistedState as Pick<AuthState, 'user'> | undefined)?.user ?? null;
        return {
          ...currentState,
          user: restoredUser,
          isAuthenticated: false,
          isLoading: restoredUser === null,
        };
      },
    }
  )
);

if (typeof window !== 'undefined') {
  useAuthStore.persist.rehydrate();
}

let sessionCheckPromise: Promise<void> | null = null;
let sessionCheckToken = 0;
