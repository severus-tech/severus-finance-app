import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { AlertTriangle, Home } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-6 text-center">
      <div className="p-4 rounded-3xl bg-purple-950/60 border border-purple-800/40 text-purple-400 mb-6 animate-bounce">
        <AlertTriangle className="w-12 h-12" />
      </div>
      <h1 className="text-4xl font-black text-slate-100 font-heading mb-2">404 - Page Not Found</h1>
      <p className="text-sm text-slate-400 max-w-sm mb-8">
        The financial page or resource you requested does not exist or has been moved.
      </p>
      <Button variant="primary" size="lg" onClick={() => navigate('/dashboard')}>
        <Home className="w-4 h-4" />
        Return to Dashboard
      </Button>
    </div>
  );
};
