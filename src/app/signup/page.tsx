
'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, AlertCircle, CheckCircle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import Image from 'next/image';
import { useSupabase } from '@/supabase/provider';

// This function now attempts to parse a JSON error message from the server
const getFriendlyErrorMessage = (error: any): string => {
  try {
    // Edge function errors might be in the response body as a JSON string
    const errorBody = JSON.parse(error.message);
    if (errorBody.error) {
      return errorBody.error;
    }
  } catch (e) {
    // Fallback for standard Supabase errors or non-JSON messages
    if (error?.message) {
      if (error.message.includes('already registered')) {
        return 'This email is already registered. Please sign in instead.';
      }
      if (error.message.includes('password should be at least 6 characters')) {
        return 'Password must be at least 6 characters long.';
      }
      if (error.message.includes('Failed to send a request to the Edge Function')) {
        return "A server-side error occurred during signup. Please try again later.";
      }
      return error.message;
    }
  }
  return 'An unexpected error occurred. Please try again.';
};

export default function SignupPage() {
  const router = useRouter();
  const { supabase } = useSupabase();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [emailSent, setEmailSent] = useState(false);

  const processGoogleSignIn = async () => {
    setIsGoogleLoading(true);
    setError(null);
    try {
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: `${process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:9002'}/auth/callback`,
        },
      });
      if (error) throw error;
    } catch (error: any) {
      setError(getFriendlyErrorMessage(error));
    } finally {
      setIsGoogleLoading(false);
    }
  };

  const handleEmailSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: {
          // This data is passed to the 'on_auth_user_created' trigger
          data: {
            full_name: displayName,
            // You can add other metadata here if needed
          }
        }
      });

      if (error) throw error;

      // If we reach here, Supabase will handle sending the confirmation email
      // (if enabled in your project settings). We just need to show a message.
      // For testing, since email confirmation is off, the user is logged in immediately.

      // After a successful sign-up, Supabase automatically authenticates the user.
      // We just need to trigger a page refresh to let the main layout handle routing.
      router.push('/');

    } catch (error: any) {
      setError(getFriendlyErrorMessage(error));
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
              Create your account
            </h2>
            <p className="mt-2 text-muted-foreground">
              Enter your details below to get started.
            </p>
          </div>

          {emailSent ? (
            <div className="rounded-lg border-l-4 border-green-500 bg-green-50 p-4 text-green-700">
              <div className="flex items-center gap-3">
                <CheckCircle className="h-5 w-5" />
                <div>
                  <h3 className="font-bold">Check your inbox</h3>
                  <p className="text-sm">A confirmation link has been sent to {email}. Click it to complete your registration.</p>
                </div>
              </div>
            </div>
          ) : (
            <form onSubmit={handleEmailSignup} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="displayName">Full Name</Label>
                <Input
                  id="displayName"
                  type="text"
                  placeholder="John Doe"
                  required
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="m@example.com"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              {error && (
                <div className="flex items-start gap-2 text-sm text-destructive bg-destructive/10 p-3 rounded-md">
                  <AlertCircle className="h-4 w-4 mt-0.5 flex-shrink-0" />
                  <span className="break-all">{error}</span>
                </div>
              )}
              <div>
                <Button type="submit" className="w-full" disabled={isLoading || isGoogleLoading}>
                  {isLoading ? 'Creating Account...' : 'Sign Up with Email'}
                </Button>
              </div>
            </form>
          )}

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
              onClick={processGoogleSignIn}
              disabled={isLoading || isGoogleLoading}
            >
              {isGoogleLoading ? (
                <div className="animate-spin h-4 w-4 mr-2 border-t-2 border-b-2 border-primary rounded-full" />
              ) : (
                <svg role="img" viewBox="0 0 24 24" className="mr-2 h-4 w-4">
                  <path
                    fill="currentColor"
                    d="M12.48 10.92v3.28h7.84c-.24 1.84-.85 3.18-1.73 4.1-1.02 1.02-2.37 1.62-4.38 1.62-3.82 0-6.94-3.1-6.94-6.93s3.12-6.94 6.94-6.94c2.2 0 3.59.87 4.48 1.72l2.42-2.42C17.64 3.02 15.34 2 12.48 2c-5.46 0-9.94 4.44-9.94 9.94s4.48 9.94 9.94 9.94c5.19 0 9.59-3.43 9.59-9.82 0-.72-.07-1.35-.19-1.95z"
                  />
                </svg>
              )}
              Google
            </Button>
          </div>

          <p className="mt-4 text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link href="/login" className="underline font-semibold text-primary">
              Login
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
