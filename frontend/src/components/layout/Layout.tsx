import React from 'react';
import { Header } from './Header';
import { BottomNav } from './BottomNav';

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 selection:bg-purple-600 selection:text-white">
      {/* Top Header with Brand, Desktop Top Nav, Quick Add, and Profile Menu */}
      <Header />

      {/* Main Content with bottom padding to prevent mobile navbar overlap */}
      <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto space-y-6 pb-28 sm:pb-24 md:pb-12 animate-in fade-in duration-300">
        {children}
      </main>

      {/* Mobile Bottom Navigation Bar (md:hidden) */}
      <BottomNav />
    </div>
  );
};
