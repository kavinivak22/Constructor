'use client';

import * as React from 'react';
import {
    Calculator,
    Calendar,
    CreditCard,
    Settings,
    User,
    LayoutDashboard,
    Briefcase,
    Users,
    FileText,
    Package,
    Search,
    PlusCircle,
    LogOut,
    Sun,
    Moon,
    Laptop,
    Building2,
    Hammer,
    MapPin
} from 'lucide-react';
import { useTheme } from 'next-themes';
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
import { useSupabase } from '@/supabase/provider';
import { useProjects, useUserProfile } from '@/hooks/queries';
import { useTranslation } from '@/lib/i18n/language-context';

export function CommandMenu() {
    const [open, setOpen] = React.useState(false);
    const { setTheme } = useTheme();
    const router = useRouter();
    const { supabase } = useSupabase();
    const { t } = useTranslation();

    // Fetch user's projects for dynamic search
    const { data: projects = [] } = useProjects();
    const { data: userProfile } = useUserProfile();

    const hasPermission = (permission: string) => {
        if (!userProfile) return false;

        const role = userProfile.role;
        const permissions = userProfile.permissions || {};

        // Admins and Managers have default access
        if (role === 'admin' || role === 'manager') return true;

        // Check specific permission override
        return !!permissions[permission];
    };

    const canCreateProject = hasPermission('create_project');
    const canManageEmployees = hasPermission('manage_employees');

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

    const handleLogout = async () => {
        await supabase.auth.signOut();
        router.push('/login');
    };

    return (
        <>
            <Button
                variant="outline"
                className="relative h-9 w-full justify-start rounded-[0.5rem] bg-muted/50 backdrop-blur-sm text-sm font-normal text-muted-foreground shadow-none sm:pr-12 md:w-40 lg:w-64 hover:bg-muted/80 transition-colors"
                onClick={() => setOpen(true)}
            >
                <Search className="mr-2 h-4 w-4 shrink-0 opacity-50" />
                <span className="hidden lg:inline-flex">Search projects...</span>
                <span className="inline-flex lg:hidden">Search...</span>
                <kbd className="pointer-events-none absolute right-[0.3rem] top-[0.3rem] hidden h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium opacity-100 sm:flex">
                    <span className="text-xs">⌘</span>K
                </kbd>
            </Button>
            <CommandDialog open={open} onOpenChange={setOpen}>
                <CommandInput placeholder="Type a command or search..." />
                <CommandList>
                    <CommandEmpty>No results found.</CommandEmpty>

                    {/* Dynamic Projects Group */}
                    <CommandGroup heading="Projects">
                        {projects.map((project) => (
                            <CommandItem
                                key={project.id}
                                onSelect={() => runCommand(() => router.push(`/projects/${project.id}`))}
                                value={`project ${project.name}`}
                            >
                                <Building2 className="mr-2 h-4 w-4" />
                                <span>{project.name}</span>
                                <span className="ml-2 text-xs text-muted-foreground truncate max-w-[100px]">{project.clientName || project.status}</span>
                            </CommandItem>
                        ))}
                    </CommandGroup>

                    <CommandSeparator />

                    <CommandGroup heading="Navigation">
                        <CommandItem onSelect={() => runCommand(() => router.push('/'))} value="dashboard">
                            <LayoutDashboard className="mr-2 h-4 w-4" />
                            <span>Dashboard</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => router.push('/projects'))} value="projects">
                            <Briefcase className="mr-2 h-4 w-4" />
                            <span>All Projects</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => router.push('/employees'))} value="employees">
                            <Users className="mr-2 h-4 w-4" />
                            <span>Employees</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => router.push('/worklog'))} value="worklogs">
                            <FileText className="mr-2 h-4 w-4" />
                            <span>Daily Worklogs</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => router.push('/inventory'))} value="inventory">
                            <Package className="mr-2 h-4 w-4" />
                            <span>Inventory</span>
                        </CommandItem>
                    </CommandGroup>

                    <CommandSeparator />

                    <CommandGroup heading="Quick Actions">
                        {canCreateProject && (
                            <CommandItem onSelect={() => runCommand(() => router.push('/projects/create'))} value="create project">
                                <PlusCircle className="mr-2 h-4 w-4" />
                                <span>Create Project</span>
                            </CommandItem>
                        )}
                        {canManageEmployees && (
                            <CommandItem onSelect={() => runCommand(() => router.push('/employees/add'))} value="add employee">
                                <User className="mr-2 h-4 w-4" />
                                <span>Add Employee</span>
                            </CommandItem>
                        )}
                        <CommandItem onSelect={() => runCommand(() => router.push('/expenses/add'))} value="add expense">
                            <CreditCard className="mr-2 h-4 w-4" />
                            <span>Add Expense</span>
                        </CommandItem>
                    </CommandGroup>

                    <CommandSeparator />

                    <CommandGroup heading="Settings & System">
                        <CommandItem onSelect={() => runCommand(() => router.push('/profile'))} value="profile">
                            <User className="mr-2 h-4 w-4" />
                            <span>Profile</span>
                            <CommandShortcut>⌘P</CommandShortcut>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => router.push('/settings'))} value="settings">
                            <Settings className="mr-2 h-4 w-4" />
                            <span>Settings</span>
                            <CommandShortcut>⌘S</CommandShortcut>
                        </CommandItem>
                        {/* Theme Toggles */}
                        <CommandItem onSelect={() => runCommand(() => setTheme('light'))} value="light theme">
                            <Sun className="mr-2 h-4 w-4" />
                            <span>Light Mode</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => setTheme('dark'))} value="dark theme">
                            <Moon className="mr-2 h-4 w-4" />
                            <span>Dark Mode</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => setTheme('system'))} value="system theme">
                            <Laptop className="mr-2 h-4 w-4" />
                            <span>System Theme</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(handleLogout)} className="text-red-500" value="logout">
                            <LogOut className="mr-2 h-4 w-4" />
                            <span>Log out</span>
                        </CommandItem>
                    </CommandGroup>
                </CommandList>
            </CommandDialog>
        </>
    );
}
