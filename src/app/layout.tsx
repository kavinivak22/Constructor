
'use client';

import './globals.css';
import { SidebarProvider } from '@/components/ui/sidebar';
import { AppSidebar } from '@/components/layout/app-sidebar';
import { Toaster } from '@/components/ui/toaster';
import { SupabaseProvider, useSupabase } from '@/supabase/provider';
import { ReactQueryProvider } from '@/lib/react-query';
import { cn } from '@/lib/utils';
import { MobileBottomNav } from '@/components/layout/mobile-bottom-nav';
import { AppHeader } from '@/components/layout/app-header';
import { InviteCheckWrapper } from '@/components/auth/invite-check-wrapper';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useIsMobile } from '@/hooks/use-mobile';
import type { User as AppUser } from '@/lib/data';

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Sora:wght@400;600;700&display=swap"
          rel="stylesheet"
        />
        <link rel="icon" href="https://api.iconify.design/lucide:building-2.svg?color=%23f97316" />
      </head>
      <body
        className={cn(
          'font-body antialiased',
          'bg-secondary'
        )}
        suppressHydrationWarning
      >
        <ReactQueryProvider>
          <SupabaseProvider>
            <SidebarProvider>
              <MainLayout>{children}</MainLayout>
            </SidebarProvider>
          </SupabaseProvider>
        </ReactQueryProvider>
      </body>
    </html>
  );
}


function MainLayout({ children }: { children: React.ReactNode }) {
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
          setUserProfile(data as AppUser);
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
    if (isMobile) {
      return (
        <div className="relative flex min-h-screen flex-col bg-secondary">
          <AppHeader />
          <main className="flex-1 pb-20">{children}</main>
          <MobileBottomNav />
          <Toaster />
        </div>
      );
    }

    return (
      <div className="relative flex min-h-screen bg-secondary">
        <AppSidebar />
        <div className="flex-1 flex flex-col min-w-0">
          <AppHeader />
          <main className="flex-1">{children}</main>
        </div>
        <Toaster />
      </div>
    );
  }

  // Show InviteCheckWrapper if user has no company and not on auth/register routes
  const shouldShowInviteCheck = user && !userProfile?.companyId && !isAuthRoute && !isRegisterCompanyRoute;

  return (
    <div className="bg-background">
      {shouldShowInviteCheck && <InviteCheckWrapper />}
      {children}
      <Toaster />
    </div>
  );
}
