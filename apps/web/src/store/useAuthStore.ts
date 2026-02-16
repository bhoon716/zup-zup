import { create } from 'zustand';
import * as userApi from '@/lib/api/user';

interface User {
  id: number;
  email: string;
  name: string;
  role: string;
}

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
    const { isLoading, isAuthenticated } = useAuthStore.getState();
    // 이미 인증이 완료된 상태면 중복 조회를 피한다.
    if (isLoading && isAuthenticated) return;

    if (isAuthenticated) {
      set({ isLoading: false });
      return;
    }

    set({ isLoading: true });
    try {
      const response = await userApi.getMyProfile();
      set({ 
        user: response.data, 
        isAuthenticated: true, 
        isLoading: false 
      });
    } catch {
      set({ 
        user: null,
        isAuthenticated: false,
        isLoading: false 
      });
    }
  },

  logout: () => {
    set({ user: null, isAuthenticated: false, isLoading: false });
  },

  isLoginModalOpen: false,
  setLoginModalOpen: (open) => set({ isLoginModalOpen: open }),
}));
