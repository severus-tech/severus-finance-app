import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Landmark,
  Receipt,
  Users,
  CheckSquare,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export const BottomNav: React.FC = () => {
  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/loans', label: 'Loans', icon: Landmark },
    { to: '/expenses', label: 'Expenses', icon: Receipt },
    { to: '/ledger', label: 'Lending', icon: Users },
    { to: '/todos', label: 'To-do', icon: CheckSquare },
  ];

  return (
    <nav
      className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-slate-950/90 backdrop-blur-xl border-t border-purple-500/20 px-2 py-1.5 shadow-2xl shadow-purple-950/40 select-none"
      style={{ paddingBottom: 'max(0.5rem, env(safe-area-inset-bottom))' }}
      aria-label="Mobile Bottom Navigation"
    >
      <div className="grid grid-cols-5 items-center gap-1 max-w-md mx-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center justify-center gap-1 py-1.5 px-2 rounded-2xl transition-all duration-200 text-[10px] font-semibold min-h-[46px]',
                isActive
                  ? 'text-purple-300 bg-purple-600/20 border border-purple-500/30 shadow-sm shadow-purple-900/30'
                  : 'text-slate-400 hover:text-slate-200 active:scale-95'
              )
            }
          >
            <item.icon className="w-5 h-5 shrink-0" />
            <span className="truncate leading-none">{item.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
};
