import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { CanceledError } from 'axios';
import * as userApi from '@/features/user/api/user.api';
import type { User } from '@/shared/types/api';
import { isUnauthenticated } from '@/shared/api/auth-error';
import { resetSessionState } from '@/shared/api/client';

const SESSION_CHECK_RETRY_DELAYS_MS = [250, 500] as const;
const waitForSessionRetry = (delayMs: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, delayMs));

let sessionCheckPromise: Promise<void> | null = null;
let profileRequests: Record<'silent' | 'regular', Promise<User> | null> = { silent: null, regular: null };

const getSafeStorage = (): Storage => {
  if (typeof window !== 'undefined' && typeof window.localStorage !== 'undefined') {
    try {
      const testKey = '__storage_test__';
      window.localStorage.setItem(testKey, testKey);
      window.localStorage.removeItem(testKey);
      return window.localStorage;
    } catch {
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
  hasCheckedSession: boolean;
  lastProfileCheckAt: number;
  sessionVersion: number;
  isLoginModalOpen: boolean;
  setUser: (user: User | null) => void;
  refreshUser: (options?: { silentAuthFailure?: boolean }) => Promise<User>;
  checkSession: () => Promise<void>;
  logout: () => void;
  setLoginModalOpen: (open: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => {
      const applyUser = (user: User | null, invalidateRequests: boolean) => {
        const identityChanged = get().user?.id !== user?.id;
        if (identityChanged) resetSessionState();
        if (invalidateRequests || identityChanged) {
          sessionCheckPromise = null;
          profileRequests = { silent: null, regular: null };
        }
        set({
          user,
          isAuthenticated: !!user,
          isLoading: false,
          hasCheckedSession: true,
          lastProfileCheckAt: Date.now(),
          sessionVersion: get().sessionVersion + (invalidateRequests || identityChanged ? 1 : 0),
        });
      };
      return {
        user: null,
        isAuthenticated: false,
        isLoading: true,
        hasCheckedSession: false,
        lastProfileCheckAt: 0,
        sessionVersion: 0,

        setUser: (user) => applyUser(user, true),

        refreshUser: (options = {}) => {
          const mode = options.silentAuthFailure ? 'silent' : 'regular';
          if (profileRequests[mode]) return profileRequests[mode];
          const version = get().sessionVersion;
          const request = userApi.getMyProfile(options)
            .then((user) => {
              if (version !== get().sessionVersion) {
                const current = get();
                // A parallel check may have verified this account; keep its current data.
                if (current.isAuthenticated && current.user?.id === user?.id) return current.user;
                throw new CanceledError('Session changed before profile update');
              }
              applyUser(user, false);
              return user;
            })
            .catch((error: unknown) => {
              if (version !== get().sessionVersion) throw new CanceledError('Session changed before profile update');
              if (isUnauthenticated(error)) get().setUser(null);
              else if (mode === 'regular') set({ hasCheckedSession: true, isLoading: false });
              throw error;
            })
            .finally(() => {
              if (profileRequests[mode] === request) profileRequests[mode] = null;
            });
          profileRequests[mode] = request;
          return request;
        },

        checkSession: () => {
          if (sessionCheckPromise) return sessionCheckPromise;
          const version = get().sessionVersion;
          set({ isLoading: get().user === null });
          const check = (async () => {
            for (let attempt = 0; attempt <= SESSION_CHECK_RETRY_DELAYS_MS.length; attempt += 1) {
              try {
                await get().refreshUser({ silentAuthFailure: true });
                return;
              } catch {
                if (version !== get().sessionVersion) return;
                const retryDelay = SESSION_CHECK_RETRY_DELAYS_MS[attempt];
                if (retryDelay === undefined) {
                  set({ isLoading: false, hasCheckedSession: true });
                  return;
                }
                await waitForSessionRetry(retryDelay);
                if (version !== get().sessionVersion) return;
              }
            }
          })().finally(() => {
            if (sessionCheckPromise === check) sessionCheckPromise = null;
          });
          sessionCheckPromise = check;
          return check;
        },

        logout: () => {
          const hadUser = get().user !== null;
          get().setUser(null);
          if (!hadUser) resetSessionState();
        },

        isLoginModalOpen: false,
        setLoginModalOpen: (open) => set({ isLoginModalOpen: open }),
      };
    },
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
          hasCheckedSession: false,
          lastProfileCheckAt: 0,
        };
      },
    }
  )
);

if (typeof window !== 'undefined') useAuthStore.persist.rehydrate();
