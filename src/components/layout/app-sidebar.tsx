
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
  Coins,
  PhoneCall,
  Globe,
} from 'lucide-react';
import { useI18n } from '@/lib/i18n-context';
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
import { Badge } from '@/components/ui/badge';
import type { User as AppUser } from '@/lib/data';


const links = [
  { href: '/', key: 'dashboard', label: 'Dashboard', icon: LayoutGrid },
  { href: '/projects', key: 'projects', label: 'Projects', icon: FolderKanban },
  { href: '/inventory', key: 'inventory', label: 'Warehouse', icon: Package },
  { href: '/purchase-orders', key: 'purchaseOrders', label: 'Purchase Orders', icon: ShoppingCart },
  { href: '/worklog', key: 'dailyWorklog', label: 'Daily Worklog', icon: ClipboardPen },
  { href: '/work-prep', key: 'workPrep', label: 'Work Prep', icon: PhoneCall },
  { href: '/team-hub', key: 'teamHub', label: 'Team Hub', icon: MessageSquare },
  { href: '/analytics', key: 'analytics', label: 'Analytics', icon: AreaChart },
];

const aiLinks = [
  { href: '/material-estimation', key: 'aiEstimation', label: 'AI Estimation', icon: Wand2 },
];

const personalLinks = [
  { href: '/notifications', key: 'notifications', label: 'Notifications', icon: Bell },
  { href: '/personal-pouch', key: 'personalPouch', label: 'Personal Pouch', icon: Wallet },
  { href: '/project-pouch', key: 'projectPouch', label: 'Project Pouch', icon: Briefcase },
]

const financialLinks = [
  { href: '/financials/salary-profiles', key: 'salaryProfiles', label: 'Salary Profiles', icon: Wallet },
  { href: '/financials/payday', key: 'weeklyPayday', label: 'Weekly Pay-Day', icon: Coins },
  { href: '/financials/contractors', key: 'contractorAccounts', label: 'Contractor Accounts', icon: Building2 },
  { href: '/expenses', key: 'projectExpenses', label: 'Project Expenses', icon: Briefcase },
  { href: '/materials/reconciliation', key: 'materialReconciliation', label: 'Material Reconciliation', icon: Package },
  { href: '/projects/milestones', key: 'clientMilestones', label: 'Client Milestones', icon: Coins },
]

const adminLinks = [
  { href: '/employees', key: 'employees', label: 'Employees', icon: Users },
];




export function AppSidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { supabase, user } = useSupabase();
  const { language, setLanguage, t } = useI18n();
  const [isClient, setIsClient] = useState(false);
  const { setOpenMobile } = useSidebar();


  useEffect(() => {
    setIsClient(true);
  }, []);

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

      const fetchUnreadNotifications = async () => {
        const { count, error } = await supabase
          .from('notifications')
          .select('*', { count: 'exact', head: true })
          .eq('user_id', user.id)
          .eq('read', false);

        if (!error && count) {
          setUnreadCount(count);
        }
      };

      fetchUserProfile();
      fetchUnreadNotifications();
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
    <Sidebar className="glass">
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
                  tooltip={t(link.key, link.label)}
                  className="justify-start font-medium"
                >
                  <link.icon className="h-5 w-5" />
                  <span className="group-data-[state=collapsed]:hidden">{t(link.key, link.label)}</span>
                </SidebarMenuButton>
              </Link>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
        <SidebarSeparator className="my-4" />
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">{t('aiTools', 'AI Tools')}</p>
        <SidebarMenu>
          {aiLinks.map((link) => (
            <SidebarMenuItem key={link.href}>
              <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                <SidebarMenuButton
                  isActive={isActive(link.href)}
                  tooltip={t(link.key, link.label)}
                  className="justify-start font-medium"
                >
                  <link.icon className="h-5 w-5" />
                  <span className="group-data-[state=collapsed]:hidden">{t(link.key, link.label)}</span>
                </SidebarMenuButton>
              </Link>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
        <SidebarSeparator className="my-4" />
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">{t('pouchSection', 'Pouch')}</p>
        <SidebarMenu>
          {personalLinks.map((link) => (
            <SidebarMenuItem key={link.href}>
              <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                <SidebarMenuButton
                  isActive={isActive(link.href)}
                  tooltip={t(link.key, link.label)}
                  className="justify-start font-medium"
                >
                  <link.icon className="h-5 w-5" />
                  <span className="group-data-[state=collapsed]:hidden flex-1 flex items-center justify-between">
                    <span>{t(link.key, link.label)}</span>
                    {link.key === 'notifications' && unreadCount > 0 && (
                      <Badge variant="destructive" className="text-[10px] font-bold px-1.5 py-0 h-4 rounded-full">
                        {unreadCount}
                      </Badge>
                    )}
                  </span>
                </SidebarMenuButton>
              </Link>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
        {(userProfile?.role === 'admin' || userProfile?.role === 'manager') && (
          <>
            <SidebarSeparator className="my-4" />
            <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">{t('financialsSection', 'Financials')}</p>
            <SidebarMenu>
              {financialLinks.map((link) => (
                <SidebarMenuItem key={link.href}>
                  <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                    <SidebarMenuButton
                      isActive={isActive(link.href)}
                      tooltip={t(link.key, link.label)}
                      className="justify-start font-medium"
                    >
                      <link.icon className="h-5 w-5" />
                      <span className="group-data-[state=collapsed]:hidden">{t(link.key, link.label)}</span>
                    </SidebarMenuButton>
                  </Link>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </>
        )}
        {userProfile?.role === 'admin' && (
          <>
            <SidebarSeparator className="my-4" />
            <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">{t('adminSection', 'Admin')}</p>
            <SidebarMenu>
              {adminLinks.map((link) => (
                <SidebarMenuItem key={link.href}>
                  <Link href={link.href} legacyBehavior={false} onClick={handleLinkClick}>
                    <SidebarMenuButton
                      isActive={isActive(link.href)}
                      tooltip={t(link.key, link.label)}
                      className="justify-start font-medium"
                    >
                      <link.icon className="h-5 w-5" />
                      <span className="group-data-[state=collapsed]:hidden">{t(link.key, link.label)}</span>
                    </SidebarMenuButton>
                  </Link>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </>
        )}

      </SidebarContent>
      <SidebarFooter className="p-2 space-y-2">
        {/* Language Switcher Pill */}
        <div className="px-1 group-data-[state=collapsed]:px-0">
          <div className="flex items-center justify-between p-1 bg-muted/30 border border-muted/20 rounded-xl group-data-[state=collapsed]:flex-col group-data-[state=collapsed]:gap-1">
            <button
              onClick={() => setLanguage('en')}
              className={cn(
                "flex-1 text-[11px] font-semibold py-1 px-2 rounded-lg transition-all text-center",
                language === 'en'
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              EN
            </button>
            <button
              onClick={() => setLanguage('ta')}
              className={cn(
                "flex-1 text-[11px] font-semibold py-1 px-2 rounded-lg transition-all text-center",
                language === 'ta'
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              தமிழ்
            </button>
          </div>
        </div>

        <SidebarSeparator className="my-1" />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <div className="flex items-center gap-3 p-2 rounded-xl hover:bg-white/20 dark:hover:bg-white/5 transition-all duration-300 cursor-pointer group-data-[state=collapsed]:p-0 group-data-[state=collapsed]:justify-center">
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
      </SidebarFooter>
    </Sidebar>
  );
}
