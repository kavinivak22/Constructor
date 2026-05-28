'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, CheckCircle, AlertCircle } from 'lucide-react';
import { useState } from 'react';
import Image from 'next/image';
import { useSupabase } from '@/supabase/provider';

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
    try {
      const { error } = await supabase.auth.resetPasswordForEmail(email, {
        redirectTo: `${process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:9002'}/update-password`,
      });
      if (error) throw error;
      setStatus({ 
        type: 'success', 
        message: 'Check your inbox for a link to reset your password.' 
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
                Forgot your password?
                </h2>
                <p className="mt-2 text-muted-foreground">
                No problem. Enter your email and we'll send you a reset link.
                </p>
            </div>

            {status?.type === 'success' ? (
                 <div className="rounded-lg border-l-4 border-green-500 bg-green-50 p-4 text-green-700">
                    <div className="flex items-center gap-3">
                        <CheckCircle className="h-5 w-5" />
                        <div>
                            <h3 className="font-bold">Email Sent</h3>
                            <p className="text-sm">{status.message}</p>
                        </div>
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
                        <div className="flex items-center gap-2 text-sm text-destructive">
                            <AlertCircle className="h-4 w-4" />
                            <span>{status.message}</span>
                        </div>
                    )}
                    <div>
                        <Button type="submit" className="w-full" disabled={isLoading}>
                            {isLoading ? 'Sending...' : 'Send Reset Link'}
                        </Button>
                    </div>
                </form>
            )}

            <p className="mt-10 text-center text-sm text-muted-foreground">
                Remembered your password?{' '}
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
