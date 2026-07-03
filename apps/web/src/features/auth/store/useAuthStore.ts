import { create } from 'zustand';
import * as userApi from '@/features/user/api/user.api';
import type { User } from '@/shared/types/api';

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

export const useAuthStore = create<AuthState>((set) => ({
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
    if (typeof window !== "undefined") {
      document.cookie = "is_logged_in=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 UTC;";
    }
    set({ user: null, isAuthenticated: false, isLoading: false });
  },

  isLoginModalOpen: false,
  setLoginModalOpen: (open) => set({ isLoginModalOpen: open }),
}));

let sessionCheckPromise: Promise<void> | null = null;
let sessionCheckToken = 0;
