'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { createClient } from '@/utils/supabase/client';

export default function AuthCallbackPage() {
  const router = useRouter();

  useEffect(() => {
    async function handleAuth() {
      try {
        const supabase = createClient();
        const params = new URLSearchParams(window.location.search);
        const code = params.get('code');
        if (code) {
          await supabase.auth.exchangeCodeForSession(code);
        }
      } catch (err) {
        console.error('Auth error:', err);
      } finally {
        router.push('/');
      }
    }
    handleAuth();
  }, [router]);

  return (
    <div className="flex h-screen items-center justify-center bg-slate-950 text-white">
      <p className="animate-pulse text-sm font-semibold">Completing Authentication...</p>
    </div>
  );
}
