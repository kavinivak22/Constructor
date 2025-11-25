
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
        <div className="flex flex-col h-full">
            <header className="flex items-center justify-between gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
                <h1 className="text-2xl font-bold tracking-tight font-headline">
                    Notifications
                </h1>
                <Button variant="outline" size="sm" onClick={handleMarkAllAsRead}>
                    Mark all as read
                </Button>
            </header>
            <main className="flex-1 p-4 overflow-y-auto md:p-6">
                <div className="max-w-2xl mx-auto">
                    <div className="flex flex-col items-center justify-center h-full text-center rounded-lg border-2 border-dashed bg-card/50 p-12">
                        <Bell className="w-12 h-12 text-muted-foreground mb-4" />
                        <h2 className="text-2xl font-bold font-headline">All Caught Up!</h2>
                        <p className="max-w-sm mt-2 text-muted-foreground">
                            You don't have any notifications right now.
                        </p>
                    </div>
                </div>
            </main>
        </div>
    );
}
