
'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, AlertCircle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useToast } from '@/hooks/use-toast';
import Image from 'next/image';
import type { User } from '@supabase/supabase-js';
import { useSupabase } from '@/supabase/provider';

const getFriendlyErrorMessage = (error: any): string => {
  if (error?.message) {
    if (error.message.includes('Invalid login credentials')) {
      return 'Invalid email or password. Please check your credentials and try again.';
    }
  }
  return 'An unexpected error occurred during login. Please try again.';
};

export default function LoginPage() {
  const router = useRouter();
  const { toast } = useToast();
  const { supabase } = useSupabase();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [accessDenied, setAccessDenied] = useState(false);

  const checkUserStatus = async (userId: string): Promise<boolean> => {
    const { data: user, error } = await supabase
      .from('users')
      .select('status')
      .eq('id', userId)
      .single();

    if (user && user.status === 'inactive') {
      await supabase.auth.signOut();
      setAccessDenied(true);
      return false;
    }
    return true;
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    setAccessDenied(false);
    try {
      const { data, error } = await supabase.auth.signInWithPassword({ email, password });
      if (error) throw error;

      if (data.user) {
        const canLogin = await checkUserStatus(data.user.id);
        if (canLogin) {
          router.push('/');
          router.refresh();
        }
      } else {
        // This case should ideally not be hit if error is null, but as a safeguard:
        setError("Login failed. Please try again.");
      }
    } catch (error: any) {
      setError(getFriendlyErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setIsLoading(true);
    setError(null);
    setAccessDenied(false);
    try {
      await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: `${process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:9002'}/auth/callback`
        }
      });
    } catch (error: any) {
      setError(getFriendlyErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const handleResign = async () => {
    try {
      const { resignFromCompany } = await import('@/app/actions/employees');
      const result = await resignFromCompany();
      if (result.success) {
        setAccessDenied(false);
        window.location.reload();
      } else {
        toast({
          title: "Error",
          description: result.error || "Failed to resign from company",
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "An unexpected error occurred",
        variant: "destructive"
      });
    }
  };

  if (accessDenied) {
    return (
      <div className="w-full min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-background">
        <div className="mx-auto w-full max-w-md space-y-8 text-center">
          <div>
            <div className="flex justify-center mb-6 items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-destructive text-destructive-foreground">
                <Building2 className="h-7 w-7" />
              </div>
            </div>
            <h2 className="mt-6 text-3xl font-bold tracking-tight text-destructive font-headline">
              Access Denied
            </h2>
            <p className="mt-4 text-muted-foreground">
              Your account has been marked as inactive. Please contact your company administrator for assistance.
            </p>
            <div className="flex flex-col gap-3 mt-6">
              <Button onClick={() => setAccessDenied(false)} variant="outline">Back to Login</Button>
              <Button onClick={handleResign} variant="destructive">Resign from Company</Button>
            </div>
          </div>
        </div>
      </div>
    )
  }

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
              Welcome back
            </h2>
            <p className="mt-2 text-muted-foreground">
              Enter your credentials to access your account.
            </p>
          </div>
          <form onSubmit={handleLogin} className="space-y-6">
            <div className="space-y-4">
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
              <div className="space-y-2">
                <div className="flex items-center">
                  <Label htmlFor="password">Password</Label>
                  <Link
                    href="/forgot-password"
                    className="ml-auto inline-block text-sm underline"
                  >
                    Forgot your password?
                  </Link>
                </div>
                <Input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 text-sm text-destructive">
                <AlertCircle className="h-4 w-4" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading && !accessDenied ? 'Signing In...' : 'Login'}
              </Button>
            </div>
          </form>
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-background px-2 text-muted-foreground">
                Or continue with
              </span>
            </div>
          </div>
          <div>
            <Button
              variant="outline"
              className="w-full"
              onClick={handleGoogleSignIn}
              disabled={isLoading}>
              <svg role="img" viewBox="0 0 24 24" className="mr-2 h-4 w-4">
                <path
                  fill="currentColor"
                  d="M12.48 10.92v3.28h7.84c-.24 1.84-.85 3.18-1.73 4.1-1.02 1.02-2.37 1.62-4.38 1.62-3.82 0-6.94-3.1-6.94-6.93s3.12-6.94 6.94-6.94c2.2 0 3.59.87 4.48 1.72l2.42-2.42C17.64 3.02 15.34 2 12.48 2c-5.46 0-9.94 4.44-9.94 9.94s4.48 9.94 9.94 9.94c5.19 0 9.59-3.43 9.59-9.82 0-.72-.07-1.35-.19-1.95z"
                />
              </svg>
              Google
            </Button>
          </div>
          <p className="mt-10 text-center text-sm text-muted-foreground">
            Don't have an account?{' '}
            <Link href="/signup" className="underline font-semibold text-primary">
              Sign up
            </Link>
          </p>
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
