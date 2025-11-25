
'use client';
import { Bell, LogOut, User as UserIcon } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { useEffect, useState, useMemo } from 'react';
import type { Notification, User as AppUser } from '@/lib/data';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '../ui/dropdown-menu';
import { useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';

export function AppHeader() {
  const { supabase, user } = useSupabase();
  const router = useRouter();
  const [userProfile, setUserProfile] = useState<AppUser | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (user) {
      const fetchUserProfile = async () => {
        const { data, error } = await supabase
          .from('users')
          .select('*')
          .eq('id', user.id)
          .single();
        if (data) setUserProfile(data as AppUser);
      };

      fetchUserProfile();
    }
  }, [user, supabase]);

  useEffect(() => {
    if (userProfile && userProfile.projectIds && userProfile.projectIds.length > 0) {
      const fetchUnreadNotifications = async () => {
        const { count, error } = await supabase
          .from('notifications')
          .select('* ', { count: 'exact', head: true })
          .eq('userId', user!.id)
          .eq('read', false)
          .in('projectId', userProfile.projectIds);

        if (error) {
          console.error('Error fetching notifications count:', error);
          setUnreadCount(0);
        } else {
          setUnreadCount(count || 0);
        }
      };

      fetchUnreadNotifications();
    }
  }, [user, userProfile, supabase]);


  const handleLogout = async () => {
    await supabase.auth.signOut();
    router.push('/login');
  };

  return (
    <header className="sticky top-0 z-10 flex items-center justify-between h-16 px-4 bg-background/60 backdrop-blur-md border-b md:px-6">
      <div className="flex items-center gap-3">
        {/* The sidebar trigger is now only part of the mobile bottom nav */}
      </div>
      <div className="flex items-center gap-4 ml-auto">
        <Link href="/notifications">
          <Button variant="ghost" size="icon" className="relative">
            <Bell className="h-5 w-5" />
            <span className="sr-only">Notifications</span>
            {unreadCount > 0 && (
              <div className="absolute top-1.5 right-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-xs text-white">
                {unreadCount}
              </div>
            )}
          </Button>
        </Link>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Avatar className="h-9 w-9 cursor-pointer">
              <AvatarImage src={user?.user_metadata?.avatar_url ?? undefined} alt="User Avatar" />
              <AvatarFallback>{user?.user_metadata?.full_name?.charAt(0) || 'U'}</AvatarFallback>
            </Avatar>
          </DropdownMenuTrigger>
          <DropdownMenuContent className="w-56" align="end">
            <DropdownMenuLabel className="font-normal">
              <div className="flex flex-col space-y-1">
                <p className="text-sm font-medium leading-none">{user?.user_metadata?.full_name}</p>
                <p className="text-xs leading-none text-muted-foreground">
                  {user?.email}
                </p>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link href="/profile">
                <UserIcon className="mr-2 h-4 w-4" />
                <span>Profile</span>
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              <span>Log out</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

      </div>
    </header>
  );
}
