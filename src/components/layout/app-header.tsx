
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
import { Breadcrumbs } from './breadcrumbs';
import { CommandMenu } from './command-menu';
import { useParams } from 'next/navigation';
import { useProject } from '@/hooks/queries';
import Image from 'next/image';

export function AppHeader() {
  const { supabase, user } = useSupabase();
  const router = useRouter();
  const params = useParams();
  const projectId = typeof params?.projectId === 'string' ? params.projectId : undefined;
  const { data: project } = useProject(projectId);
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
        if (data) {
          setUserProfile({
            ...data,
            companyId: data.company_id,
            displayName: data.display_name,
            photoURL: data.photo_url,
          } as AppUser);
        }
      };

      fetchUserProfile();
    }
  }, [user, supabase]);

  useEffect(() => {
    if (user) {
      const fetchUnreadNotifications = async () => {
        const { count, error } = await supabase
          .from('notifications')
          .select('*', { count: 'exact', head: true })
          .eq('user_id', user.id)
          .eq('read', false);

        if (error) {
          console.error('Error fetching notifications count:', error);
          setUnreadCount(0);
        } else {
          setUnreadCount(count || 0);
        }
      };

      fetchUnreadNotifications();
    }
  }, [user, supabase]);


  const handleLogout = async () => {
    await supabase.auth.signOut();
    router.push('/login');
  };

  return (
    <header className="sticky top-0 z-50 flex items-center justify-between h-16 px-4 glass md:px-6">
      <div className="flex items-center gap-4 flex-1">
        {project?.thumbnail_url && (
          <div className="relative w-8 h-8 rounded-full overflow-hidden shrink-0 border border-border/50">
            <Image src={project.thumbnail_url} alt={project.client_name || project.clientName || 'Project Logo'} fill className="object-cover" />
          </div>
        )}
        {/* The sidebar trigger is now only part of the mobile bottom nav */}
        <Breadcrumbs />
      </div>

      <div className="flex items-center gap-4">
        <div className="hidden md:flex">
          <CommandMenu />
        </div>

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
              <AvatarImage src={userProfile?.photoURL || user?.user_metadata?.avatar_url || undefined} alt="User Avatar" />
              <AvatarFallback>{userProfile?.displayName?.charAt(0) || user?.user_metadata?.full_name?.charAt(0) || 'U'}</AvatarFallback>
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

