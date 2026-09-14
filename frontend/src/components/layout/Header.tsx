import React, { useState, useRef, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { useTheme } from '@/context/ThemeContext';
import { Button } from '@/components/ui/Button';
import { QuickAddDialog } from '@/components/common/QuickAddDialog';
import {
  Wallet,
  Plus,
  Sun,
  Moon,
  Settings,
  LogOut,
  LayoutDashboard,
  Landmark,
  Receipt,
  Users,
  CheckSquare,
  User as UserIcon,
  ChevronDown,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const [quickAddOpen, setQuickAddOpen] = useState(false);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);

  // Close profile dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        profileMenuRef.current &&
        !profileMenuRef.current.contains(event.target as Node)
      ) {
        setProfileMenuOpen(false);
      }
    };
    if (profileMenuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [profileMenuOpen]);

  const desktopNavItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/loans', label: 'Loans', icon: Landmark },
    { to: '/expenses', label: 'Expenses', icon: Receipt },
    { to: '/ledger', label: 'Lending', icon: Users },
    { to: '/todos', label: 'To-do', icon: CheckSquare },
  ];

  const userInitial = (user?.displayName || user?.email || 'S').charAt(0).toUpperCase();

  return (
    <>
      <header className="sticky top-0 z-30 bg-slate-950/85 backdrop-blur-xl border-b border-purple-900/25 px-4 sm:px-8 py-3.5 flex items-center justify-between">
        {/* Brand / Logo */}
        <div
          className="flex items-center gap-3 cursor-pointer select-none"
          onClick={() => navigate('/dashboard')}
        >
          <div className="p-2 rounded-2xl bg-gradient-to-tr from-purple-600 to-indigo-600 text-white shadow-md shadow-purple-950/50 border border-purple-400/20">
            <Wallet className="w-5 h-5" />
          </div>
          <div>
            <span className="text-lg font-black tracking-tight font-heading text-slate-100 block leading-tight">
              SEVERUS
            </span>
            <span className="text-[10px] font-bold text-purple-400 tracking-wider uppercase hidden sm:block">
              FINANCE
            </span>
          </div>
        </div>

        {/* Desktop Top Navigation Bar */}
        <nav className="hidden md:flex items-center gap-1 bg-slate-900/80 p-1 rounded-2xl border border-purple-900/20">
          {desktopNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-200',
                  isActive
                    ? 'bg-purple-600/25 text-purple-300 border border-purple-500/30 shadow-sm shadow-purple-900/30 font-bold'
                    : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/50'
                )
              }
            >
              <item.icon className="w-4 h-4 shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        {/* Right Controls */}
        <div className="flex items-center gap-2.5 sm:gap-3">
          {/* Quick Add Button */}
          <Button
            variant="primary"
            size="sm"
            onClick={() => setQuickAddOpen(true)}
            className="rounded-xl shadow-md shadow-purple-950/40 min-h-[40px]"
          >
            <Plus className="w-4 h-4" />
            <span className="hidden sm:inline font-semibold">Quick Add</span>
          </Button>

          {/* Theme Toggle Button */}
          <button
            onClick={toggleTheme}
            className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-purple-300 hover:border-purple-500/40 transition-colors min-h-[40px] min-w-[40px] flex items-center justify-center"
            title="Toggle theme"
            aria-label="Toggle theme"
          >
            {theme === 'DARK' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>

          {/* Profile / Avatar Menu Dropdown */}
          <div className="relative" ref={profileMenuRef}>
            <button
              onClick={() => setProfileMenuOpen((prev) => !prev)}
              className="flex items-center gap-2 p-1 sm:p-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-purple-500/40 transition-colors focus:outline-none focus:ring-2 focus:ring-purple-500/40 min-h-[40px]"
              aria-label="User Profile Menu"
              aria-expanded={profileMenuOpen}
            >
              <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-purple-700 to-indigo-600 flex items-center justify-center text-xs font-bold text-white shadow-inner">
                {userInitial}
              </div>
              <ChevronDown
                className={cn(
                  'w-3.5 h-3.5 text-slate-400 transition-transform duration-200 hidden sm:block',
                  profileMenuOpen && 'rotate-180 text-purple-300'
                )}
              />
            </button>

            {/* Dropdown Menu Card */}
            {profileMenuOpen && (
              <div className="absolute right-0 mt-2 w-56 glass-card bg-slate-900/95 border border-purple-500/30 rounded-2xl shadow-2xl p-2 z-50 animate-in fade-in zoom-in-95 duration-150">
                <div className="px-3 py-2.5 border-b border-purple-900/20">
                  <p className="text-xs font-bold text-slate-100 truncate">
                    {user?.displayName || 'Severus User'}
                  </p>
                  <p className="text-[11px] text-slate-400 truncate mt-0.5">
                    {user?.email || ''}
                  </p>
                </div>

                <div className="py-1 space-y-0.5">
                  <button
                    onClick={() => {
                      setProfileMenuOpen(false);
                      navigate('/settings');
                    }}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-slate-300 hover:text-slate-100 hover:bg-slate-800/70 rounded-xl transition-colors min-h-[40px]"
                  >
                    <Settings className="w-4 h-4 text-purple-400" />
                    <span>Settings</span>
                  </button>

                  <button
                    onClick={() => {
                      setProfileMenuOpen(false);
                      logout();
                    }}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-rose-400 hover:text-rose-300 hover:bg-rose-950/40 rounded-xl transition-colors min-h-[40px]"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Log Out</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <QuickAddDialog isOpen={quickAddOpen} onClose={() => setQuickAddOpen(false)} />
    </>
  );
};
