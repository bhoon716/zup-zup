import { create } from 'zustand';

interface User {
  email: string;
  name: string;
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  
  // Actions
  setUser: (user: User) => void;
  checkSession: () => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true, // Initial load checks for session

  setUser: (user) => set({ user, isAuthenticated: true, isLoading: false }),
  
  checkSession: async () => {
    // TODO: Call Backend API (/api/users/me) to validate session
    // For now, we simulate a check or assume cookie-based
    try {
      // Mock API Call - Replace with actual `api.get('/users/me')`
      // const { data } = await api.get('/users/me');
      // set({ user: data, isAuthenticated: true, isLoading: false });
      
      // Temporary: Should be implemented when Backend API is ready
      set({ isLoading: false }); 
    } catch (error) {
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },

  logout: () => {
    // TODO: Call Backend Logout Endpoint
    set({ user: null, isAuthenticated: false, isLoading: false });
  },
}));
