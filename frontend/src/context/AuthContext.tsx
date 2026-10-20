import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react';
import { api, setAccessToken } from '@/lib/api';
import { User } from '@/types';
import { toast } from 'sonner';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes
const LAST_ACTIVITY_KEY = 'severus_last_active';
const SESSION_ACTIVE_KEY = 'severus_session_active';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const lastThrottleRef = useRef<number>(0);

  const performClientLogout = useCallback(async (notifyMessage?: string) => {
    sessionStorage.removeItem(SESSION_ACTIVE_KEY);
    localStorage.removeItem(LAST_ACTIVITY_KEY);
    setAccessToken(null);
    setUser(null);
    try {
      await api.post('/api/v1/auth/logout');
    } catch {
      // Ignore network errors on logout
    }
    if (notifyMessage) {
      toast.error(notifyMessage);
    }
  }, []);

  const initAuth = async () => {
    const sessionActive = sessionStorage.getItem(SESSION_ACTIVE_KEY);
    const lastActiveStr = localStorage.getItem(LAST_ACTIVITY_KEY);
    const now = Date.now();

    // If reopening a closed tab/browser (sessionStorage is empty) OR inactivity expired (>10 mins)
    const isExpired = !lastActiveStr || (now - parseInt(lastActiveStr, 10) > INACTIVITY_TIMEOUT_MS);

    if (!sessionActive || isExpired) {
      sessionStorage.removeItem(SESSION_ACTIVE_KEY);
      localStorage.removeItem(LAST_ACTIVITY_KEY);
      setAccessToken(null);
      setUser(null);
      setIsLoading(false);
      try {
        await api.post('/api/v1/auth/logout');
      } catch {
        // Ignore
      }
      return;
    }

    try {
      const res = await api.post('/api/v1/auth/refresh');
      setAccessToken(res.data.accessToken);
      setUser({
        id: res.data.userId,
        email: res.data.email,
        displayName: res.data.displayName,
      });
      sessionStorage.setItem(SESSION_ACTIVE_KEY, 'true');
      localStorage.setItem(LAST_ACTIVITY_KEY, Date.now().toString());
    } catch {
      sessionStorage.removeItem(SESSION_ACTIVE_KEY);
      localStorage.removeItem(LAST_ACTIVITY_KEY);
      setAccessToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    initAuth();

    const handleUnauthorized = () => {
      sessionStorage.removeItem(SESSION_ACTIVE_KEY);
      localStorage.removeItem(LAST_ACTIVITY_KEY);
      setUser(null);
      setAccessToken(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  // Inactivity tracking when logged in
  useEffect(() => {
    if (!user) return;

    const recordActivity = () => {
      const now = Date.now();
      // Throttle localStorage writes to once every 3 seconds
      if (now - lastThrottleRef.current > 3000) {
        lastThrottleRef.current = now;
        localStorage.setItem(LAST_ACTIVITY_KEY, now.toString());
      }
    };

    const checkInactivity = () => {
      const lastActiveStr = localStorage.getItem(LAST_ACTIVITY_KEY);
      const now = Date.now();
      if (!lastActiveStr || (now - parseInt(lastActiveStr, 10) > INACTIVITY_TIMEOUT_MS)) {
        performClientLogout('Logged out due to 10 minutes of inactivity. Please log in again.');
      }
    };

    // Activity event listeners
    const activityEvents = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'];
    activityEvents.forEach((ev) => window.addEventListener(ev, recordActivity, { passive: true }));

    // Periodic check every 5 seconds
    const intervalId = window.setInterval(checkInactivity, 5000);

    // Tab visibility change check (when tab is reopened or focused)
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        checkInactivity();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);

    // Cross-tab synchronization
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === LAST_ACTIVITY_KEY && !e.newValue) {
        performClientLogout();
      }
    };
    window.addEventListener('storage', handleStorageChange);

    return () => {
      activityEvents.forEach((ev) => window.removeEventListener(ev, recordActivity));
      window.clearInterval(intervalId);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, [user, performClientLogout]);

  const login = async (email: string, password: string) => {
    const res = await api.post('/api/v1/auth/login', { email, password });
    setAccessToken(res.data.accessToken);
    sessionStorage.setItem(SESSION_ACTIVE_KEY, 'true');
    localStorage.setItem(LAST_ACTIVITY_KEY, Date.now().toString());
    setUser({
      id: res.data.userId,
      email: res.data.email,
      displayName: res.data.displayName,
    });
  };

  const logout = async () => {
    await performClientLogout();
  };

  const refreshUser = async () => {
    try {
      const res = await api.get('/api/v1/auth/me');
      setUser(res.data);
    } catch {
      // Ignore me check errors
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
