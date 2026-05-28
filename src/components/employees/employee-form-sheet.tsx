
'use client';

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Check, ChevronsUpDown, Loader2 } from 'lucide-react';
import { useSupabase } from '@/supabase/provider';
import { useToast } from '@/hooks/use-toast';
import { useEffect, useState } from 'react';
import { type Project, type User as AppUser } from '@/lib/data';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Popover, PopoverContent, PopoverTrigger } from '../ui/popover';
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem } from '../ui/command';
import { cn } from '@/lib/utils';
import { Badge } from '../ui/badge';

const formSchema = z.object({
  displayName: z.string().min(2, 'Name must be at least 2 characters.'),
  email: z.string().email('Please enter a valid email.'),
  role: z.enum(['admin', 'manager', 'member']),
  projectIds: z.array(z.string()).optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface EmployeeFormSheetProps {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  editingUser: AppUser | null;
  companyId: string;
  projects: Project[];
}

export function EmployeeFormSheet({ isOpen, setIsOpen, editingUser, companyId, projects }: EmployeeFormSheetProps) {
  const { supabase } = useSupabase();
  const { toast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      displayName: '',
      email: '',
      role: 'member',
      projectIds: [],
    },
  });

  useEffect(() => {
    if (editingUser) {
      form.reset({
        displayName: editingUser.displayName,
        email: editingUser.email,
        role: editingUser.role,
        projectIds: editingUser.projectIds || [],
      });
    } else {
      form.reset({
        displayName: '',
        email: '',
        role: 'member',
        projectIds: [],
      });
    }
  }, [editingUser, isOpen, form]);

  const onSubmit = async (values: FormValues) => {
    setIsSubmitting(true);
    try {
      if (editingUser) {
        // Update existing user
        const { error: userError } = await supabase
            .from('users')
            .update({
                displayName: values.displayName,
                role: values.role,
                projectIds: values.projectIds,
             })
            .eq('id', editingUser.id);
        
        if (userError) throw userError;

        // In a real app you might want to handle project assignments more robustly,
        // but for now we just update the user's projectIds.

        toast({ title: 'Employee Updated', description: 'The employee details have been successfully updated.' });
      } else {
        // This is a simplified invite. A real implementation would generate a unique token.
        // For now, we are creating an 'invites' record. A user with this email
        // will be associated with this company when they sign up.
        const { error } = await supabase.from('invites').insert({
          email: values.email,
          role: values.role,
          projectIds: values.projectIds || [],
          companyId: companyId,
          status: 'pending',
        });
        
        if (error) throw error;
        
        toast({ title: 'Invite Sent', description: `An invitation has been sent to ${values.email}.` });
      }
      setIsOpen(false);
    } catch (error: any) {
      toast({ title: 'Error Saving Employee', description: error.message, variant: 'destructive' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetContent className="sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{editingUser ? 'Edit Employee' : 'Invite New Employee'}</SheetTitle>
          <SheetDescription>
            {editingUser ? 'Update the details for this employee.' : 'Invite a new member to your company by email.'}
          </SheetDescription>
        </SheetHeader>
        <div className="py-4">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <FormField
                control={form.control}
                name="displayName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Full Name</FormLabel>
                    <FormControl><Input placeholder="e.g., John Doe" {...field} /></FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl><Input type="email" placeholder="e.g., john@example.com" {...field} disabled={!!editingUser} /></FormControl>
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
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl><SelectTrigger><SelectValue placeholder="Select a role" /></SelectTrigger></FormControl>
                      <SelectContent>
                        <SelectItem value="admin">Admin</SelectItem>
                        <SelectItem value="manager">Project Manager</SelectItem>
                        <SelectItem value="member">Member</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
               <FormField
                control={form.control}
                name="projectIds"
                render={({ field }) => (
                    <FormItem className="flex flex-col">
                        <FormLabel>Assigned Projects</FormLabel>
                        <Popover>
                            <PopoverTrigger asChild>
                                <FormControl>
                                    <Button
                                        variant="outline"
                                        role="combobox"
                                        className={cn(
                                            "w-full justify-between h-auto min-h-10",
                                            !field.value?.length && "text-muted-foreground"
                                        )}
                                    >
                                        <div className="flex flex-wrap gap-1">
                                            {field.value && field.value.length > 0 ? field.value.map(projectId => (
                                                <Badge variant="secondary" key={projectId}>{projects.find(p => p.id === projectId)?.name}</Badge>
                                            )) : "Select projects"}
                                        </div>
                                        <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                                    </Button>
                                </FormControl>
                            </PopoverTrigger>
                            <PopoverContent className="w-[300px] p-0">
                                <Command>
                                    <CommandInput placeholder="Search projects..." />
                                    <CommandEmpty>No projects found.</CommandEmpty>
                                    <CommandGroup>
                                        {projects.map((project) => (
                                            <CommandItem
                                                value={project.id}
                                                key={project.id}
                                                onSelect={() => {
                                                    const selected = field.value || [];
                                                    const newValue = selected.includes(project.id)
                                                        ? selected.filter((id) => id !== project.id)
                                                        : [...selected, project.id];
                                                    field.onChange(newValue);
                                                }}
                                            >
                                                <Check
                                                    className={cn(
                                                        "mr-2 h-4 w-4",
                                                        (field.value || []).includes(project.id)
                                                            ? "opacity-100"
                                                            : "opacity-0"
                                                    )}
                                                />
                                                {project.name}
                                            </CommandItem>
                                        ))}
                                    </CommandGroup>
                                </Command>
                            </PopoverContent>
                        </Popover>
                        <FormMessage />
                    </FormItem>
                )}
              />
              <SheetFooter className="pt-6">
                <Button variant="outline" type="button" onClick={() => setIsOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  {editingUser ? 'Save Changes' : 'Send Invite'}
                </Button>
              </SheetFooter>
            </form>
          </Form>
        </div>
      </SheetContent>
    </Sheet>
  );
}
