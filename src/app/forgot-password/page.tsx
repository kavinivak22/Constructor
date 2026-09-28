'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, MailCheck, AlertCircle, ShieldAlert, ArrowLeft } from 'lucide-react';
import { useState } from 'react';
import Image from 'next/image';
import { useSupabase } from '@/supabase/provider';

const RATE_LIMIT_MS = 24 * 60 * 60 * 1000; // 24 hours

function getRateLimitTime(email: string): number | null {
  if (typeof window === 'undefined') return null;
  const ls = localStorage.getItem(`pwd_reset_${email}`);
  if (ls) return parseInt(ls, 10);
  const match = document.cookie.match(new RegExp(`(^|; )last_pwd_reset_${encodeURIComponent(email)}=([^;]+)`));
  if (match && match[2]) {
    return parseInt(match[2], 10);
  }
  return null;
}

function setRateLimitTime(email: string) {
  if (typeof window === 'undefined') return;
  const now = Date.now().toString();
  localStorage.setItem(`pwd_reset_${email}`, now);
  if (typeof document !== 'undefined') {
    const maxAge = 24 * 60 * 60; // 24 hours in seconds
    document.cookie = `last_pwd_reset_${encodeURIComponent(email)}=${now}; path=/; max-age=${maxAge}; SameSite=Lax`;
  }
}

const getFriendlyErrorMessage = (error: any): string => {
  if (error?.message) {
    if (error.message.includes('not find')) {
        return 'No account found with that email address.';
    }
  }
  return 'An unexpected error occurred. Please try again.';
};

export default function ForgotPasswordPage() {
  const { supabase } = useSupabase();
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setStatus(null);

    const cleanEmail = email.toLowerCase().trim();

    // Enforce 1-per-day rate limit
    const lastSent = getRateLimitTime(cleanEmail);
    if (lastSent) {
      const elapsed = Date.now() - lastSent;
      if (elapsed < RATE_LIMIT_MS) {
        const remainingMs = RATE_LIMIT_MS - elapsed;
        const remainingHours = Math.floor(remainingMs / (1000 * 60 * 60));
        const remainingMins = Math.ceil((remainingMs % (1000 * 60 * 60)) / (1000 * 60));
        const timeStr = remainingHours > 0 ? `${remainingHours}h ${remainingMins}m` : `${remainingMins}m`;

        setStatus({
          type: 'error',
          message: `A password reset link was already sent to this email within the last 24 hours. For security, reset links can only be requested once per day. Please check your inbox (including spam) or try again in ${timeStr}.`
        });
        setIsLoading(false);
        return;
      }
    }

    try {
      const origin = typeof window !== 'undefined' && window.location.origin 
        ? window.location.origin 
        : (process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:9002');
      const { error } = await supabase.auth.resetPasswordForEmail(cleanEmail, {
        redirectTo: `${origin}/auth/callback?type=recovery`,
      });
      if (error) throw error;

      // Record rate limit timestamp
      setRateLimitTime(cleanEmail);

      setStatus({ 
        type: 'success', 
        message: 'Password reset link sent successfully.' 
      });
    } catch (error: any) {
      setStatus({ 
        type: 'error', 
        message: getFriendlyErrorMessage(error) 
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full min-h-screen lg:grid lg:grid-cols-2">
      <div className="flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
        <div className="mx-auto w-full max-w-md space-y-8">
            <div>
                <div className="flex justify-start mb-6 items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
                        <Building2 className="h-6 w-6" />
                    </div>
                    <span className="text-2xl font-semibold font-headline text-foreground">Constructor</span>
                </div>
                <h2 className="mt-6 text-3xl font-bold tracking-tight text-foreground font-headline">
                {status?.type === 'success' ? 'Check Your Email' : 'Forgot your password?'}
                </h2>
                <p className="mt-2 text-muted-foreground">
                {status?.type === 'success' 
                  ? 'We have sent you instructions to reset your password.'
                  : "No problem. Enter your email and we'll send you a reset link."
                }
                </p>
            </div>

            {status?.type === 'success' ? (
              <div className="space-y-6">
                <div className="rounded-xl border border-green-200 bg-green-50/80 dark:bg-green-950/20 dark:border-green-900/40 p-5 space-y-4">
                  <div className="flex items-start gap-3.5">
                    <div className="w-10 h-10 rounded-full bg-green-100 dark:bg-green-900/50 text-green-700 dark:text-green-300 flex items-center justify-center shrink-0 mt-0.5">
                      <MailCheck className="h-5 w-5" />
                    </div>
                    <div className="space-y-1">
                      <h3 className="font-bold text-foreground text-base">Reset Link Sent</h3>
                      <p className="text-sm text-muted-foreground leading-relaxed">
                        A secure password reset link was sent to{' '}
                        <span className="font-semibold text-foreground underline decoration-primary/40 underline-offset-2">
                          {email}
                        </span>
                      </p>
                    </div>
                  </div>

                  {/* 24-hour security limit callout */}
                  <div className="rounded-lg bg-background/90 dark:bg-slate-900/80 border border-amber-200/60 dark:border-amber-900/50 p-3.5 flex items-start gap-2.5 text-xs text-muted-foreground">
                    <ShieldAlert className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
                    <div className="space-y-1 leading-normal">
                      <p className="font-semibold text-foreground flex items-center gap-1.5">
                        Security Notice: Sent Once Per Day
                      </p>
                      <p>
                        Password reset links can only be requested <strong>once every 24 hours</strong>. The link remains valid for <strong>1 hour</strong>.
                      </p>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  <p className="text-xs text-muted-foreground text-center">
                    Can't find the email? Be sure to check your <strong>Spam</strong> or <strong>Junk</strong> folder.
                  </p>
                  <Button asChild className="w-full">
                    <Link href="/login">Back to Sign In</Link>
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    className="w-full text-xs text-muted-foreground hover:text-foreground"
                    onClick={() => {
                      setStatus(null);
                    }}
                  >
                    Need to enter a different email?
                  </Button>
                </div>
              </div>
            ) : (
                <form onSubmit={handleResetPassword} className="space-y-6">
                    <div className="space-y-2">
                        <Label htmlFor="email">Email</Label>
                        <Input
                        id="email"
                        type="email"
                        placeholder="m@example.com"
                        required
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>
                    {status?.type === 'error' && (
                        <div className="flex items-center gap-2 text-sm text-destructive bg-destructive/10 p-3 rounded-lg border border-destructive/20">
                            <AlertCircle className="h-4 w-4 shrink-0" />
                            <span className="leading-snug">{status.message}</span>
                        </div>
                    )}
                    <div>
                        <Button type="submit" className="w-full" disabled={isLoading}>
                            {isLoading ? 'Sending...' : 'Send Reset Link'}
                        </Button>
                    </div>
                </form>
            )}

            {status?.type !== 'success' && (
              <p className="mt-10 text-center text-sm text-muted-foreground">
                  Remembered your password?{' '}
                  <Link href="/login" className="underline font-semibold text-primary">
                  Login
                  </Link>
              </p>
            )}
        </div>
      </div>
       <div className="hidden lg:block relative">
        <Image
          src="https://images.unsplash.com/photo-1581093450021-4a7360e9a6b5?q=80&w=2940&ixlib=rb-4.1.0"
          alt="A construction worker looking at blueprints"
          width={1920}
          height={1080}
          className="h-full w-full object-cover"
          data-ai-hint="construction worker"
        />
      </div>
    </div>
  );
}
