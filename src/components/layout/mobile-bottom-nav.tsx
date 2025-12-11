
'use client';

import { usePathname } from 'next/navigation';
import Link from 'next/link';
import {
  LayoutGrid,
  FolderKanban,
  ShoppingCart,
  MessageSquare,
  MoreHorizontal,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { AppSidebar } from './app-sidebar';
import { useSidebar } from '../ui/sidebar';
import { useTranslation } from '@/lib/i18n/language-context';

// Static definition removed, moved inside component


export function MobileBottomNav() {
  const pathname = usePathname();
  const { openMobile, setOpenMobile } = useSidebar();
  const { t } = useTranslation();

  const mainNavItems = [
    { href: '/', label: t('common.dashboard'), icon: LayoutGrid },
    { href: '/projects', label: t('common.projects'), icon: FolderKanban },
    { href: '/purchase-orders', label: t('common.purchase_orders'), icon: ShoppingCart },
    { href: '/team-hub', label: t('common.team_hub'), icon: MessageSquare },
  ];

  const isActive = (href: string) => {
    return pathname === href;
  };

  return (
    <div className="md:hidden fixed bottom-4 left-4 right-4 z-50">
      {/* Glassmorphic Container */}
      <div className="absolute inset-0 bg-background/80 backdrop-blur-xl border border-border/50 shadow-lg rounded-2xl" />

      <nav className="relative grid grid-cols-5 items-center h-16 px-2">
        {mainNavItems.map((item) => {
          const active = isActive(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'group flex flex-col items-center justify-center h-full w-full relative transition-all duration-300 ease-out',
                active ? 'text-primary' : 'text-muted-foreground hover:text-foreground'
              )}
            >
              {/* Active Indicator (Top Line) */}
              <span
                className={cn(
                  "absolute top-0 w-8 h-1 rounded-b-full bg-primary transition-all duration-300 ease-out",
                  active ? "opacity-100 translate-y-0" : "opacity-0 -translate-y-2"
                )}
              />

              {/* Icon Container */}
              <div className={cn(
                "relative p-1.5 rounded-xl transition-all duration-300",
                active ? "bg-primary/10 -translate-y-1" : "group-hover:bg-muted/50"
              )}>
                <item.icon
                  className={cn(
                    "h-5 w-5 transition-all duration-300",
                    active ? "stroke-[2.5px]" : "stroke-2"
                  )}
                />
              </div>

              {/* Label */}
              <span className={cn(
                "text-[10px] font-medium transition-all duration-300",
                active ? "opacity-100 translate-y-0" : "opacity-70 group-hover:opacity-100"
              )}>
                {item.label}
              </span>
            </Link>
          );
        })}

        <Sheet open={openMobile} onOpenChange={setOpenMobile}>
          <SheetTrigger asChild>
            <button className='group flex flex-col items-center justify-center h-full w-full relative text-muted-foreground hover:text-foreground transition-all duration-300'>
              <div className="relative p-1.5 rounded-xl group-hover:bg-muted/50 transition-all duration-300">
                <MoreHorizontal className="h-5 w-5 stroke-2" />
              </div>
              <span className="text-[10px] font-medium opacity-70 group-hover:opacity-100 transition-all duration-300">
                {t('common.more') || 'More'}
              </span>
            </button>
          </SheetTrigger>
          <SheetContent side="right" className="p-0 w-64 bg-transparent border-none" data-mobile="true">
            <SheetTitle className="sr-only">Main Menu</SheetTitle>
            <AppSidebar />
          </SheetContent>
        </Sheet>
      </nav>
    </div>
  );
}
