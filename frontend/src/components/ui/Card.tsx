import React from 'react';
import { cn } from '@/lib/utils';

export const Card: React.FC<React.HTMLAttributes<HTMLDivElement> & { variant?: 'default' | 'glass' }> = ({
  className,
  variant = 'glass',
  children,
  ...props
}) => {
  return (
    <div
      className={cn(
        'rounded-2xl transition-all duration-300',
        variant === 'glass'
          ? 'glass-card bg-slate-900/80 border border-purple-900/30 text-slate-100 shadow-xl'
          : 'bg-slate-900 border border-slate-800 text-slate-100 shadow-md',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
};

export const CardHeader: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({ className, children, ...props }) => (
  <div className={cn('p-6 pb-3 border-b border-purple-900/20', className)} {...props}>
    {children}
  </div>
);

export const CardTitle: React.FC<React.HTMLAttributes<HTMLHeadingElement>> = ({ className, children, ...props }) => (
  <h3 className={cn('text-lg font-bold text-slate-100 tracking-tight flex items-center gap-2', className)} {...props}>
    {children}
  </h3>
);

export const CardDescription: React.FC<React.HTMLAttributes<HTMLParagraphElement>> = ({ className, children, ...props }) => (
  <p className={cn('text-xs text-slate-400 mt-1', className)} {...props}>
    {children}
  </p>
);

export const CardContent: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({ className, children, ...props }) => (
  <div className={cn('p-6', className)} {...props}>
    {children}
  </div>
);

export const CardFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({ className, children, ...props }) => (
  <div className={cn('p-6 pt-0 flex items-center justify-end gap-3', className)} {...props}>
    {children}
  </div>
);
