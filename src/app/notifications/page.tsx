
'use client';

import { Bell, MessageSquare, Package, FileText } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useToast } from '@/hooks/use-toast';

const iconMap = {
    team: MessageSquare,
    inventory: Package,
    approvals: FileText,
};

export default function NotificationsPage() {
    const { toast } = useToast();

    const handleMarkAllAsRead = async () => {
        toast({
            title: 'Success',
            description: 'All notifications marked as read.',
        });
    };

    return (
        <div className="flex flex-col h-full bg-transparent">
            <header className="flex items-center justify-between gap-3 p-3.5 sm:p-4 md:px-6 shrink-0 glass sticky top-0 z-10 border-b border-white/10">
                <h1 className="text-xl sm:text-2xl font-bold tracking-tight font-headline text-foreground">
                    Notifications
                </h1>
                <Button variant="outline" size="sm" onClick={handleMarkAllAsRead} className="h-8 sm:h-9 text-xs sm:text-sm rounded-xl">
                    Mark all as read
                </Button>
            </header>
            <main className="flex-1 p-3.5 sm:p-4 md:p-6 overflow-y-auto">
                <div className="max-w-2xl mx-auto">
                    <div className="flex flex-col items-center justify-center h-full text-center glass-card border border-white/10 p-8 sm:p-12 rounded-3xl shadow-sm">
                        <div className="bg-primary/10 p-4 rounded-2xl mb-3 text-primary">
                            <Bell className="w-8 h-8 sm:w-10 sm:h-10" />
                        </div>
                        <h2 className="text-lg sm:text-2xl font-bold font-headline text-foreground">All Caught Up!</h2>
                        <p className="max-w-sm mt-1.5 text-xs sm:text-sm text-muted-foreground">
                            You don't have any notifications right now.
                        </p>
                    </div>
                </div>
            </main>
        </div>
    );
}
