import React from 'react';
import { cn } from '@/lib/utils';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'purple' | 'emerald' | 'amber' | 'rose' | 'slate' | 'indigo';
}

export const Badge: React.FC<BadgeProps> = ({ className, variant = 'purple', children, ...props }) => {
  const variants = {
    purple: 'bg-purple-950/80 text-purple-300 border-purple-800/50',
    emerald: 'bg-emerald-950/80 text-emerald-300 border-emerald-800/50',
    amber: 'bg-amber-950/80 text-amber-300 border-amber-800/50',
    rose: 'bg-rose-950/80 text-rose-300 border-rose-800/50',
    slate: 'bg-slate-800 text-slate-300 border-slate-700',
    indigo: 'bg-indigo-950/80 text-indigo-300 border-indigo-800/50',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border tracking-wide uppercase',
        variants[variant],
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
};
