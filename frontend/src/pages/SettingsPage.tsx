import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { AppSettings } from '@/types';
import { useTheme } from '@/context/ThemeContext';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Skeleton } from '@/components/ui/Skeleton';
import { Settings, Lock, User, Save, ShieldCheck } from 'lucide-react';
import { toast } from 'sonner';

export const SettingsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { theme } = useTheme();

  const [monthlySalary, setMonthlySalary] = useState('');
  const [salaryCreditDay, setSalaryCreditDay] = useState('1');
  const [displayName, setDisplayName] = useState('');
  const [savingSettings, setSavingSettings] = useState(false);

  // Security State
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);

  const { data: settings, isLoading } = useQuery<AppSettings>({
    queryKey: ['settings'],
    queryFn: async () => {
      const res = await api.get('/api/v1/settings');
      return res.data;
    },
  });

  useEffect(() => {
    if (settings) {
      setMonthlySalary(settings.monthlySalary.toString());
      setSalaryCreditDay(settings.salaryCreditDay.toString());
      setDisplayName(settings.displayName || '');
    }
  }, [settings]);

  const handleUpdateSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingSettings(true);

    try {
      await api.put('/api/v1/settings', {
        monthlySalary: parseFloat(monthlySalary),
        salaryCreditDay: parseInt(salaryCreditDay),
        displayName,
        currency: 'INR',
        timezone: 'Asia/Kolkata',
        themePreference: settings?.themePreference || theme || 'DARK',
      });

      toast.success('App settings updated successfully!');
      queryClient.invalidateQueries({ queryKey: ['settings'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to update settings');
    } finally {
      setSavingSettings(false);
    }
  };

  const handleUpdatePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match');
      return;
    }
    setSavingPassword(true);

    try {
      await api.post('/api/v1/auth/change-password', {
        currentPassword,
        newPassword,
      });

      toast.success('Password updated successfully!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to update password');
    } finally {
      setSavingPassword(false);
    }
  };

  if (isLoading) return <Skeleton className="h-96 w-full" />;

  return (
    <div className="space-y-8 animate-in fade-in max-w-4xl">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 font-heading tracking-tight">
          Application & Security Settings
        </h1>
        <p className="text-xs text-slate-400">
          Configure monthly income parameters, salary cycle anchor days, and security preferences
        </p>
      </div>

      {/* General Settings */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="w-5 h-5 text-purple-400" />
            Financial Profile & Salary Parameters
          </CardTitle>
          <CardDescription>Defines baseline calculations for remaining salary engine</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleUpdateSettings} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Display Name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required
                placeholder="Severus Administrator"
              />
              <Input
                label="Monthly Salary (₹)"
                type="number"
                step="0.01"
                value={monthlySalary}
                onChange={(e) => setMonthlySalary(e.target.value)}
                required
                placeholder="80000"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Salary Credit Day of Month (1-31)"
                type="number"
                min="1"
                max="31"
                value={salaryCreditDay}
                onChange={(e) => setSalaryCreditDay(e.target.value)}
                required
                helperText="Cycle runs from credit day to day before credit day next month"
              />
              <div className="space-y-1.5">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                  Currency & Timezone
                </label>
                <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-300 font-semibold flex items-center justify-between">
                  <span>INR (₹)</span>
                  <span className="text-purple-400">Asia/Kolkata (IST)</span>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-4 border-t border-purple-900/20">
              <Button variant="primary" type="submit" isLoading={savingSettings}>
                <Save className="w-4 h-4" />
                Save Profile Parameters
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Security & Credentials Settings */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Lock className="w-5 h-5 text-purple-400" />
            Account Security & Access Key
          </CardTitle>
          <CardDescription>Update your single-user access credentials</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleUpdatePassword} className="space-y-4">
            <Input
              label="Current Password"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="New Password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
              <Input
                label="Confirm New Password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>

            <div className="flex justify-end pt-4 border-t border-purple-900/20">
              <Button variant="outline" type="submit" isLoading={savingPassword}>
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                Update Password
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};
