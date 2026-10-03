'use client';

import { useState, useEffect, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
    Loader2,
    Shield,
    Building2,
    FolderKanban,
    Receipt,
    Package,
    FileSpreadsheet,
    ClipboardList,
    Wallet,
    FileText,
    Sparkles,
    Search,
    CheckCircle2,
    UserCheck,
    UserPlus
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { useToast } from '@/hooks/use-toast';
import { inviteEmployee, updateEmployee, checkEmailStatus, rejoinEmployee } from '@/app/actions/employees';
import {
    Project,
    type User as AppUser,
    EmployeePermissions,
    DEFAULT_EMPTY_PERMISSIONS,
    ADMIN_PERMISSIONS_PRESET,
    MANAGER_PERMISSIONS_PRESET,
    SITE_ENGINEER_PRESET,
    ACCOUNTANT_PRESET,
    MEMBER_PERMISSIONS_PRESET
} from '@/lib/data';

const formSchema = z.object({
    email: z.string().email({ message: 'Invalid email address' }),
    role: z.enum(['admin', 'manager', 'member']),
    preset: z.string().default('custom'),
    projectIds: z.array(z.string()).default([]),
    permissions: z.object({
        company: z.object({
            manageCompany: z.boolean().default(false),
            manageEmployees: z.boolean().default(false),
            viewFinancials: z.boolean().default(false),
            manageSalaries: z.boolean().default(false),
        }),
        projects: z.object({
            viewAllProjects: z.boolean().default(false),
            createProjects: z.boolean().default(false),
            editProjects: z.boolean().default(false),
            deleteProjects: z.boolean().default(false),
            manageProjectMembers: z.boolean().default(false),
        }),
        expenses: z.object({
            viewExpenses: z.boolean().default(false),
            createExpenses: z.boolean().default(false),
            approveExpenses: z.boolean().default(false),
            deleteExpenses: z.boolean().default(false),
            exportExpenses: z.boolean().default(false),
        }),
        inventory: z.object({
            viewInventory: z.boolean().default(false),
            manageInventory: z.boolean().default(false),
            logMaterialUsage: z.boolean().default(false),
            manageSuppliers: z.boolean().default(false),
            materialEstimation: z.boolean().default(false),
        }),
        purchaseOrders: z.object({
            viewPurchaseOrders: z.boolean().default(false),
            createPurchaseOrders: z.boolean().default(false),
            approvePurchaseOrders: z.boolean().default(false),
            completePurchaseOrders: z.boolean().default(false),
        }),
        worklogs: z.object({
            viewWorklogs: z.boolean().default(false),
            createWorklogs: z.boolean().default(false),
            approveWorklogs: z.boolean().default(false),
            exportWorklogs: z.boolean().default(false),
        }),
        pouches: z.object({
            viewPouches: z.boolean().default(false),
            requestPouchFunds: z.boolean().default(false),
            approvePouchFunds: z.boolean().default(false),
        }),
        documents: z.object({
            viewDocuments: z.boolean().default(false),
            uploadDocuments: z.boolean().default(false),
            deleteDocuments: z.boolean().default(false),
        }),
    }),
});

export type AddEmployeeFormValues = z.infer<typeof formSchema>;

interface AddEmployeeDialogProps {
    isOpen: boolean;
    onOpenChange: (open: boolean) => void;
    projects: Project[];
    onSuccess?: () => void;
    editingUser?: AppUser | null;
}

export function AddEmployeeDialog({
    isOpen,
    onOpenChange,
    projects,
    onSuccess,
    editingUser = null,
}: AddEmployeeDialogProps) {
    const { toast } = useToast();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [activeTab, setActiveTab] = useState<'details' | 'permissions'>('details');
    const [projectSearch, setProjectSearch] = useState('');
    const [emailStatus, setEmailStatus] = useState<'new' | 'current' | 'ex' | 'other_company' | 'checking'>('new');
    const [exUserId, setExUserId] = useState<string | null>(null);
    const [emailCheckMessage, setEmailCheckMessage] = useState<string | null>(null);

    const isEditMode = !!editingUser;

    const form = useForm<AddEmployeeFormValues>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            email: '',
            role: 'member',
            preset: 'member',
            projectIds: [],
            permissions: MEMBER_PERMISSIONS_PRESET,
        },
    });

    // Helper function to resolve initial permissions based on role or profile
    const getInitialPermissions = (user: AppUser | null): EmployeePermissions => {
        if (user?.permissions && Object.keys(user.permissions).length > 0) {
            return {
                company: { ...DEFAULT_EMPTY_PERMISSIONS.company, ...user.permissions.company },
                projects: { ...DEFAULT_EMPTY_PERMISSIONS.projects, ...user.permissions.projects },
                expenses: { ...DEFAULT_EMPTY_PERMISSIONS.expenses, ...user.permissions.expenses },
                inventory: { ...DEFAULT_EMPTY_PERMISSIONS.inventory, ...user.permissions.inventory },
                purchaseOrders: { ...DEFAULT_EMPTY_PERMISSIONS.purchaseOrders, ...user.permissions.purchaseOrders },
                worklogs: { ...DEFAULT_EMPTY_PERMISSIONS.worklogs, ...user.permissions.worklogs },
                pouches: { ...DEFAULT_EMPTY_PERMISSIONS.pouches, ...user.permissions.pouches },
                documents: { ...DEFAULT_EMPTY_PERMISSIONS.documents, ...user.permissions.documents },
            };
        }

        if (user?.role === 'admin') return ADMIN_PERMISSIONS_PRESET;
        if (user?.role === 'manager') return MANAGER_PERMISSIONS_PRESET;
        return MEMBER_PERMISSIONS_PRESET;
    };

    // Update form state when editingUser or isOpen changes
    useEffect(() => {
        if (isOpen) {
            setActiveTab('details');
            if (editingUser) {
                const perms = getInitialPermissions(editingUser);
                form.reset({
                    email: editingUser.email,
                    role: editingUser.role,
                    preset: 'custom',
                    projectIds: editingUser.projectIds || [],
                    permissions: perms,
                });
            } else {
                form.reset({
                    email: '',
                    role: 'member',
                    preset: 'member',
                    projectIds: [],
                    permissions: MEMBER_PERMISSIONS_PRESET,
                });
            }
        }
    }, [editingUser, isOpen, form]);

    // Handle preset selection
    const applyPreset = (presetKey: string) => {
        form.setValue('preset', presetKey);
        let selectedPreset: EmployeePermissions = MEMBER_PERMISSIONS_PRESET;

        switch (presetKey) {
            case 'admin':
                selectedPreset = ADMIN_PERMISSIONS_PRESET;
                form.setValue('role', 'admin');
                break;
            case 'manager':
                selectedPreset = MANAGER_PERMISSIONS_PRESET;
                form.setValue('role', 'manager');
                break;
            case 'site_engineer':
                selectedPreset = SITE_ENGINEER_PRESET;
                form.setValue('role', 'member');
                break;
            case 'accountant':
                selectedPreset = ACCOUNTANT_PRESET;
                form.setValue('role', 'member');
                break;
            case 'member':
                selectedPreset = MEMBER_PERMISSIONS_PRESET;
                form.setValue('role', 'member');
                break;
            case 'custom':
                return;
        }

        form.setValue('permissions', selectedPreset);
    };

    // Handle primary role change
    const handleRoleChange = (newRole: 'admin' | 'manager' | 'member') => {
        form.setValue('role', newRole);
        if (newRole === 'admin') {
            form.setValue('preset', 'admin');
            form.setValue('permissions', ADMIN_PERMISSIONS_PRESET);
        } else if (newRole === 'manager') {
            form.setValue('preset', 'manager');
            form.setValue('permissions', MANAGER_PERMISSIONS_PRESET);
        } else {
            form.setValue('preset', 'member');
            form.setValue('permissions', MEMBER_PERMISSIONS_PRESET);
        }
    };

    const emailValue = form.watch('email');

    // Debounced check of email status
    useEffect(() => {
        if (isEditMode || !isOpen) {
            setEmailStatus('new');
            setExUserId(null);
            setEmailCheckMessage(null);
            return;
        }

        const email = form.getValues('email');
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email || !emailRegex.test(email)) {
            setEmailStatus('new');
            setExUserId(null);
            setEmailCheckMessage(null);
            return;
        }

        setEmailStatus('checking');

        const timer = setTimeout(async () => {
            try {
                const result = await checkEmailStatus(email);
                setEmailStatus(result.status);
                setExUserId(result.userId || null);
                setEmailCheckMessage(result.message || null);
            } catch (error) {
                console.error('Error checking email status:', error);
                setEmailStatus('new');
                setExUserId(null);
                setEmailCheckMessage(null);
            }
        }, 500);

        return () => clearTimeout(timer);
    }, [emailValue, isEditMode, isOpen, form]);

    async function onSubmit(values: AddEmployeeFormValues) {
        setIsSubmitting(true);
        try {
            if (isEditMode && editingUser) {
                const result = await updateEmployee(editingUser.id, {
                    role: values.role,
                    projectIds: values.projectIds,
                    permissions: values.permissions,
                });

                if (result.success) {
                    toast({
                        title: 'Employee Updated',
                        description: `${editingUser.displayName}'s permissions and profile updated.`,
                    });
                    form.reset();
                    onOpenChange(false);
                    onSuccess?.();
                } else {
                    toast({
                        title: 'Error',
                        description: result.error || 'Failed to update employee.',
                        variant: 'destructive',
                    });
                }
            } else if (emailStatus === 'ex' && exUserId) {
                const result = await rejoinEmployee(exUserId, {
                    role: values.role,
                    projectIds: values.projectIds,
                    permissions: values.permissions,
                });

                if (result.success) {
                    toast({
                        title: 'Employee Rejoined',
                        description: `${values.email} has been successfully rejoined to the company.`,
                    });
                    form.reset();
                    onOpenChange(false);
                    onSuccess?.();
                } else {
                    toast({
                        title: 'Error',
                        description: result.error || 'Failed to rejoin employee.',
                        variant: 'destructive',
                    });
                }
            } else {
                const result = await inviteEmployee(values);

                if (result.success) {
                    if (result.emailSent) {
                        toast({
                            title: 'Invitation Sent',
                            description: `An invitation with customized permissions has been sent to ${values.email}.`,
                        });
                    } else {
                        toast({
                            title: 'Invitation Created',
                            description: `Invite created with permissions, but email failed: ${result.emailError || 'Unknown error'}.`,
                            variant: 'destructive',
                        });
                    }
                    form.reset();
                    onOpenChange(false);
                    onSuccess?.();
                } else {
                    toast({
                        title: 'Error',
                        description: result.error || 'Failed to send invitation.',
                        variant: 'destructive',
                    });
                }
            }
        } catch (error) {
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive',
            });
        } finally {
            setIsSubmitting(false);
        }
    }

    const currentPermissions = form.watch('permissions');

    // Helper for category master toggling
    const toggleCategoryAll = (categoryKey: keyof EmployeePermissions, enableAll: boolean) => {
        const categoryObj = { ...form.getValues(`permissions.${categoryKey}`) };
        Object.keys(categoryObj).forEach(key => {
            (categoryObj as any)[key] = enableAll;
        });
        form.setValue(`permissions.${categoryKey}`, categoryObj as any);
        form.setValue('preset', 'custom');
    };

    const isCategoryAllActive = (categoryKey: keyof EmployeePermissions) => {
        const categoryObj = currentPermissions?.[categoryKey];
        if (!categoryObj) return false;
        return Object.values(categoryObj).every(val => val === true);
    };

    const countActivePermissions = useMemo(() => {
        if (!currentPermissions) return 0;
        let count = 0;
        Object.values(currentPermissions).forEach(category => {
            if (category && typeof category === 'object') {
                Object.values(category).forEach(val => {
                    if (val === true) count++;
                });
            }
        });
        return count;
    }, [currentPermissions]);

    const filteredProjects = projects.filter(p =>
        p.name.toLowerCase().includes(projectSearch.toLowerCase()) ||
        (p.client_name || p.clientName || '').toLowerCase().includes(projectSearch.toLowerCase())
    );

    const isSubmitDisabled = isSubmitting ||
        emailStatus === 'checking' ||
        (!isEditMode && (emailStatus === 'current' || emailStatus === 'other_company'));

    return (
        <Dialog open={isOpen} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-4xl w-[95vw] max-h-[90vh] flex flex-col p-0 overflow-hidden border border-border shadow-2xl rounded-2xl sm:rounded-3xl">
                <DialogHeader className="p-4 sm:p-6 pb-3 border-b bg-muted/30">
                    <div className="flex items-center gap-3">
                        <div className="p-2.5 rounded-xl bg-primary/10 text-primary">
                            {isEditMode ? <UserCheck className="h-6 w-6" /> : <UserPlus className="h-6 w-6" />}
                        </div>
                        <div>
                            <DialogTitle className="text-xl font-bold font-headline">
                                {isEditMode ? 'Edit Employee Controls & Access' : 'Invite New Employee & Customize Access'}
                            </DialogTitle>
                            <DialogDescription className="text-xs sm:text-sm text-muted-foreground mt-0.5">
                                {isEditMode
                                    ? `Fine-tune permissions, role, and project assignments for ${editingUser?.displayName}.`
                                    : 'Invite team members with role presets and granular action-level permission controls.'}
                            </DialogDescription>
                        </div>
                    </div>
                </DialogHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="flex-1 flex flex-col overflow-hidden">
                        <Tabs value={activeTab} onValueChange={(val: any) => setActiveTab(val)} className="flex-1 flex flex-col overflow-hidden">
                            <div className="px-6 pt-3 pb-0 border-b bg-muted/10 flex items-center justify-between gap-4 flex-wrap">
                                <TabsList className="grid grid-cols-2 w-full sm:w-auto h-9">
                                    <TabsTrigger value="details" className="text-xs sm:text-sm px-4">
                                        1. Member Details & Projects
                                    </TabsTrigger>
                                    <TabsTrigger value="permissions" className="text-xs sm:text-sm px-4 flex items-center gap-2">
                                        <span>2. Granular Permissions</span>
                                        <Badge variant="secondary" className="px-1.5 py-0 text-[10px] font-semibold">
                                            {countActivePermissions} active
                                        </Badge>
                                    </TabsTrigger>
                                </TabsList>

                                {/* Quick Preset Switcher Header */}
                                <div className="flex items-center gap-2 text-xs pb-2 sm:pb-0">
                                    <Sparkles className="h-3.5 w-3.5 text-amber-500" />
                                    <span className="text-muted-foreground font-medium hidden sm:inline">Permission Preset:</span>
                                    <Select
                                        value={form.watch('preset')}
                                        onValueChange={applyPreset}
                                    >
                                        <SelectTrigger className="h-8 text-xs w-[170px] bg-background">
                                            <SelectValue placeholder="Choose template" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="admin">Full Admin Template</SelectItem>
                                            <SelectItem value="manager">Project Manager</SelectItem>
                                            <SelectItem value="site_engineer">Site Engineer</SelectItem>
                                            <SelectItem value="accountant">Accountant / Finance</SelectItem>
                                            <SelectItem value="member">Standard Team Member</SelectItem>
                                            <SelectItem value="custom">Custom Configuration</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            <ScrollArea className="flex-1 p-4 sm:p-6 overflow-y-auto">
                                {/* TAB 1: DETAILS & PROJECTS */}
                                <TabsContent value="details" className="mt-0 space-y-6">
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        {/* Left Column: Email & Primary Role */}
                                        <div className="space-y-4">
                                            <FormField
                                                control={form.control}
                                                name="email"
                                                render={({ field }) => (
                                                    <FormItem>
                                                        <FormLabel className="text-xs font-semibold">Work Email Address</FormLabel>
                                                        <FormControl>
                                                            <Input
                                                                placeholder="employee@company.com"
                                                                {...field}
                                                                disabled={isEditMode}
                                                                className="h-9 text-sm"
                                                            />
                                                        </FormControl>
                                                        {isEditMode && (
                                                            <FormDescription className="text-[11px]">Email cannot be modified once registered.</FormDescription>
                                                        )}
                                                        {emailStatus === 'checking' && (
                                                            <div className="flex items-center text-xs text-muted-foreground mt-1">
                                                                <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                                                                Verifying email status...
                                                            </div>
                                                        )}
                                                        {!isEditMode && emailCheckMessage && (
                                                            <div className={`text-xs mt-1 font-medium p-2 rounded-md ${
                                                                emailStatus === 'current' || emailStatus === 'other_company'
                                                                    ? 'bg-red-500/10 text-red-600 dark:text-red-400 border border-red-500/20'
                                                                    : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20'
                                                            }`}>
                                                                {emailCheckMessage}
                                                            </div>
                                                        )}
                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />

                                            <FormField
                                                control={form.control}
                                                name="role"
                                                render={({ field }) => (
                                                    <FormItem>
                                                        <FormLabel className="text-xs font-semibold">Primary Administrative Role</FormLabel>
                                                        <Select onValueChange={(val: any) => handleRoleChange(val)} value={field.value}>
                                                            <FormControl>
                                                                <SelectTrigger className="h-9 text-sm">
                                                                    <SelectValue placeholder="Select primary role" />
                                                                </SelectTrigger>
                                                            </FormControl>
                                                            <SelectContent>
                                                                <SelectItem value="member">Member (Default Restricted Access)</SelectItem>
                                                                <SelectItem value="manager">Manager (Project Level Admin Access)</SelectItem>
                                                                <SelectItem value="admin">Admin (Full System Access)</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                        <FormDescription className="text-[11px]">
                                                            Role sets standard access tier. Fine-tune granular permissions in tab 2.
                                                        </FormDescription>
                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />

                                            <div className="p-3.5 rounded-xl border bg-muted/20 space-y-2">
                                                <div className="flex items-center justify-between text-xs font-semibold">
                                                    <span className="flex items-center gap-1.5">
                                                        <Shield className="h-4 w-4 text-primary" />
                                                        Current Permission Status
                                                    </span>
                                                    <Badge variant="outline" className="text-[11px] capitalize">
                                                        {form.watch('preset').replace('_', ' ')}
                                                    </Badge>
                                                </div>
                                                <p className="text-xs text-muted-foreground">
                                                    This employee will be assigned <strong className="text-foreground">{countActivePermissions} active permissions</strong> across company, project, financial, PO, worklog, and material modules.
                                                </p>
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => setActiveTab('permissions')}
                                                    className="text-xs text-primary h-7 px-2 p-0 hover:bg-transparent hover:underline"
                                                >
                                                    Customize individual controls →
                                                </Button>
                                            </div>
                                        </div>

                                        {/* Right Column: Assigned Projects */}
                                        <div className="space-y-3">
                                            <div className="flex items-center justify-between">
                                                <FormLabel className="text-xs font-semibold">Assign Construction Projects</FormLabel>
                                                <span className="text-[11px] text-muted-foreground">
                                                    {form.watch('projectIds')?.length || 0} selected
                                                </span>
                                            </div>

                                            <div className="relative">
                                                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                                                <Input
                                                    placeholder="Search projects..."
                                                    value={projectSearch}
                                                    onChange={e => setProjectSearch(e.target.value)}
                                                    className="pl-8 h-8 text-xs"
                                                />
                                            </div>

                                            <div className="border rounded-xl p-3 bg-background">
                                                <ScrollArea className="h-[220px]">
                                                    {filteredProjects.length > 0 ? (
                                                        <div className="space-y-1.5">
                                                            {filteredProjects.map((project) => (
                                                                <FormField
                                                                    key={project.id}
                                                                    control={form.control}
                                                                    name="projectIds"
                                                                    render={({ field }) => {
                                                                        const isChecked = field.value?.includes(project.id);
                                                                        return (
                                                                            <FormItem
                                                                                key={project.id}
                                                                                className={`flex flex-row items-center space-x-3 space-y-0 p-2 rounded-lg transition-colors cursor-pointer ${
                                                                                    isChecked ? 'bg-primary/10 border border-primary/20' : 'hover:bg-muted/50'
                                                                                }`}
                                                                                onClick={() => {
                                                                                    if (isChecked) {
                                                                                        field.onChange(field.value.filter(id => id !== project.id));
                                                                                    } else {
                                                                                        field.onChange([...field.value, project.id]);
                                                                                    }
                                                                                }}
                                                                            >
                                                                                <Checkbox
                                                                                    checked={isChecked}
                                                                                    onCheckedChange={(checked) => {
                                                                                        if (checked) {
                                                                                            field.onChange([...field.value, project.id]);
                                                                                        } else {
                                                                                            field.onChange(field.value.filter(id => id !== project.id));
                                                                                        }
                                                                                    }}
                                                                                />
                                                                                <div className="flex-1 min-w-0">
                                                                                    <div className="text-xs font-medium truncate">{project.name}</div>
                                                                                    <div className="text-[10px] text-muted-foreground truncate">
                                                                                        {project.client_name || project.clientName || 'General Project'}
                                                                                    </div>
                                                                                </div>
                                                                                <Badge variant={project.status === 'active' ? 'default' : 'secondary'} className="text-[9px] capitalize px-1.5 py-0">
                                                                                    {project.status}
                                                                                </Badge>
                                                                            </FormItem>
                                                                        );
                                                                    }}
                                                                />
                                                            ))}
                                                        </div>
                                                    ) : (
                                                        <div className="py-8 text-center text-xs text-muted-foreground">
                                                            No matching projects found.
                                                        </div>
                                                    )}
                                                </ScrollArea>
                                            </div>
                                        </div>
                                    </div>
                                </TabsContent>

                                {/* TAB 2: DETAILED PERMISSIONS MATRIX */}
                                <TabsContent value="permissions" className="mt-0 space-y-4">
                                    <div className="flex items-center justify-between pb-2 border-b">
                                        <div>
                                            <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
                                                <Shield className="h-4 w-4 text-primary" />
                                                Detailed Action Controls Matrix
                                            </h3>
                                            <p className="text-xs text-muted-foreground">
                                                Select exact permissions for this user across all system modules.
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <Button
                                                type="button"
                                                variant="outline"
                                                size="sm"
                                                className="text-xs h-7"
                                                onClick={() => {
                                                    form.setValue('permissions', ADMIN_PERMISSIONS_PRESET);
                                                    form.setValue('preset', 'custom');
                                                }}
                                            >
                                                Enable All
                                            </Button>
                                            <Button
                                                type="button"
                                                variant="outline"
                                                size="sm"
                                                className="text-xs h-7"
                                                onClick={() => {
                                                    form.setValue('permissions', DEFAULT_EMPTY_PERMISSIONS);
                                                    form.setValue('preset', 'custom');
                                                }}
                                            >
                                                Clear All
                                            </Button>
                                        </div>
                                    </div>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        {/* 1. COMPANY & ADMIN */}
                                        <PermissionCategoryCard
                                            icon={<Building2 className="h-4 w-4 text-blue-500" />}
                                            title="Company & Administration"
                                            categoryKey="company"
                                            form={form}
                                            isAllActive={isCategoryAllActive('company')}
                                            onToggleAll={(val) => toggleCategoryAll('company', val)}
                                            items={[
                                                { name: 'manageCompany', label: 'Manage Company Details', desc: 'Edit company profile, settings & info.' },
                                                { name: 'manageEmployees', label: 'Manage Employees', desc: 'Invite, edit roles, and manage team members.' },
                                                { name: 'viewFinancials', label: 'View Financial Dashboards', desc: 'Access overall company revenue & financial reports.' },
                                                { name: 'manageSalaries', label: 'Manage Salaries & Payouts', desc: 'View and update employee salary & bank info.' },
                                            ]}
                                        />

                                        {/* 2. PROJECTS MANAGEMENT */}
                                        <PermissionCategoryCard
                                            icon={<FolderKanban className="h-4 w-4 text-purple-500" />}
                                            title="Project Management"
                                            categoryKey="projects"
                                            form={form}
                                            isAllActive={isCategoryAllActive('projects')}
                                            onToggleAll={(val) => toggleCategoryAll('projects', val)}
                                            items={[
                                                { name: 'viewAllProjects', label: 'View All Projects', desc: 'Access all projects regardless of explicit assignment.' },
                                                { name: 'createProjects', label: 'Create Projects', desc: 'Draft and add new construction projects.' },
                                                { name: 'editProjects', label: 'Edit Project Details', desc: 'Modify status, dates, and client information.' },
                                                { name: 'deleteProjects', label: 'Delete Projects', desc: 'Delete or archive project records.' },
                                                { name: 'manageProjectMembers', label: 'Manage Team Members', desc: 'Assign and remove members from projects.' },
                                            ]}
                                        />

                                        {/* 3. EXPENSES & FINANCIALS */}
                                        <PermissionCategoryCard
                                            icon={<Receipt className="h-4 w-4 text-emerald-500" />}
                                            title="Financials & Expenses"
                                            categoryKey="expenses"
                                            form={form}
                                            isAllActive={isCategoryAllActive('expenses')}
                                            onToggleAll={(val) => toggleCategoryAll('expenses', val)}
                                            items={[
                                                { name: 'viewExpenses', label: 'View Project Expenses', desc: 'View logged site & project expense entries.' },
                                                { name: 'createExpenses', label: 'Create & Claim Expenses', desc: 'Submit new expense claims and upload receipts.' },
                                                { name: 'approveExpenses', label: 'Approve Expense Requests', desc: 'Review & approve employee expense claims.' },
                                                { name: 'deleteExpenses', label: 'Delete Expense Records', desc: 'Remove expense entries.' },
                                                { name: 'exportExpenses', label: 'Export Expense Data', desc: 'Download expense reports in CSV/Excel.' },
                                            ]}
                                        />

                                        {/* 4. INVENTORY & MATERIALS */}
                                        <PermissionCategoryCard
                                            icon={<Package className="h-4 w-4 text-amber-500" />}
                                            title="Inventory & Materials"
                                            categoryKey="inventory"
                                            form={form}
                                            isAllActive={isCategoryAllActive('inventory')}
                                            onToggleAll={(val) => toggleCategoryAll('inventory', val)}
                                            items={[
                                                { name: 'viewInventory', label: 'View Stock & Catalog', desc: 'View material quantities and unit pricing.' },
                                                { name: 'manageInventory', label: 'Manage Material Catalog', desc: 'Add, update, or remove inventory items.' },
                                                { name: 'logMaterialUsage', label: 'Log Material Usage', desc: 'Record consumed material items on project sites.' },
                                                { name: 'manageSuppliers', label: 'Manage Suppliers', desc: 'Maintain supplier directories & contacts.' },
                                                { name: 'materialEstimation', label: 'Run Material Estimation', desc: 'Access estimation calculations & prep tools.' },
                                            ]}
                                        />

                                        {/* 5. PURCHASE ORDERS */}
                                        <PermissionCategoryCard
                                            icon={<FileSpreadsheet className="h-4 w-4 text-indigo-500" />}
                                            title="Purchase Orders (POs)"
                                            categoryKey="purchaseOrders"
                                            form={form}
                                            isAllActive={isCategoryAllActive('purchaseOrders')}
                                            onToggleAll={(val) => toggleCategoryAll('purchaseOrders', val)}
                                            items={[
                                                { name: 'viewPurchaseOrders', label: 'View Purchase Orders', desc: 'See PO records and fulfillment status.' },
                                                { name: 'createPurchaseOrders', label: 'Create Purchase Orders', desc: 'Draft POs to send to suppliers.' },
                                                { name: 'approvePurchaseOrders', label: 'Approve POs', desc: 'Approve or reject proposed POs.' },
                                                { name: 'completePurchaseOrders', label: 'Complete POs', desc: 'Mark orders as received & fulfilled.' },
                                            ]}
                                        />

                                        {/* 6. WORK LOGS & SITE REPORTS */}
                                        <PermissionCategoryCard
                                            icon={<ClipboardList className="h-4 w-4 text-cyan-500" />}
                                            title="Daily Work Logs & Progress"
                                            categoryKey="worklogs"
                                            form={form}
                                            isAllActive={isCategoryAllActive('worklogs')}
                                            onToggleAll={(val) => toggleCategoryAll('worklogs', val)}
                                            items={[
                                                { name: 'viewWorklogs', label: 'View Daily Work Logs', desc: 'View daily site reports & progress updates.' },
                                                { name: 'createWorklogs', label: 'Submit Daily Logs', desc: 'Post daily site logs, worker counts, and photos.' },
                                                { name: 'approveWorklogs', label: 'Approve Work Logs', desc: 'Review & approve submitted site logs.' },
                                                { name: 'exportWorklogs', label: 'Export Log Summaries', desc: 'Download work log summaries for reports.' },
                                            ]}
                                        />

                                        {/* 7. POUCHES & FUND ALLOCATIONS */}
                                        <PermissionCategoryCard
                                            icon={<Wallet className="h-4 w-4 text-rose-500" />}
                                            title="Pouches & Petty Cash Funds"
                                            categoryKey="pouches"
                                            form={form}
                                            isAllActive={isCategoryAllActive('pouches')}
                                            onToggleAll={(val) => toggleCategoryAll('pouches', val)}
                                            items={[
                                                { name: 'viewPouches', label: 'View Pouch Balances', desc: 'View personal and project pouch balances.' },
                                                { name: 'requestPouchFunds', label: 'Request Fund Top-Ups', desc: 'Submit petty cash top-up requests.' },
                                                { name: 'approvePouchFunds', label: 'Approve & Allocate Funds', desc: 'Approve transfers into pouches.' },
                                            ]}
                                        />

                                        {/* 8. DOCUMENTS & BLUEPRINTS */}
                                        <PermissionCategoryCard
                                            icon={<FileText className="h-4 w-4 text-teal-500" />}
                                            title="Documents & Blueprints"
                                            categoryKey="documents"
                                            form={form}
                                            isAllActive={isCategoryAllActive('documents')}
                                            onToggleAll={(val) => toggleCategoryAll('documents', val)}
                                            items={[
                                                { name: 'viewDocuments', label: 'View Project Documents', desc: 'View contracts, blueprints, and uploaded files.' },
                                                { name: 'uploadDocuments', label: 'Upload New Documents', desc: 'Upload drawings, specifications, and files.' },
                                                { name: 'deleteDocuments', label: 'Delete Documents', desc: 'Remove document files from project storage.' },
                                            ]}
                                        />
                                    </div>
                                </TabsContent>
                            </ScrollArea>
                        </Tabs>

                        <DialogFooter className="p-4 sm:p-5 border-t bg-muted/20 flex-row items-center justify-between gap-3">
                            <div className="text-xs text-muted-foreground flex items-center gap-1.5">
                                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
                                <span>{countActivePermissions} active permission controls</span>
                            </div>

                            <div className="flex items-center gap-2">
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    onClick={() => onOpenChange(false)}
                                    className="h-9 text-xs"
                                >
                                    Cancel
                                </Button>
                                <Button
                                    type="submit"
                                    size="sm"
                                    disabled={isSubmitDisabled}
                                    className="h-9 text-xs font-semibold px-4"
                                >
                                    {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                    {isSubmitting
                                        ? (isEditMode ? 'Updating Employee...' : emailStatus === 'ex' ? 'Rejoining...' : 'Sending Invite...')
                                        : (isEditMode ? 'Save Permissions & Changes' : emailStatus === 'ex' ? 'Rejoin Employee' : 'Send Customized Invite')}
                                </Button>
                            </div>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}

interface PermissionCategoryCardProps {
    icon: React.ReactNode;
    title: string;
    categoryKey: keyof EmployeePermissions;
    form: any;
    isAllActive: boolean;
    onToggleAll: (checked: boolean) => void;
    items: { name: string; label: string; desc: string }[];
}

function PermissionCategoryCard({
    icon,
    title,
    categoryKey,
    form,
    isAllActive,
    onToggleAll,
    items
}: PermissionCategoryCardProps) {
    return (
        <div className="border rounded-xl p-3.5 bg-card hover:shadow-xs transition-shadow space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-border/50">
                <div className="flex items-center gap-2 font-semibold text-xs text-foreground">
                    {icon}
                    <span>{title}</span>
                </div>
                <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
                    <span>Select All</span>
                    <Switch
                        checked={isAllActive}
                        onCheckedChange={onToggleAll}
                        className="scale-75"
                    />
                </div>
            </div>

            <div className="grid grid-cols-1 gap-2">
                {items.map((item) => (
                    <FormField
                        key={item.name}
                        control={form.control}
                        name={`permissions.${categoryKey}.${item.name}`}
                        render={({ field }) => (
                            <FormItem className="flex flex-row items-start space-x-2.5 space-y-0 p-1.5 rounded-lg hover:bg-muted/40 transition-colors">
                                <FormControl className="mt-0.5">
                                    <Checkbox
                                        checked={field.value}
                                        onCheckedChange={(val) => {
                                            field.onChange(val);
                                            form.setValue('preset', 'custom');
                                        }}
                                    />
                                </FormControl>
                                <div className="space-y-0.5 leading-none cursor-pointer" onClick={() => {
                                    field.onChange(!field.value);
                                    form.setValue('preset', 'custom');
                                }}>
                                    <FormLabel className="text-xs font-medium cursor-pointer">{item.label}</FormLabel>
                                    <FormDescription className="text-[10px] text-muted-foreground leading-tight">
                                        {item.desc}
                                    </FormDescription>
                                </div>
                            </FormItem>
                        )}
                    />
                ))}
            </div>
        </div>
    );
}
