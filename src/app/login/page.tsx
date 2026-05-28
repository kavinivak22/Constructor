'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, AlertCircle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useToast } from '@/hooks/use-toast';
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
        }
      } else {
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
              <Button onClick={async () => { await supabase.auth.signOut(); setAccessDenied(false); }} variant="outline">Back to Login</Button>
              <Button onClick={handleResign} variant="destructive">Resign from Company</Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full min-h-screen lg:grid lg:grid-cols-2">
      {/* Left Column: Traditional login form */}
      <div className="flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-background">
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
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="glass border-white/10 dark:border-white/5 focus-visible:ring-primary"
                />
              </div>
              <div className="space-y-2">
                <div className="flex items-center">
                  <Label htmlFor="password">Password</Label>
                  <Link
                    href="/forgot-password"
                    className="ml-auto inline-block text-sm underline text-primary hover:text-primary/80"
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
                  className="glass border-white/10 dark:border-white/5 focus-visible:ring-primary"
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
              className="w-full glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5"
              onClick={handleGoogleSignIn}
              disabled={isLoading}>
              <svg role="img" viewBox="0 0 24 24" className="mr-2 h-4 w-4 fill-current">
                <path d="M12.48 10.92v3.28h7.84c-.24 1.84-.85 3.18-1.73 4.1-1.02 1.02-2.37 1.62-4.38 1.62-3.82 0-6.94-3.1-6.94-6.93s3.12-6.94 6.94-6.94c2.2 0 3.59.87 4.48 1.72l2.42-2.42C17.64 3.02 15.34 2 12.48 2c-5.46 0-9.94 4.44-9.94 9.94s4.48 9.94 9.94 9.94c5.19 0 9.59-3.43 9.59-9.82 0-.72-.07-1.35-.19-1.95z" />
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

      {/* Right Column: Immersive features preview dashboard (PC View only) */}
      <div className="hidden lg:flex flex-col items-center justify-center p-8 bg-[#0a0f1d] border-l border-slate-900 relative overflow-hidden">
        {/* Blueprint grid background */}
        <div 
          className="absolute inset-0 z-0 opacity-15" 
          style={{
            backgroundImage: `
              linear-gradient(rgba(249, 115, 22, 0.05) 1px, transparent 1px),
              linear-gradient(90deg, rgba(249, 115, 22, 0.05) 1px, transparent 1px)
            `,
            backgroundSize: '25px 25px',
          }}
        />
        
        {/* Glowing Blobs */}
        <div className="absolute top-1/4 left-1/4 w-72 h-72 rounded-full bg-orange-500/10 blur-[80px] animate-blob-1 pointer-events-none z-0" />
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 rounded-full bg-indigo-500/10 blur-[100px] animate-blob-2 pointer-events-none z-0" />

        <div className="relative z-10 w-full max-w-lg space-y-6">
          <div className="space-y-2 text-center lg:text-left mb-6">
            <span className="text-[10px] font-bold uppercase tracking-widest text-orange-400 bg-orange-500/10 border border-orange-500/20 px-3 py-1 rounded-full">Workspace Preview</span>
            <h2 className="text-3xl font-extrabold tracking-tight font-headline text-white mt-3 leading-tight">
              Manage construction, <br />
              <span className="bg-gradient-to-r from-orange-400 via-amber-400 to-yellow-300 bg-clip-text text-transparent">simpler and faster.</span>
            </h2>
            <p className="text-slate-400 text-sm max-w-md">
              Track project milestones, materials inventory, purchase orders, and daily logs in one unified system.
            </p>
          </div>

          {/* Mock Widget 1: Project Progress Card */}
          <div className="backdrop-blur-md bg-slate-950/40 border border-slate-800/80 p-5 rounded-2xl shadow-xl space-y-4 hover:border-slate-700/60 transition-colors">
            <div className="flex justify-between items-start">
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-500">Active Project</span>
                <h3 className="font-bold text-white text-sm mt-0.5">Downtown Plaza Phase 2</h3>
                <p className="text-[11px] text-slate-400">Client: Urban Dev Group</p>
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 bg-green-500/10 border border-green-500/20 text-green-400 rounded-md uppercase">Active</span>
            </div>
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-semibold">
                <span className="text-slate-400">Overall Progress</span>
                <span className="text-orange-400">74%</span>
              </div>
              <div className="h-1.5 w-full bg-slate-950/60 rounded-full overflow-hidden border border-slate-800/80">
                <div className="h-full bg-gradient-to-r from-orange-500 to-amber-500" style={{ width: '74%' }} />
              </div>
            </div>
            <div className="grid grid-cols-3 gap-2 pt-1 text-center">
              <div className="bg-slate-950/30 p-2 rounded-xl border border-slate-900/60">
                <span className="text-[9px] uppercase font-bold text-slate-500 block">Budget</span>
                <span className="text-xs font-bold text-white">₹8.5M</span>
              </div>
              <div className="bg-slate-950/30 p-2 rounded-xl border border-slate-900/60">
                <span className="text-[9px] uppercase font-bold text-slate-500 block">Spent</span>
                <span className="text-xs font-bold text-white">₹6.1M</span>
              </div>
              <div className="bg-slate-950/30 p-2 rounded-xl border border-slate-900/60">
                <span className="text-[9px] uppercase font-bold text-slate-500 block">Team Size</span>
                <span className="text-xs font-bold text-white">12 Members</span>
              </div>
            </div>
          </div>

          {/* Mock Widget 2: Recent Activity Timeline */}
          <div className="backdrop-blur-md bg-slate-950/40 border border-slate-800/80 p-5 rounded-2xl shadow-xl space-y-3 hover:border-slate-700/60 transition-colors">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-indigo-500" /> Recent Updates
            </h4>
            <div className="space-y-3 pl-1.5 border-l border-slate-800">
              <div className="relative pl-4">
                <div className="absolute left-[-5px] top-[5px] h-2.5 w-2.5 rounded-full bg-orange-500 ring-4 ring-[#0a0f1d]" />
                <p className="text-xs text-white font-semibold">Steel Framing & Column Reinforcement</p>
                <p className="text-[10px] text-slate-400">Daily Worklog by Kavin B. • 2 hrs ago</p>
              </div>
              <div className="relative pl-4">
                <div className="absolute left-[-5px] top-[5px] h-2.5 w-2.5 rounded-full bg-indigo-400 ring-4 ring-[#0a0f1d]" />
                <p className="text-xs text-white font-semibold">Cement delivery received & inventoried</p>
                <p className="text-[10px] text-slate-400">PO #8972 Approved by Admin • 4 hrs ago</p>
              </div>
            </div>
          </div>

          {/* Mock Widget 3: Low Stock Warning Alert */}
          <div className="backdrop-blur-md bg-red-950/20 border border-red-900/30 p-4 rounded-xl flex items-start gap-3 hover:border-red-800/40 transition-colors">
            <div className="bg-red-500/10 p-1.5 rounded-lg border border-red-500/20 text-red-400 shrink-0">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-alert-triangle"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            </div>
            <div>
              <h5 className="font-bold text-red-400 text-xs">Inventory Warning</h5>
              <p className="text-[10px] text-red-300/85 mt-0.5 leading-relaxed">
                Cement (OPC-53 Grade) stock level is below critical threshold. 15 bags left. Reorder recommended.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
