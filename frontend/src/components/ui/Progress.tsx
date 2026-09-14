import React from 'react';
import { cn } from '@/lib/utils';

export interface ProgressProps extends React.HTMLAttributes<HTMLDivElement> {
  value: number; // 0 - 100
  color?: 'purple' | 'emerald' | 'amber' | 'rose';
}

export const Progress: React.FC<ProgressProps> = ({ className, value, color = 'purple', ...props }) => {
  const clampedValue = Math.min(100, Math.max(0, value));

  const colors = {
    purple: 'bg-gradient-to-r from-purple-600 to-indigo-500',
    emerald: 'bg-gradient-to-r from-emerald-600 to-teal-500',
    amber: 'bg-gradient-to-r from-amber-600 to-orange-500',
    rose: 'bg-gradient-to-r from-rose-600 to-red-500',
  };

  return (
    <div className={cn('w-full bg-slate-800/80 rounded-full h-2.5 overflow-hidden p-0.5 border border-slate-700/50', className)} {...props}>
      <div
        className={cn('h-full rounded-full transition-all duration-500 ease-out', colors[color])}
        style={{ width: `${clampedValue}%` }}
      />
    </div>
  );
};
