'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { createClient } from '@/utils/supabase/client';
import type { EmailOtpType } from '@supabase/supabase-js';
import { AlertCircle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import Link from 'next/link';

export default function AuthCallbackPage() {
  const router = useRouter();
  const [status, setStatus] = useState<'loading' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    async function handleAuth() {
      try {
        const supabase = createClient();

        // 1. Inspect search params and hash fragment for errors or tokens
        const searchParams = new URLSearchParams(window.location.search);
        const hashParams = new URLSearchParams(
          typeof window !== 'undefined' ? window.location.hash.replace(/^#/, '') : ''
        );

        // Check for error responses returned by Supabase Auth
        const errorDesc = searchParams.get('error_description') || hashParams.get('error_description');
        const errorCode = searchParams.get('error_code') || hashParams.get('error_code') || searchParams.get('error');

        if (errorDesc || errorCode) {
          if (errorCode === 'otp_expired' || errorDesc?.toLowerCase().includes('expired')) {
            throw new Error('This email verification link has expired or has already been used. Please request a new one.');
          }
          throw new Error(errorDesc || 'Authentication failed. Please try again.');
        }

        // 2. Handle token_hash verification (PKCE / Token Hash flow)
        const token_hash = searchParams.get('token_hash');
        const type = (searchParams.get('type') || hashParams.get('type')) as EmailOtpType | null;

        if (token_hash && type) {
          const { error } = await supabase.auth.verifyOtp({ token_hash, type });
          if (error) throw error;

          if (type === 'recovery') {
            router.push('/update-password');
            return;
          }
          const next = searchParams.get('next') || '/';
          router.push(next);
          return;
        }

        // 3. Handle OAuth / PKCE authorization code
        const code = searchParams.get('code');
        if (code) {
          const { error } = await supabase.auth.exchangeCodeForSession(code);
          if (error) throw error;

          const next = searchParams.get('next') || '/';
          router.push(next);
          return;
        }

        // 4. Handle recovery type in hash fragment (e.g. #access_token=...&type=recovery)
        if (hashParams.get('type') === 'recovery') {
          router.push('/update-password');
          return;
        }

        // 5. If access token present in hash, let Supabase client session hydrate and redirect
        if (hashParams.get('access_token')) {
          router.push('/');
          return;
        }

        // Default fallback if no auth tokens found
        router.push('/');
      } catch (err: any) {
        console.error('Auth callback error:', err);
        setStatus('error');
        setErrorMessage(err.message || 'An unexpected error occurred during authentication.');
      }
    }

    handleAuth();
  }, [router]);

  if (status === 'error') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <div className="max-w-md w-full p-6 bg-card border rounded-2xl shadow-lg space-y-5 text-center">
          <div className="w-14 h-14 rounded-full bg-destructive/10 text-destructive flex items-center justify-center mx-auto">
            <AlertCircle className="w-8 h-8" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-foreground">Link Expired or Invalid</h2>
            <p className="text-sm text-muted-foreground mt-2 leading-relaxed">
              {errorMessage}
            </p>
          </div>
          <div className="pt-2 flex flex-col sm:flex-row gap-3">
            <Button asChild variant="outline" className="flex-1">
              <Link href="/login">Back to Sign In</Link>
            </Button>
            <Button asChild className="flex-1">
              <Link href="/forgot-password">Request New Link</Link>
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col items-center justify-center bg-slate-950 text-white gap-3">
      <Loader2 className="w-8 h-8 animate-spin text-primary" />
      <p className="text-sm font-medium text-slate-300">Completing authentication...</p>
    </div>
  );
}
