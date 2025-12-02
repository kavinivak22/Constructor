
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

    const [activeTab, setActiveTab] = useState<'current' | 'ex'>('current');
    const [exEmployees, setExEmployees] = useState<any[]>([]);

    useEffect(() => {
        if (activeTab === 'ex' && currentUserProfile?.role === 'admin') {
            const fetchExEmployees = async () => {
                const { getExEmployees } = await import('@/app/actions/employees');
                const data = await getExEmployees();
                setExEmployees(data);
            };
            fetchExEmployees();
        }
    }, [activeTab, currentUserProfile]);

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

    const handleRemove = async (employee: AppUser) => {
        try {
            const { removeEmployee } = await import('@/app/actions/employees');
            const result = await removeEmployee(employee.id);

            if (result.success) {
                setEmployees(employees.filter(e => e.id !== employee.id));
                toast({
                    title: 'Employee Removed',
                    description: `${employee.displayName} has been removed from the company.`,
                });
                // Refresh ex-employees if tab is active (though we are on current tab here)
            } else {
                throw new Error(result.error);
            }
        } catch (error: any) {
            toast({
                title: 'Error',
                description: error.message || 'Failed to remove employee.',
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
                <header className="flex flex-col gap-4 p-4 border-b bg-background sticky top-0 z-10">
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div className="flex items-center justify-between">
                            <h1 className="text-2xl font-bold tracking-tight font-headline">
                                {isAdmin ? 'Employee Management' : 'My Colleagues'}
                            </h1>
                            {/* Mobile Tab Toggle - Visible only on small screens */}
                            {isAdmin && (
                                <div className="md:hidden flex items-center bg-muted rounded-lg p-1">
                                    <button
                                        onClick={() => setActiveTab('current')}
                                        className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${activeTab === 'current' ? 'bg-background shadow-sm text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
                                    >
                                        Current
                                    </button>
                                    <button
                                        onClick={() => setActiveTab('ex')}
                                        className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${activeTab === 'ex' ? 'bg-background shadow-sm text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
                                    >
                                        Ex
                                    </button>
                                </div>
                            )}
                        </div>

                        {/* Desktop Tab Toggle - Hidden on small screens */}
                        {isAdmin && (
                            <div className="hidden md:flex items-center bg-muted rounded-lg p-1">
                                <button
                                    onClick={() => setActiveTab('current')}
                                    className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${activeTab === 'current' ? 'bg-background shadow-sm text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
                                >
                                    Current
                                </button>
                                <button
                                    onClick={() => setActiveTab('ex')}
                                    className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${activeTab === 'ex' ? 'bg-background shadow-sm text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
                                >
                                    Ex-Employees
                                </button>
                            </div>
                        )}

                        <div className="flex items-center w-full gap-2 md:w-auto">
                            {activeTab === 'current' && (
                                <div className="relative flex-1 md:w-64">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                                    <Input
                                        type="search"
                                        placeholder={isAdmin ? "Search..." : "Search colleagues..."}
                                        className="pl-9 h-9"
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                    />
                                </div>
                            )}
                            {isAdmin && activeTab === 'current' && (
                                <Button onClick={handleAddNew} size="sm" className="shrink-0">
                                    <PlusCircle className="w-4 h-4 md:mr-2" />
                                    <span className="hidden md:inline">Add Employee</span>
                                    <span className="md:hidden">Add</span>
                                </Button>
                            )}
                        </div>
                    </div>
                </header>
                <main className="flex-1 p-4 overflow-y-auto md:p-6">
                    {activeTab === 'current' ? (
                        <>
                            {filteredUsers.length > 0 ? (
                                <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                                    {filteredUsers.map(employee => (
                                        <EmployeeCard
                                            key={employee.id}
                                            employee={employee}
                                            projects={projects.filter(p => employee.projectIds?.includes(p.id)) ?? []}
                                            onEdit={isAdmin ? handleEdit : undefined}
                                            onStatusChange={isAdmin ? handleStatusChange : undefined}
                                            onRemove={isAdmin ? handleRemove : undefined}
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
                        </>
                    ) : (
                        // Ex-Employees List
                        <div className="space-y-4">
                            {exEmployees.length > 0 ? (
                                <div className="border rounded-lg overflow-hidden">
                                    <table className="w-full text-sm text-left">
                                        <thead className="bg-muted/50 text-muted-foreground font-medium border-b">
                                            <tr>
                                                <th className="px-4 py-3">Name</th>
                                                <th className="px-4 py-3">Role</th>
                                                <th className="px-4 py-3">Exit Date</th>
                                                <th className="px-4 py-3">Reason</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y">
                                            {exEmployees.map((ex: any) => (
                                                <tr key={ex.id} className="bg-background hover:bg-muted/50">
                                                    <td className="px-4 py-3 font-medium">
                                                        <div className="flex items-center gap-2">
                                                            {ex.user_details?.displayName || 'Unknown'}
                                                            <span className="text-xs text-muted-foreground font-normal">({ex.user_details?.email})</span>
                                                        </div>
                                                    </td>
                                                    <td className="px-4 py-3 capitalize">{ex.role}</td>
                                                    <td className="px-4 py-3 text-muted-foreground">
                                                        {new Date(ex.exit_date).toLocaleDateString()}
                                                    </td>
                                                    <td className="px-4 py-3 capitalize">{ex.exit_reason || '-'}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div className="flex flex-col items-center justify-center py-12 text-center rounded-lg border-2 border-dashed bg-card/50">
                                    <Users className="w-12 h-12 mb-4 text-muted-foreground" />
                                    <h2 className="text-lg font-semibold">No Ex-Employees</h2>
                                    <p className="text-sm text-muted-foreground">History of employees who left the company will appear here.</p>
                                </div>
                            )}
                        </div>
                    )}
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
