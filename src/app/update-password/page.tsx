'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, AlertCircle, CheckCircle } from 'lucide-react';
import { useSupabase } from '@/supabase/provider';

export default function UpdatePasswordPage() {
  const router = useRouter();
  const { supabase } = useSupabase();
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [status, setStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleUpdatePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setStatus({ type: 'error', message: 'Passwords do not match.' });
      return;
    }
    setIsLoading(true);
    setStatus(null);
    try {
      const { error } = await supabase.auth.updateUser({ password });
      if (error) throw error;
      setStatus({ type: 'success', message: 'Password updated successfully. You can now log in with your new password.' });
      setTimeout(() => router.push('/login'), 3000);
    } catch (error: any) {
      setStatus({ type: 'error', message: error.message || 'An unexpected error occurred.' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-background">
      <div className="mx-auto w-full max-w-md space-y-8">
        <div>
          <div className="flex justify-center mb-6 items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
              <Building2 className="h-6 w-6" />
            </div>
            <span className="text-2xl font-semibold font-headline text-foreground">Constructor</span>
          </div>
          <h2 className="mt-6 text-center text-3xl font-bold tracking-tight text-foreground font-headline">
            Update Your Password
          </h2>
          <p className="mt-2 text-center text-muted-foreground">
            Enter your new password below.
          </p>
        </div>

        {status?.type === 'success' ? (
          <div className="rounded-lg border-l-4 border-green-500 bg-green-50 p-4 text-green-700">
            <div className="flex items-center gap-3">
              <CheckCircle className="h-5 w-5" />
              <div>
                <h3 className="font-bold">Success!</h3>
                <p className="text-sm">{status.message}</p>
              </div>
            </div>
          </div>
        ) : (
          <form onSubmit={handleUpdatePassword} className="space-y-6">
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="password">New Password</Label>
                <Input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirm-password">Confirm New Password</Label>
                <Input
                  id="confirm-password"
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>
            </div>
            {status?.type === 'error' && (
              <div className="flex items-center gap-2 text-sm text-destructive">
                <AlertCircle className="h-4 w-4" />
                <span>{status.message}</span>
              </div>
            )}
            <div>
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? 'Updating...' : 'Update Password'}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
