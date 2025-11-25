
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
  Beaker,
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


const links = [
  { href: '/', label: 'Dashboard', icon: LayoutGrid },
  { href: '/projects', label: 'Projects', icon: FolderKanban },
  { href: '/inventory', label: 'Inventory', icon: Package },
  { href: '/purchase-orders', label: 'Purchase Orders', icon: ShoppingCart },
  { href: '/worklog', label: 'Daily Worklog', icon: ClipboardPen },
  { href: '/team-hub', label: 'Team Hub', icon: MessageSquare },
  { href: '/analytics', label: 'Analytics', icon: AreaChart },
];

const aiLinks = [
  { href: '/material-estimation', label: 'AI Estimation', icon: Wand2 },
];

const personalLinks = [
  { href: '/notifications', label: 'Notifications', icon: Bell },
  { href: '/personal-pouch', label: 'Personal Pouch', icon: Wallet },
  { href: '/project-pouch', label: 'Project Pouch', icon: Briefcase },
]

const adminLinks = [
  { href: '/employees', label: 'Employees', icon: Users },
];

const devLinks = [
  { href: '/test-db', label: 'DB Test', icon: Beaker },
];


export function AppSidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { supabase, user } = useSupabase();
  const [isClient, setIsClient] = useState(false);
  const { setOpenMobile } = useSidebar();


  useEffect(() => {
    setIsClient(true);
  }, []);

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
    <Sidebar className="bg-background/60 backdrop-blur-md">
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
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">AI Tools</p>
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
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">Pouch</p>
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
        <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">Admin</p>
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
        {process.env.NODE_ENV === 'development' && (
          <>
            <SidebarSeparator className="my-4" />
            <p className="px-2 text-xs font-semibold uppercase text-muted-foreground tracking-wider group-data-[state=collapsed]:hidden">Dev</p>
            <SidebarMenu>
              {devLinks.map((link) => (
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
          </>
        )}
      </SidebarContent>
      <SidebarFooter className="p-2">
        <SidebarSeparator className="mb-2" />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <div className="flex items-center gap-3 p-2 rounded-md hover:bg-muted cursor-pointer group-data-[state=collapsed]:p-0 group-data-[state=collapsed]:justify-center">
              <Avatar className="h-9 w-9">
                <AvatarImage src={user?.user_metadata.avatar_url ?? undefined} alt="User Avatar" />
                <AvatarFallback>{user?.user_metadata.full_name?.charAt(0) || 'U'}</AvatarFallback>
              </Avatar>
              <div className="flex-1 group-data-[state=collapsed]:hidden">
                <p className="text-sm font-medium leading-none truncate">{user?.user_metadata.full_name}</p>
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
