'use client';

import * as React from 'react';
import {
    Calculator,
    Calendar,
    CreditCard,
    Settings,
    Smile,
    User,
    LayoutDashboard,
    Briefcase,
    Users,
    FileText,
    Package,
    Search,
    PlusCircle
} from 'lucide-react';

import {
    CommandDialog,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    CommandSeparator,
    CommandShortcut,
} from '@/components/ui/command';
import { Button } from '@/components/ui/button';
import { useRouter } from 'next/navigation';

export function CommandMenu() {
    const [open, setOpen] = React.useState(false);
    const router = useRouter();

    React.useEffect(() => {
        const down = (e: KeyboardEvent) => {
            if (e.key === 'k' && (e.metaKey || e.ctrlKey)) {
                e.preventDefault();
                setOpen((open) => !open);
            }
        };

        document.addEventListener('keydown', down);
        return () => document.removeEventListener('keydown', down);
    }, []);

    const runCommand = React.useCallback((command: () => unknown) => {
        setOpen(false);
        command();
    }, []);

    return (
        <>
            <Button
                variant="outline"
                className="relative h-9 w-full justify-start rounded-[0.5rem] bg-background text-sm font-normal text-muted-foreground shadow-none sm:pr-12 md:w-40 lg:w-64"
                onClick={() => setOpen(true)}
            >
                <span className="hidden lg:inline-flex">Search...</span>
                <span className="inline-flex lg:hidden">Search...</span>
                <kbd className="pointer-events-none absolute right-[0.3rem] top-[0.3rem] hidden h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium opacity-100 sm:flex">
                    <span className="text-xs">⌘</span>K
                </kbd>
            </Button>
            <CommandDialog open={open} onOpenChange={setOpen}>
                <CommandInput placeholder="Type a command or search..." />
                <CommandList>
                    <CommandEmpty>No results found.</CommandEmpty>
                    <CommandGroup heading="Navigation">
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/dashboard'))}
                        >
                            <LayoutDashboard className="mr-2 h-4 w-4" />
                            <span>Dashboard</span>
                        </CommandItem>
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/projects'))}
                        >
                            <Briefcase className="mr-2 h-4 w-4" />
                            <span>Projects</span>
                        </CommandItem>
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/employees'))}
                        >
                            <Users className="mr-2 h-4 w-4" />
                            <span>Employees</span>
                        </CommandItem>
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/worklogs'))}
                        >
                            <FileText className="mr-2 h-4 w-4" />
                            <span>Worklogs</span>
                        </CommandItem>
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/inventory'))}
                        >
                            <Package className="mr-2 h-4 w-4" />
                            <span>Inventory</span>
                        </CommandItem>
                    </CommandGroup>
                    <CommandSeparator />
                    <CommandGroup heading="Quick Actions">
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/projects/new'))} // Assuming this route exists or similar
                        >
                            <PlusCircle className="mr-2 h-4 w-4" />
                            <span>Create Project</span>
                        </CommandItem>
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/profile'))}
                        >
                            <User className="mr-2 h-4 w-4" />
                            <span>Profile</span>
                            <CommandShortcut>⌘P</CommandShortcut>
                        </CommandItem>
                        <CommandItem
                            onSelect={() => runCommand(() => router.push('/settings'))}
                        >
                            <Settings className="mr-2 h-4 w-4" />
                            <span>Settings</span>
                            <CommandShortcut>⌘S</CommandShortcut>
                        </CommandItem>
                    </CommandGroup>
                </CommandList>
            </CommandDialog>
        </>
    );
}
