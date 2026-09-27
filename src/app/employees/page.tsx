'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { PlusCircle, Search, Users, Wallet, Landmark, Loader2, Check } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { EmployeeCard } from '@/components/employees/employee-card';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/hooks/use-toast';
import { type User as AppUser, type Project } from '@/lib/data';
import { AddEmployeeSheet } from '@/components/employees/add-employee-sheet';
import { useSupabase } from '@/supabase/provider';
import { getPendingInvites } from '@/app/actions/employees';
import { getSalaryProfiles, saveSalaryProfile } from '@/app/actions/financials';
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

    // Salary & Payout Management State
    const [salaryProfiles, setSalaryProfiles] = useState<any[]>([]);
    const [selectedSalaryEmployee, setSelectedSalaryEmployee] = useState<AppUser | null>(null);
    const [isSalaryModalOpen, setIsSalaryModalOpen] = useState(false);
    const [salaryPaymentType, setSalaryPaymentType] = useState<'monthly' | 'daily_wage' | 'hourly'>('monthly');
    const [salaryRate, setSalaryRate] = useState('');
    const [salaryBankName, setSalaryBankName] = useState('');
    const [salaryAccountNumber, setSalaryAccountNumber] = useState('');
    const [salaryIfscCode, setSalaryIfscCode] = useState('');
    const [isSavingSalary, setIsSavingSalary] = useState(false);

    useEffect(() => {
        if (activeTab === 'ex' && currentUserProfile?.role === 'admin') {
            const fetchExEmployees = async () => {
                const { getExEmployees } = await import('@/app/actions/employees');
                const data = await getExEmployees();
                setExEmployees(data);
            };
            fetchExEmployees();
        }
    }, [activeTab, currentUserProfile, refreshTrigger]);

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

                if (profile) {
                    // Fetch user's project IDs from project_members table
                    const { data: memberProjects } = await supabase
                        .from('project_members')
                        .select('project_id')
                        .eq('user_id', user.id);
                    
                    const userProjectIds = memberProjects?.map(m => m.project_id) || [];

                    const mappedProfile: AppUser = {
                        id: profile.id,
                        email: profile.email,
                        displayName: profile.display_name,
                        phone: profile.phone || undefined,
                        photoURL: profile.photo_url || undefined,
                        role: profile.role,
                        projectIds: userProjectIds,
                        permissions: profile.permissions || undefined,
                        companyId: profile.company_id,
                        status: profile.status,
                        created_at: profile.created_at
                    };

                    setCurrentUserProfile(mappedProfile);

                    const companyId = profile.company_id;
                    if (companyId) {
                        // Fetch company users
                        const { data: usersData } = await supabase
                            .from('users')
                            .select('*')
                            .eq('company_id', companyId);

                        // Fetch project memberships for all users in this company
                        let allMemberships: any[] = [];
                        if (usersData && usersData.length > 0) {
                            const { data: membersData } = await supabase
                                .from('project_members')
                                .select('user_id, project_id')
                                .in('user_id', usersData.map(u => u.id));
                            allMemberships = membersData || [];
                        }

                        const mappedEmployees: AppUser[] = (usersData || []).map(u => ({
                            id: u.id,
                            email: u.email,
                            displayName: u.display_name,
                            phone: u.phone || undefined,
                            photoURL: u.photo_url || undefined,
                            role: u.role,
                            projectIds: allMemberships
                                .filter(m => m.user_id === u.id)
                                .map(m => m.project_id) || [],
                            permissions: u.permissions || undefined,
                            companyId: u.company_id,
                            status: u.status,
                            created_at: u.created_at
                        }));
                        setEmployees(mappedEmployees);

                        // Fetch company projects
                        const { data: projectsData } = await supabase
                            .from('projects')
                            .select('*')
                            .eq('company_id', companyId);

                        const mappedProjects: Project[] = (projectsData || []).map(p => ({
                            ...p,
                            companyId: p.company_id,
                            startDate: p.start_date,
                            endDate: p.end_date,
                            clientName: p.client_name,
                            clientContact: p.client_contact,
                            projectType: p.project_type
                        }));
                        setProjects(mappedProjects);

                        // Fetch pending invites
                        const invites = await getPendingInvites();
                        setPendingInvites(invites as PendingInvite[]);

                        // Fetch salary profiles
                        const profiles = await getSalaryProfiles();
                        setSalaryProfiles(profiles || []);
                    }
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

    const handleOpenSalaryModal = (employee: AppUser) => {
        setSelectedSalaryEmployee(employee);
        const existing = salaryProfiles.find(p => p.user_id === employee.id);
        if (existing) {
            setSalaryPaymentType(existing.payment_type || 'monthly');
            setSalaryRate(existing.rate ? existing.rate.toString() : '');
            setSalaryBankName(existing.bank_name || '');
            setSalaryAccountNumber(existing.account_number || '');
            setSalaryIfscCode(existing.ifsc_code || '');
        } else {
            setSalaryPaymentType('monthly');
            setSalaryRate('');
            setSalaryBankName('');
            setSalaryAccountNumber('');
            setSalaryIfscCode('');
        }
        setIsSalaryModalOpen(true);
    };

    const handleSaveSalary = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedSalaryEmployee) return;

        if (!salaryRate || isNaN(Number(salaryRate)) || Number(salaryRate) <= 0) {
            toast({
                title: 'Error',
                description: 'Please enter a valid salary amount greater than 0.',
                variant: 'destructive'
            });
            return;
        }

        setIsSavingSalary(true);
        try {
            const existing = salaryProfiles.find(p => p.user_id === selectedSalaryEmployee.id);
            const res = await saveSalaryProfile({
                id: existing?.id,
                user_id: selectedSalaryEmployee.id,
                payment_type: salaryPaymentType,
                rate: Number(salaryRate),
                bank_name: salaryBankName.trim() || undefined,
                account_number: salaryAccountNumber.trim() || undefined,
                ifsc_code: salaryIfscCode.trim() || undefined
            });

            if (res.success) {
                toast({
                    title: 'Salary Profile Saved',
                    description: `${selectedSalaryEmployee.displayName}'s salary and payout details updated.`
                });
                setIsSalaryModalOpen(false);
                const updatedProfiles = await getSalaryProfiles();
                setSalaryProfiles(updatedProfiles || []);
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to save salary profile.',
                    variant: 'destructive'
                });
            }
        } catch (err: any) {
            toast({
                title: 'Error',
                description: err.message || 'Something went wrong.',
                variant: 'destructive'
            });
        } finally {
            setIsSavingSalary(false);
        }
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
                title: "Status Updated",
                description: `${employee.displayName} is now ${newStatus}.`,
            });
        } catch (error) {
            console.error('Error updating status:', error);
            toast({
                variant: "destructive",
                title: "Error",
                description: "Failed to update employee status.",
            });
        }
    };

    const handleRemove = async (employee: AppUser) => {
        try {
            const { error } = await supabase
                .from('users')
                .update({ company_id: null, status: 'inactive' })
                .eq('id', employee.id);

            if (error) throw error;

            setEmployees(employees.filter(e => e.id !== employee.id));

            toast({
                title: "Employee Removed",
                description: `${employee.displayName} has been removed from the company.`,
            });
        } catch (error) {
            console.error('Error removing employee:', error);
            toast({
                variant: "destructive",
                title: "Error",
                description: "Failed to remove employee.",
            });
        }
    };

    const filteredUsers = employees.filter(employee =>
        employee.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        employee.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
        employee.role.toLowerCase().includes(searchQuery.toLowerCase())
    );

    if (isLoading) {
        return (
            <div className="flex-1 p-4 md:p-6 space-y-6">
                <header className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                    <Skeleton className="h-8 w-48" />
                    <div className="flex items-center gap-2">
                        <Skeleton className="h-9 w-64" />
                        <Skeleton className="h-9 w-32" />
                    </div>
                </header>
                <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                    {[1, 2, 3, 4, 5, 6].map(i => (
                        <div key={i} className="flex flex-col space-y-3 p-4 border rounded-xl">
                            <div className="flex items-center space-x-4">
                                <Skeleton className="h-12 w-12 rounded-full" />
                                <div className="space-y-2">
                                    <Skeleton className="h-4 w-32" />
                                    <Skeleton className="h-4 w-20" />
                                </div>
                            </div>
                            <div className="space-y-2 pt-4">
                                <Skeleton className="h-4 w-full" />
                                <Skeleton className="h-4 w-3/4" />
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    return (
        <>
            <div className="flex flex-col flex-1 h-full overflow-hidden">
                <header className="p-4 border-b md:p-6 bg-background/95 backdrop-blur-xs sticky top-0 z-10">
                    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                        <div className="flex items-center justify-between">
                            <h1 className="text-2xl font-bold tracking-tight font-headline">
                                {isAdmin ? 'Employee Management' : 'My Colleagues'}
                            </h1>
                            {/* Mobile Tab Toggle - Visible only on small screens */}
                            {isAdmin && (
                                <div className="md:hidden flex items-center bg-muted rounded-lg p-1">
                                    <button
                                        onClick={() => setActiveTab('current')}
                                        className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${activeTab === 'current' ? 'bg-background shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
                                    >
                                        Current
                                    </button>
                                    <button
                                        onClick={() => setActiveTab('ex')}
                                        className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${activeTab === 'ex' ? 'bg-background shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
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
                                    className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${activeTab === 'current' ? 'bg-background shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
                                >
                                    Current
                                </button>
                                <button
                                    onClick={() => setActiveTab('ex')}
                                    className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${activeTab === 'ex' ? 'bg-background shadow-xs text-foreground' : 'text-muted-foreground hover:text-foreground'}`}
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
                                            salaryProfile={salaryProfiles.find(p => p.user_id === employee.id)}
                                            onManageSalary={isAdmin ? handleOpenSalaryModal : undefined}
                                            isAdmin={isAdmin}
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

            {/* Manage Employee Salary & Payout Modal */}
            <Dialog open={isSalaryModalOpen} onOpenChange={setIsSalaryModalOpen}>
                <DialogContent className="max-w-md rounded-2xl p-4 sm:p-5 glass-card border border-white/20 shadow-2xl">
                    <DialogHeader className="pb-2 border-b border-border/40">
                        <DialogTitle className="text-base sm:text-lg font-bold font-headline text-foreground flex items-center gap-2">
                            <Wallet className="h-4 w-4 text-primary" />
                            <span>Salary & Bank Account</span>
                        </DialogTitle>
                        <DialogDescription className="text-xs text-muted-foreground">
                            Configure salary rate and payout bank details for {selectedSalaryEmployee?.displayName}.
                        </DialogDescription>
                    </DialogHeader>

                    <form onSubmit={handleSaveSalary} className="space-y-3.5 pt-1">
                        <div className="grid grid-cols-2 gap-2 text-xs">
                            <div>
                                <Label className="text-[11px] font-semibold text-foreground">Payment Type</Label>
                                <Select
                                    value={salaryPaymentType}
                                    onValueChange={(val: any) => setSalaryPaymentType(val)}
                                >
                                    <SelectTrigger className="h-8 text-xs rounded-lg bg-background/50 mt-1">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="monthly" className="text-xs">Monthly Salary</SelectItem>
                                        <SelectItem value="daily_wage" className="text-xs">Daily Wage</SelectItem>
                                        <SelectItem value="hourly" className="text-xs">Hourly Rate</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>

                            <div>
                                <Label className="text-[11px] font-semibold text-foreground">
                                    Amount (₹)
                                </Label>
                                <Input
                                    type="number"
                                    placeholder={salaryPaymentType === 'monthly' ? 'e.g. 45000' : 'e.g. 1200'}
                                    value={salaryRate}
                                    onChange={(e) => setSalaryRate(e.target.value)}
                                    className="h-8 text-xs rounded-lg bg-background/50 font-mono mt-1"
                                />
                            </div>
                        </div>

                        {/* Bank Details */}
                        <div className="space-y-2 pt-2 border-t border-border/40 text-xs">
                            <Label className="text-[11px] font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                                <Landmark className="h-3.5 w-3.5 text-primary" />
                                Bank Payout Details
                            </Label>

                            <div>
                                <Label className="text-[10px] text-muted-foreground">Bank Name</Label>
                                <Input
                                    placeholder="e.g. HDFC Bank, SBI, ICICI"
                                    value={salaryBankName}
                                    onChange={(e) => setSalaryBankName(e.target.value)}
                                    className="h-8 text-xs rounded-lg bg-background/50 mt-0.5"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-2">
                                <div>
                                    <Label className="text-[10px] text-muted-foreground">Account Number</Label>
                                    <Input
                                        placeholder="A/C Number"
                                        value={salaryAccountNumber}
                                        onChange={(e) => setSalaryAccountNumber(e.target.value)}
                                        className="h-8 text-xs rounded-lg bg-background/50 font-mono mt-0.5"
                                    />
                                </div>
                                <div>
                                    <Label className="text-[10px] text-muted-foreground">IFSC Code</Label>
                                    <Input
                                        placeholder="IFSC Code"
                                        value={salaryIfscCode}
                                        onChange={(e) => setSalaryIfscCode(e.target.value.toUpperCase())}
                                        className="h-8 text-xs rounded-lg bg-background/50 uppercase font-mono mt-0.5"
                                    />
                                </div>
                            </div>
                        </div>

                        <DialogFooter className="flex-row items-center justify-end gap-2 pt-2 border-t border-border/30">
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setIsSalaryModalOpen(false)}
                                className="h-8 text-xs rounded-lg"
                            >
                                Cancel
                            </Button>
                            <Button
                                type="submit"
                                size="sm"
                                disabled={isSavingSalary}
                                className="h-8 text-xs rounded-lg gap-1"
                            >
                                {isSavingSalary ? (
                                    <>
                                        <Loader2 className="h-3 w-3 animate-spin" />
                                        <span>Saving...</span>
                                    </>
                                ) : (
                                    <>
                                        <Check className="h-3 w-3" />
                                        <span>Save Salary</span>
                                    </>
                                )}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>
        </>
    );
}
