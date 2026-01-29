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
  
  // Actions
  setUser: (user: User | null) => void;
  checkSession: () => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true, // Initial load checks for session

  setUser: (user) => set({ 
    user, 
    isAuthenticated: !!user, 
    isLoading: false 
  }),
  
  checkSession: async () => {
    const { isLoading, isAuthenticated } = useAuthStore.getState();
    if (isLoading && isAuthenticated) return; // 이미 로딩 중이거나 인증된 경우 스킵 (초기값 isLoading: true 고려)
    
    // 이미 데이터가 있고 로딩 중이 아니면 굳이 다시 부르지 않음 (필요 시 강제 새로고침 로직 별도 구성)
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
}));
