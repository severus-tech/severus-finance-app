import React from 'react';
import { Card } from '@/components/ui/Card';
import { cn, formatINR } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  amount: number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: string;
  color?: 'purple' | 'emerald' | 'amber' | 'rose' | 'indigo' | 'slate';
  tooltip?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  amount,
  subtitle,
  icon: Icon,
  trend,
  color = 'purple',
  tooltip,
}) => {
  const iconGradients = {
    purple: 'bg-purple-900/40 text-purple-400 border-purple-700/50',
    emerald: 'bg-emerald-900/40 text-emerald-400 border-emerald-700/50',
    amber: 'bg-amber-900/40 text-amber-400 border-amber-700/50',
    rose: 'bg-rose-900/40 text-rose-400 border-rose-700/50',
    indigo: 'bg-indigo-900/40 text-indigo-400 border-indigo-700/50',
    slate: 'bg-slate-800 text-slate-400 border-slate-700',
  };

  const amountColors = {
    purple: 'text-purple-300',
    emerald: 'text-emerald-300',
    amber: 'text-amber-300',
    rose: 'text-rose-300',
    indigo: 'text-indigo-300',
    slate: 'text-slate-100',
  };

  return (
    <Card className="p-6 relative overflow-hidden group hover:border-purple-500/50 transition-all duration-300">
      <div className="flex items-start justify-between">
        <div>
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
            {title}
          </span>
          <h4 className={cn('text-2xl font-extrabold tracking-tight', amountColors[color])}>
            {formatINR(amount)}
          </h4>
          {subtitle && <p className="text-xs text-slate-400 mt-1 font-medium">{subtitle}</p>}
          {trend && <p className="text-xs font-semibold text-purple-400 mt-1.5">{trend}</p>}
        </div>
        <div
          className={cn(
            'p-3 rounded-2xl border backdrop-blur-md shadow-md transition-transform duration-300 group-hover:scale-110',
            iconGradients[color]
          )}
          title={tooltip}
        >
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </Card>
  );
};
