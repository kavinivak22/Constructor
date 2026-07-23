'use client';

import { SidebarProvider } from '@/components/ui/sidebar';
import { AppSidebar } from '@/components/layout/app-sidebar';
import { Toaster } from '@/components/ui/toaster';
import { useSupabase } from '@/supabase/provider';
import { MobileBottomNav } from '@/components/layout/mobile-bottom-nav';
import { AppHeader } from '@/components/layout/app-header';
import { InviteCheckWrapper } from '@/components/auth/invite-check-wrapper';
import { I18nProvider } from '@/lib/i18n-context';
import { VoiceBar } from '@/components/voice-assistant/voice-bar';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useIsMobile } from '@/hooks/use-mobile';
import type { User as AppUser } from '@/lib/data';

export function MainLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { supabase, user, isLoading: isUserLoading } = useSupabase();
  const isMobile = useIsMobile();
  const [isClient, setIsClient] = useState(false);
  const [userProfile, setUserProfile] = useState<AppUser | null>(null);
  const [isUserProfileLoading, setIsUserProfileLoading] = useState(true);

  useEffect(() => {
    setIsClient(true);
  }, []);

  const authRoutes = ['/login', '/signup', '/forgot-password', '/update-password', '/auth/callback'];
  const isAuthRoute = authRoutes.includes(pathname);
  const isRegisterCompanyRoute = pathname === '/register-company';

  useEffect(() => {
    if (isUserLoading) return;

    // Don't run profile fetching/routing logic on auth routes
    if (user && !isAuthRoute) {
      setIsUserProfileLoading(true);
      const fetchUserProfile = async () => {
        const { data, error } = await supabase
          .from('users')
          .select('*')
          .eq('id', user.id)
          .single();

        // User record might not exist yet (created only when joining/creating company)
        if (error && error.code === 'PGRST116') {
          // No user record found - this is expected for new signups
          setUserProfile(null);
        } else if (error) {
          console.error('Error fetching user profile:', error.message);
          setUserProfile(null);
        } else {
          setUserProfile({
            ...data,
            companyId: data.company_id,
            displayName: data.display_name,
            photoURL: data.photo_url,
            projectIds: [],
          } as AppUser);
        }

        setIsUserProfileLoading(false);
      };

      fetchUserProfile();
    } else if (!user) {
      setUserProfile(null);
      setIsUserProfileLoading(false);
    }
  }, [user, isUserLoading, isAuthRoute, supabase]);


  useEffect(() => {
    const isLoading = isUserLoading || isUserProfileLoading;
    if (isLoading) return;

    // After loading, if the user is on an auth route, we don't need to do any routing.
    // If they are not on an auth route, we enforce routing rules.
    if (!isAuthRoute) {
      if (user) {
        // User is authenticated
        // NOTE: Removed automatic redirect to /register-company
        // The InviteCheckWrapper component will handle this
        if (userProfile?.companyId && isRegisterCompanyRoute) {
          // If user has a company, they should not be on the register page.
          router.push('/');
        }
      } else {
        // User is not authenticated, send them to login.
        router.push('/login');
      }
    }

  }, [user, userProfile, isUserLoading, isUserProfileLoading, pathname, router, isRegisterCompanyRoute, isAuthRoute]);

  const isLoadingScreen = isUserLoading || (user && !isAuthRoute && isUserProfileLoading);
  const showFullLayout = user && userProfile?.companyId && !isAuthRoute;

  if (isLoadingScreen || !isClient) {
    return (
      <div className="bg-background h-screen flex items-center justify-center" />
    );
  }

  if (showFullLayout) {
    return (
      <I18nProvider>
        <SidebarProvider>
          {isMobile ? (
            <div className="relative flex min-h-screen flex-col bg-secondary overflow-hidden">
              {/* Dynamic Background Fluid Blobs */}
              <div className="absolute inset-0 pointer-events-none overflow-hidden z-0">
                <div className="absolute top-[-10%] left-[-10%] w-[70vw] h-[70vw] rounded-full bg-gradient-to-tr from-indigo-500/20 to-purple-500/25 dark:from-indigo-500/15 dark:to-purple-500/20 blur-[80px] animate-blob-1" />
                <div className="absolute bottom-[-10%] right-[-10%] w-[60vw] h-[60vw] rounded-full bg-gradient-to-br from-amber-500/15 to-orange-500/20 dark:from-amber-600/10 dark:to-orange-500/15 blur-[80px] animate-blob-2" />
                <div className="absolute top-[40%] right-[15%] w-[50vw] h-[50vw] rounded-full bg-gradient-to-tl from-cyan-500/15 to-emerald-500/15 dark:from-cyan-500/10 dark:to-emerald-500/10 blur-[80px] animate-blob-3" />
              </div>
              <div className="relative z-10 flex flex-col flex-1">
                <AppHeader />
                <main className="flex-1 pb-20">{children}</main>
                <MobileBottomNav />
              </div>
              <Toaster />
            </div>
          ) : (
            <div className="relative flex min-h-screen bg-secondary overflow-hidden w-full">
              {/* Dynamic Background Fluid Blobs */}
              <div className="absolute inset-0 pointer-events-none overflow-hidden z-0">
                <div className="absolute top-[-10%] left-[-10%] w-[50vw] h-[50vw] rounded-full bg-gradient-to-tr from-indigo-500/20 to-purple-500/25 dark:from-indigo-500/15 dark:to-purple-500/20 blur-[120px] animate-blob-1" />
                <div className="absolute bottom-[-10%] right-[-10%] w-[45vw] h-[45vw] rounded-full bg-gradient-to-br from-amber-500/15 to-orange-500/20 dark:from-amber-600/10 dark:to-orange-500/15 blur-[120px] animate-blob-2" />
                <div className="absolute top-[40%] right-[15%] w-[35vw] h-[35vw] rounded-full bg-gradient-to-tl from-cyan-500/15 to-emerald-500/15 dark:from-cyan-500/10 dark:to-emerald-500/10 blur-[120px] animate-blob-3" />
              </div>
              <div className="relative z-10 flex w-full flex-1">
                <AppSidebar />
                <div className="flex-1 flex flex-col min-w-0">
                  <AppHeader />
                  <main className="flex-1">{children}</main>
                </div>
              </div>
              <Toaster />
              <VoiceBar />
            </div>
          )}
        </SidebarProvider>
      </I18nProvider>
    );
  }

  // Show InviteCheckWrapper if user has no company and not on auth/register routes
  const shouldShowInviteCheck = user && !userProfile?.companyId && !isAuthRoute && !isRegisterCompanyRoute;

  return (
    <I18nProvider>
      <div className="bg-background">
        {shouldShowInviteCheck && <InviteCheckWrapper />}
        {children}
        <Toaster />
      </div>
    </I18nProvider>
  );
}
