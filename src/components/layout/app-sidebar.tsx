
'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  Briefcase,
  FolderKanban,
  LayoutGrid,
  MessageSquare,
  Package,
  Settings,
  ShoppingCart,
  Users,
  Wallet,
  Wand2,
  ClipboardPen,
  Building2,
  LogOut,
  AreaChart,
  Bell,
  User as UserIcon,

} from 'lucide-react';
import {
  Sidebar,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarContent,
  SidebarFooter,
  SidebarSeparator,
  useSidebar,
} from '@/components/ui/sidebar';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { cn } from '@/lib/utils';
import { useSupabase } from '@/supabase/provider';
import { Button } from '../ui/button';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '../ui/dropdown-menu';
import { useTranslation } from '@/lib/i18n/language-context';
import type { User as AppUser } from '@/lib/data';



// Removed static link definitions in favor of dynamic ones inside component





export function AppSidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { supabase, user } = useSupabase();
  const [isClient, setIsClient] = useState(false);
  const { setOpenMobile } = useSidebar();
  const { t } = useTranslation();


  // Memoize links to update when language changes
  const links = [
    { href: '/', label: t('common.dashboard'), icon: LayoutGrid },
    { href: '/projects', label: t('common.projects'), icon: FolderKanban },
    { href: '/inventory', label: t('common.inventory'), icon: Package },
    { href: '/purchase-orders', label: t('common.purchase_orders'), icon: ShoppingCart },
    { href: '/worklog', label: t('common.daily_worklog'), icon: ClipboardPen },
    { href: '/team-hub', label: t('common.team_hub'), icon: MessageSquare },
    { href: '/analytics', label: t('common.analytics'), icon: AreaChart },
  ];

  const aiLinks = [
    { href: '/material-estimation', label: t('common.ai_estimation'), icon: Wand2 },
  ];

  const personalLinks = [
    { href: '/notifications', label: t('common.notifications'), icon: Bell },
    { href: '/personal-pouch', label: t('common.personal_pouch'), icon: Wallet },
    { href: '/project-pouch', label: t('common.project_pouch'), icon: Briefcase },
  ]

  const adminLinks = [
    { href: '/employees', label: t('common.employees'), icon: Users },
  ];


  useEffect(() => {
    setIsClient(true);
  }, []);

  const [userProfile, setUserProfile] = useState<AppUser | null>(null);

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

  const isActive = (href: string) => {
    // Handle exact match for the dashboard and startsWith for other routes
    if (href === '/') {
      return pathname === href;
    }
    return pathname.startsWith(href);
  };

  const handleLogout = async () => {
    await supabase.auth.signOut();
    router.push('/login');
  };

  const handleLinkClick = () => {
    setOpenMobile(false);
  };

  return (
    <Sidebar className="glass border-r">
      <SidebarHeader>
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Building2 className="h-5 w-5" />
          </div>
          <span className="text-lg font-semibold font-headline text-foreground group-data-[state=collapsed]:hidden">Constructor</span>
        </div>
      </SidebarHeader>
      <SidebarContent className="p-2">
        <SidebarMenu>
          {links.map((link) => (
            <SidebarMenuItem key={link.href}>
              <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                <SidebarMenuButton
                  isActive={isActive(link.href)}
                  tooltip={link.label}
                  className="justify-start font-medium"
                >
                  <link.icon className="h-5 w-5" />
                  <span className="group-data-[state=collapsed]:hidden">{link.label}</span>
                </SidebarMenuButton>
              </Link>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
        <SidebarSeparator className="my-4" />
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">{t('common.ai_tools')}</p>
        <SidebarMenu>
          {aiLinks.map((link) => (
            <SidebarMenuItem key={link.href}>
              <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                <SidebarMenuButton
                  isActive={isActive(link.href)}
                  tooltip={link.label}
                  className="justify-start font-medium"
                >
                  <link.icon className="h-5 w-5" />
                  <span className="group-data-[state=collapsed]:hidden">{link.label}</span>
                </SidebarMenuButton>
              </Link>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
        <SidebarSeparator className="my-4" />
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">{t('common.pouch')}</p>
        <SidebarMenu>
          {personalLinks.map((link) => (
            <SidebarMenuItem key={link.href}>
              <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                <SidebarMenuButton
                  isActive={isActive(link.href)}
                  tooltip={link.label}
                  className="justify-start font-medium"
                >
                  <link.icon className="h-5 w-5" />
                  <span className="group-data-[state=collapsed]:hidden">{link.label}</span>
                </SidebarMenuButton>
              </Link>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
        <SidebarSeparator className="my-4" />
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">{t('common.admin')}</p>
        <SidebarMenu>
          {adminLinks.map((link) => (
            <SidebarMenuItem key={link.href}>
              <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                <SidebarMenuButton
                  isActive={isActive(link.href)}
                  tooltip={link.label}
                  className="justify-start font-medium"
                >
                  <link.icon className="h-5 w-5" />
                  <span className="group-data-[state=collapsed]:hidden">{link.label}</span>
                </SidebarMenuButton>
              </Link>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>

      </SidebarContent>
      <SidebarFooter className="p-2">
        <SidebarSeparator className="mb-2" />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <div className="flex items-center gap-3 p-2 rounded-md hover:bg-muted cursor-pointer group-data-[state=collapsed]:p-0 group-data-[state=collapsed]:justify-center">
              <Avatar className="h-9 w-9">
                <AvatarImage src={userProfile?.photoURL || user?.user_metadata.avatar_url || undefined} alt="User Avatar" />
                <AvatarFallback>{userProfile?.displayName?.charAt(0) || user?.user_metadata.full_name?.charAt(0) || 'U'}</AvatarFallback>
              </Avatar>
              <div className="flex-1 group-data-[state=collapsed]:hidden">
                <p className="text-sm font-medium leading-none truncate">{userProfile?.displayName || user?.user_metadata.full_name}</p>
                <p className="text-xs leading-none text-muted-foreground truncate">
                  {user?.email}
                </p>
              </div>
            </div>
          </DropdownMenuTrigger>
          <DropdownMenuContent className="w-56 mb-2" align="end">
            <DropdownMenuItem asChild>
              <Link href="/profile">
                <UserIcon className="mr-2 h-4 w-4" />
                <span>{t('common.profile')}</span>
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <Link href="/settings">
                <Settings className="mr-2 h-4 w-4" />
                <span>{t('common.settings')}</span>
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              <span>{t('common.logout')}</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
