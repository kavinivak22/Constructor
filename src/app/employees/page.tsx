
'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { PlusCircle, Search, Users } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { EmployeeCard } from '@/components/employees/employee-card';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/hooks/use-toast';
import { type User as AppUser, type Project } from '@/lib/data';
import { AddEmployeeSheet } from '@/components/employees/add-employee-sheet';
import { useSupabase } from '@/supabase/provider';
import { useEffect } from 'react';
import { getPendingInvites } from '@/app/actions/employees';
import { PendingInvitesList, PendingInvite } from '@/components/employees/pending-invites-list';

export default function EmployeesPage() {
    const { toast } = useToast();
    const { supabase } = useSupabase();

    const [searchQuery, setSearchQuery] = useState('');
    const [isSheetOpen, setIsSheetOpen] = useState(false);
    const [editingUser, setEditingUser] = useState<AppUser | null>(null);
    const [employees, setEmployees] = useState<AppUser[]>([]);
    const [projects, setProjects] = useState<Project[]>([]);
    const [pendingInvites, setPendingInvites] = useState<PendingInvite[]>([]);
    const [currentUserProfile, setCurrentUserProfile] = useState<AppUser | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const [refreshTrigger, setRefreshTrigger] = useState(0);

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const { data: { user } } = await supabase.auth.getUser();
                if (!user) return;

                // Fetch current user profile
                const { data: profile } = await supabase
                    .from('users')
                    .select('*')
                    .eq('id', user.id)
                    .single();

                if (profile) setCurrentUserProfile(profile);

                if (profile?.companyId) {
                    // Fetch company users
                    const { data: usersData } = await supabase
                        .from('users')
                        .select('*')
                        .eq('companyId', profile.companyId);
                    if (usersData) setEmployees(usersData);

                    // Fetch company projects
                    const { data: projectsData } = await supabase
                        .from('projects')
                        .select('*')
                        .eq('companyId', profile.companyId);
                    if (projectsData) setProjects(projectsData);

                    // Fetch pending invites
                    const invites = await getPendingInvites();
                    setPendingInvites(invites as PendingInvite[]);
                }
            } catch (error) {
                console.error('Error fetching data:', error);
                toast({
                    title: 'Error',
                    description: 'Failed to load employees data.',
                    variant: 'destructive',
                });
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, [supabase, toast, refreshTrigger]);

    const isAdmin = currentUserProfile?.role === 'admin';

    const handleAddNew = () => {
        setEditingUser(null);
        setIsSheetOpen(true);
    };

    const handleEdit = (employee: AppUser) => {
        setEditingUser(employee);
        setIsSheetOpen(true);
    };

    const handleStatusChange = async (employee: AppUser, newStatus: 'active' | 'inactive') => {
        try {
            const { error } = await supabase
                .from('users')
                .update({ status: newStatus })
                .eq('id', employee.id);

            if (error) throw error;

            setEmployees(employees.map(e =>
                e.id === employee.id ? { ...e, status: newStatus } : e
            ));

            toast({
                title: 'Status Updated',
                description: `${employee.displayName}'s status has been updated to ${newStatus}.`,
            });
        } catch (error) {
            toast({
                title: 'Error',
                description: 'Failed to update status.',
                variant: 'destructive',
            });
        }
    };

    const filteredUsers = employees.filter(employee =>
        (employee.displayName?.toLowerCase() || '').includes(searchQuery.toLowerCase()) ||
        (employee.email?.toLowerCase() || '').includes(searchQuery.toLowerCase())
    );

    if (isLoading) {
        return (
            <div className="flex flex-col h-full">
                <header className="flex items-center justify-between gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
                    <h1 className="text-2xl font-bold tracking-tight font-headline">Employees</h1>
                    <Skeleton className="h-10 w-32" />
                </header>
                <main className="flex-1 p-4 overflow-y-auto md:p-6">
                    <div className="space-y-4">
                        <Skeleton className="h-10 w-full max-w-sm" />
                        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                            {[...Array(6)].map((_, i) => <Skeleton key={i} className="h-64" />)}
                        </div>
                    </div>
                </main>
            </div>
        );
    }

    return (
        <>
            <div className="flex flex-col h-full">
                <header className="flex flex-col items-start justify-between gap-4 p-4 border-b md:flex-row md:items-center md:px-6 shrink-0 bg-background sticky top-0 z-10">
                    <h1 className="text-2xl font-bold tracking-tight font-headline">
                        {isAdmin ? 'Employee Management' : 'My Colleagues'}
                    </h1>
                    <div className="flex items-center w-full gap-4 md:w-auto">
                        <div className="relative flex-1">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                            <Input
                                type="search"
                                placeholder={isAdmin ? "Search employees..." : "Search colleagues..."}
                                className="pl-10"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                        </div>
                        {isAdmin && (
                            <Button onClick={handleAddNew}>
                                <PlusCircle className="w-4 h-4 mr-2" />
                                Add Employee
                            </Button>
                        )}
                    </div>
                </header>
                <main className="flex-1 p-4 overflow-y-auto md:p-6">
                    {filteredUsers.length > 0 ? (
                        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                            {filteredUsers.map(employee => (
                                <EmployeeCard
                                    key={employee.id}
                                    employee={employee}
                                    projects={projects.filter(p => employee.projectIds?.includes(p.id)) ?? []}
                                    onEdit={isAdmin ? handleEdit : undefined}
                                    onStatusChange={isAdmin ? handleStatusChange : undefined}
                                    isCurrentUser={currentUserProfile?.id === employee.id}
                                />
                            ))}
                        </div>
                    ) : (
                        <div className="flex flex-col items-center justify-center h-full text-center rounded-lg border-2 border-dashed bg-card/50 p-6">
                            <Users className="w-12 h-12 mb-4 text-muted-foreground" />
                            <h2 className="text-2xl font-bold font-headline">{isAdmin ? "No Employees Found" : "No Colleagues Found"}</h2>
                            <p className="max-w-sm mt-2 text-muted-foreground">
                                {isAdmin
                                    ? "Get started by adding your first employee to the company."
                                    : "You are not assigned to any projects with other team members."
                                }
                            </p>
                        </div>
                    )}

                    {isAdmin && <PendingInvitesList invites={pendingInvites} />}
                </main>
            </div>

            <AddEmployeeSheet
                isOpen={isSheetOpen}
                onOpenChange={setIsSheetOpen}
                projects={projects}
                onSuccess={() => setRefreshTrigger(prev => prev + 1)}
                editingUser={editingUser}
            />
        </>
    );
}
