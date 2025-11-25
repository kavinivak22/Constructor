'use client';

import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Loader2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@/components/ui/sheet';
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
import { useToast } from '@/hooks/use-toast';
import { inviteEmployee, updateEmployee } from '@/app/actions/employees';
import { Project, type User as AppUser } from '@/lib/data';

const formSchema = z.object({
    email: z.string().email({ message: 'Invalid email address' }),
    role: z.enum(['admin', 'manager', 'member']),
    projectIds: z.array(z.string()).default([]),
    permissions: z.object({
        company: z.object({
            manageCompany: z.boolean().default(false),
            manageEmployees: z.boolean().default(false),
            viewFinancials: z.boolean().default(false),
        }),
        projects: z.object({
            createProjects: z.boolean().default(false),
            deleteProjects: z.boolean().default(false),
            manageProjectSettings: z.boolean().default(false),
        }),
    }),
});

interface AddEmployeeSheetProps {
    isOpen: boolean;
    onOpenChange: (open: boolean) => void;
    projects: Project[];
    onSuccess?: () => void;
    editingUser?: AppUser | null;
}

export function AddEmployeeSheet({
    isOpen,
    onOpenChange,
    projects,
    onSuccess,
    editingUser = null,
}: AddEmployeeSheetProps) {
    const { toast } = useToast();
    const [isSubmitting, setIsSubmitting] = useState(false);

    const isEditMode = !!editingUser;

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            email: '',
            role: 'member',
            projectIds: [],
            permissions: {
                company: {
                    manageCompany: false,
                    manageEmployees: false,
                    viewFinancials: false,
                },
                projects: {
                    createProjects: false,
                    deleteProjects: false,
                    manageProjectSettings: false,
                },
            },
        },
    });

    // Update form when editingUser changes
    useEffect(() => {
        if (editingUser) {
            form.reset({
                email: editingUser.email,
                role: editingUser.role,
                projectIds: editingUser.projectIds || [],
                permissions: editingUser.permissions || {
                    company: {
                        manageCompany: false,
                        manageEmployees: false,
                        viewFinancials: false,
                    },
                    projects: {
                        createProjects: false,
                        deleteProjects: false,
                        manageProjectSettings: false,
                    },
                },
            });
        } else {
            form.reset({
                email: '',
                role: 'member',
                projectIds: [],
                permissions: {
                    company: {
                        manageCompany: false,
                        manageEmployees: false,
                        viewFinancials: false,
                    },
                    projects: {
                        createProjects: false,
                        deleteProjects: false,
                        manageProjectSettings: false,
                    },
                },
            });
        }
    }, [editingUser, form, isOpen]);

    async function onSubmit(values: z.infer<typeof formSchema>) {
        setIsSubmitting(true);
        try {
            if (isEditMode && editingUser) {
                // Update existing employee
                const result = await updateEmployee(editingUser.id, {
                    role: values.role,
                    projectIds: values.projectIds,
                    permissions: values.permissions,
                });

                if (result.success) {
                    toast({
                        title: 'Employee Updated',
                        description: `${editingUser.displayName}'s details have been updated.`,
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
            } else {
                // Invite new employee
                const result = await inviteEmployee(values);

                if (result.success) {
                    if (result.emailSent) {
                        toast({
                            title: 'Invitation Sent',
                            description: `An invitation has been sent to ${values.email}.`,
                        });
                    } else {
                        toast({
                            title: 'Invitation Created',
                            description: `Invite created, but email failed: ${result.emailError || 'Unknown error'}. Please check logs.`,
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

    return (
        <Sheet open={isOpen} onOpenChange={onOpenChange}>
            <SheetContent className="sm:max-w-[540px] overflow-y-auto">
                <SheetHeader>
                    <SheetTitle>{isEditMode ? 'Edit Employee' : 'Add New Employee'}</SheetTitle>
                    <SheetDescription>
                        {isEditMode
                            ? 'Update the employee\'s role, projects, and permissions.'
                            : 'Invite a new member to your company. They will receive an email to join.'}
                    </SheetDescription>
                </SheetHeader>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 py-6">
                        <FormField
                            control={form.control}
                            name="email"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Email Address</FormLabel>
                                    <FormControl>
                                        <Input
                                            placeholder="colleague@example.com"
                                            {...field}
                                            disabled={isEditMode}
                                        />
                                    </FormControl>
                                    {isEditMode && (
                                        <FormDescription>Email cannot be changed</FormDescription>
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
                                    <FormLabel>Role</FormLabel>
                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select a role" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="member">Member</SelectItem>
                                            <SelectItem value="manager">Manager</SelectItem>
                                            <SelectItem value="admin">Admin</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormDescription>
                                        Roles define the default access level. You can customize permissions below.
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="projectIds"
                            render={() => (
                                <FormItem>
                                    <FormLabel>Assign Projects</FormLabel>
                                    <div className="border rounded-md p-4">
                                        <ScrollArea className="h-[150px]">
                                            {projects.length > 0 ? (
                                                <div className="space-y-2">
                                                    {projects.map((project) => (
                                                        <FormField
                                                            key={project.id}
                                                            control={form.control}
                                                            name="projectIds"
                                                            render={({ field }) => {
                                                                return (
                                                                    <FormItem
                                                                        key={project.id}
                                                                        className="flex flex-row items-start space-x-3 space-y-0"
                                                                    >
                                                                        <FormControl>
                                                                            <Checkbox
                                                                                checked={field.value?.includes(project.id)}
                                                                                onCheckedChange={(checked) => {
                                                                                    return checked
                                                                                        ? field.onChange([...field.value, project.id])
                                                                                        : field.onChange(
                                                                                            field.value?.filter(
                                                                                                (value) => value !== project.id
                                                                                            )
                                                                                        );
                                                                                }}
                                                                            />
                                                                        </FormControl>
                                                                        <FormLabel className="font-normal cursor-pointer">
                                                                            {project.name}
                                                                        </FormLabel>
                                                                    </FormItem>
                                                                );
                                                            }}
                                                        />
                                                    ))}
                                                </div>
                                            ) : (
                                                <p className="text-sm text-muted-foreground">No projects available.</p>
                                            )}
                                        </ScrollArea>
                                    </div>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <div className="space-y-4">
                            <h3 className="text-sm font-medium">Permissions</h3>

                            <div className="border rounded-md p-4 space-y-4">
                                <h4 className="text-sm font-semibold text-muted-foreground mb-2">Company Level</h4>
                                <div className="grid grid-cols-1 gap-2">
                                    <FormField
                                        control={form.control}
                                        name="permissions.company.manageCompany"
                                        render={({ field }) => (
                                            <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                                                <FormControl>
                                                    <Checkbox
                                                        checked={field.value}
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormControl>
                                                <div className="space-y-1 leading-none">
                                                    <FormLabel>Manage Company Details</FormLabel>
                                                    <FormDescription>
                                                        Edit company profile, address, etc.
                                                    </FormDescription>
                                                </div>
                                            </FormItem>
                                        )}
                                    />
                                    <FormField
                                        control={form.control}
                                        name="permissions.company.manageEmployees"
                                        render={({ field }) => (
                                            <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                                                <FormControl>
                                                    <Checkbox
                                                        checked={field.value}
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormControl>
                                                <div className="space-y-1 leading-none">
                                                    <FormLabel>Manage Employees</FormLabel>
                                                    <FormDescription>
                                                        Invite, edit, and remove employees.
                                                    </FormDescription>
                                                </div>
                                            </FormItem>
                                        )}
                                    />
                                    <FormField
                                        control={form.control}
                                        name="permissions.company.viewFinancials"
                                        render={({ field }) => (
                                            <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                                                <FormControl>
                                                    <Checkbox
                                                        checked={field.value}
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormControl>
                                                <div className="space-y-1 leading-none">
                                                    <FormLabel>View Financials</FormLabel>
                                                    <FormDescription>
                                                        Access to budget and expense reports.
                                                    </FormDescription>
                                                </div>
                                            </FormItem>
                                        )}
                                    />
                                </div>

                                <div className="my-4 border-t" />

                                <h4 className="text-sm font-semibold text-muted-foreground mb-2">Project Level</h4>
                                <div className="grid grid-cols-1 gap-2">
                                    <FormField
                                        control={form.control}
                                        name="permissions.projects.createProjects"
                                        render={({ field }) => (
                                            <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                                                <FormControl>
                                                    <Checkbox
                                                        checked={field.value}
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormControl>
                                                <div className="space-y-1 leading-none">
                                                    <FormLabel>Create Projects</FormLabel>
                                                </div>
                                            </FormItem>
                                        )}
                                    />
                                    <FormField
                                        control={form.control}
                                        name="permissions.projects.deleteProjects"
                                        render={({ field }) => (
                                            <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                                                <FormControl>
                                                    <Checkbox
                                                        checked={field.value}
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormControl>
                                                <div className="space-y-1 leading-none">
                                                    <FormLabel>Delete Projects</FormLabel>
                                                </div>
                                            </FormItem>
                                        )}
                                    />
                                    <FormField
                                        control={form.control}
                                        name="permissions.projects.manageProjectSettings"
                                        render={({ field }) => (
                                            <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                                                <FormControl>
                                                    <Checkbox
                                                        checked={field.value}
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormControl>
                                                <div className="space-y-1 leading-none">
                                                    <FormLabel>Manage Project Settings</FormLabel>
                                                </div>
                                            </FormItem>
                                        )}
                                    />
                                </div>
                            </div>
                        </div>

                        <SheetFooter>
                            <Button type="submit" disabled={isSubmitting}>
                                {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                {isSubmitting
                                    ? (isEditMode ? 'Updating...' : 'Sending Invite...')
                                    : (isEditMode ? 'Update Employee' : 'Send Invitation')}
                            </Button>
                        </SheetFooter>
                    </form>
                </Form>
            </SheetContent>
        </Sheet>
    );
}
