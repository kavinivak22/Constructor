'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, AlertCircle, CheckCircle, Loader2, Mail } from 'lucide-react';
import { useSupabase } from '@/supabase/provider';

export default function UpdatePasswordPage() {
  const router = useRouter();
  const { supabase, user } = useSupabase();
  const [userEmail, setUserEmail] = useState<string | null>(user?.email || null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [status, setStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);

  // Sync userEmail when user state resolves from provider
  useEffect(() => {
    if (user?.email) {
      setUserEmail(user.email);
    }
  }, [user]);

  useEffect(() => {
    async function exchangeAuthTokens() {
      if (typeof window === 'undefined') return;
      const searchParams = new URLSearchParams(window.location.search);
      const code = searchParams.get('code');
      const token_hash = searchParams.get('token_hash');
      const type = (searchParams.get('type') || 'recovery') as any;

      if (code) {
        setIsVerifying(true);
        try {
          const { data, error } = await supabase.auth.exchangeCodeForSession(code);
          if (error) {
            setStatus({ type: 'error', message: error.message });
          } else if (data.user?.email) {
            setUserEmail(data.user.email);
          }
        } finally {
          setIsVerifying(false);
        }
      } else if (token_hash) {
        setIsVerifying(true);
        try {
          const { data, error } = await supabase.auth.verifyOtp({ token_hash, type });
          if (error) {
            setStatus({ type: 'error', message: error.message });
          } else if (data.user?.email) {
            setUserEmail(data.user.email);
          }
        } finally {
          setIsVerifying(false);
        }
      } else {
        // Query user directly from session if already authenticated
        const { data } = await supabase.auth.getUser();
        if (data.user?.email) {
          setUserEmail(data.user.email);
        }
      }
    }

    exchangeAuthTokens();
  }, [supabase]);

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
          {userEmail ? (
            <div className="mt-3 flex items-center justify-center gap-1.5 text-sm text-muted-foreground">
              <span>Account:</span>
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-primary/10 text-primary border border-primary/20">
                <Mail className="h-3 w-3" />
                {userEmail}
              </span>
            </div>
          ) : (
            <p className="mt-2 text-center text-muted-foreground">
              Enter your new password below.
            </p>
          )}
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
              {userEmail && (
                <div className="space-y-1.5">
                  <Label htmlFor="account-email" className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    Account Email
                  </Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="account-email"
                      type="email"
                      disabled
                      value={userEmail}
                      className="pl-9 bg-muted/60 cursor-not-allowed font-medium text-foreground select-all"
                    />
                  </div>
                </div>
              )}
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
