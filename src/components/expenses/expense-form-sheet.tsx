
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
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Calendar } from '@/components/ui/calendar';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { CalendarIcon, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { useSupabase } from '@/supabase/provider';
import { useToast } from '@/hooks/use-toast';
import { useEffect, useState } from 'react';
import { type Expense, type Project } from '@/lib/data';

const formSchema = z.object({
  projectId: z.string().optional(),
  amount: z.preprocess(
    (a) => parseFloat(z.string().parse(a)),
    z.number().positive('Amount must be a positive number.')
  ),
  category: z.string().nonempty('Please select a category.'),
  description: z.string().min(3, 'Description must be at least 3 characters.'),
  expenseDate: z.date({ required_error: 'An expense date is required.' }),
}).refine((data) => data.projectId || data.projectId === '', {
  message: "Project is required",
  path: ["projectId"],
});

interface ExpenseFormSheetProps {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  projectId?: string;
  expense?: Expense | null;
  onSuccess?: () => void;
}

export function ExpenseFormSheet({ isOpen, setIsOpen, projectId, expense, onSuccess }: ExpenseFormSheetProps) {
  const { supabase, user } = useSupabase();
  const { toast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoadingProjects, setIsLoadingProjects] = useState(false);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      projectId: projectId || '',
      amount: 0,
      category: '',
      description: '',
      expenseDate: new Date(),
    },
  });

  // Fetch projects if projectId is not provided
  useEffect(() => {
    const fetchProjects = async () => {
      if (!user || projectId) return;

      setIsLoadingProjects(true);
      try {
        const { data: userData, error: userError } = await supabase
          .from('users')
          .select('projectIds')
          .eq('id', user.id)
          .single();

        if (userError) throw userError;

        const projectIds = userData?.projectIds || [];

        if (projectIds.length === 0) {
          setProjects([]);
          return;
        }

        const { data, error } = await supabase
          .from('projects')
          .select('*')
          .in('id', projectIds);

        if (error) throw error;
        setProjects(data || []);

        // Auto-select if only one project
        if (data && data.length === 1) {
          form.setValue('projectId', data[0].id);
        }

      } catch (error) {
        console.error('Error fetching projects:', error);
      } finally {
        setIsLoadingProjects(false);
      }
    };

    if (isOpen && !projectId) {
      fetchProjects();
    }
  }, [isOpen, projectId, user, supabase, form]);

  useEffect(() => {
    if (expense) {
      form.reset({
        projectId: projectId || expense.projectId, // Use prop or expense's projectId
        amount: expense.amount,
        category: expense.category,
        description: expense.description,
        expenseDate: new Date(expense.expenseDate as any),
      });
    } else {
      form.reset({
        projectId: projectId || '',
        amount: 0,
        category: '',
        description: '',
        expenseDate: new Date(),
      });
    }
  }, [expense, form, projectId, isOpen]);

  async function onSubmit(values: z.infer<typeof formSchema>) {
    const targetProjectId = projectId || values.projectId;

    if (!user || !targetProjectId) {
      toast({ title: 'Error', description: 'Project and User are required.', variant: 'destructive' });
      return;
    }

    setIsSubmitting(true);

    try {
      const expenseData = {
        projectId: targetProjectId,
        userId: user.id,
        amount: values.amount,
        category: values.category,
        description: values.description,
        expenseDate: values.expenseDate.toISOString(),
      };

      if (expense) {
        // Update existing expense
        const { error } = await supabase
          .from('expenses')
          .update(expenseData)
          .eq('id', expense.id);

        if (error) throw error;
        toast({ title: 'Expense Updated', description: 'The expense has been successfully updated.' });
      } else {
        // Create new expense
        const { error } = await supabase
          .from('expenses')
          .insert({
            ...expenseData,
            createdAt: new Date().toISOString(),
          });

        if (error) throw error;
        toast({ title: 'Expense Logged', description: 'The new expense has been added.' });
      }

      setIsOpen(false);
      if (onSuccess) onSuccess();
    } catch (error: any) {
      console.error('Error saving expense:', error);
      toast({ title: 'Error Saving Expense', description: error.message || 'Failed to save expense.', variant: 'destructive' });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetContent className="w-full sm:max-w-md flex flex-col h-full">
        <SheetHeader>
          <SheetTitle>{expense ? 'Edit Expense' : 'Log New Expense'}</SheetTitle>
          <SheetDescription>
            {expense ? 'Update the details of your expense.' : 'Add a new expense.'}
          </SheetDescription>
        </SheetHeader>
        <div className="flex-1 overflow-y-auto py-4 -mx-6 px-6">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">

              {/* Project Selection (Only if projectId prop is missing) */}
              {!projectId && (
                <FormField
                  control={form.control}
                  name="projectId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Project</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value} value={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder={isLoadingProjects ? "Loading..." : "Select a project"} />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {projects.map((project) => (
                            <SelectItem key={project.id} value={project.id}>
                              {project.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              <FormField
                control={form.control}
                name="amount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Amount (₹)</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-muted-foreground">₹</span>
                        <Input type="number" placeholder="0.00" className="pl-8" {...field} />
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="category"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Category</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a category" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="materials">Materials</SelectItem>
                        <SelectItem value="labor">Labor</SelectItem>
                        <SelectItem value="equipment">Equipment Rental</SelectItem>
                        <SelectItem value="permits">Permits & Fees</SelectItem>
                        <SelectItem value="subcontractor">Subcontractor</SelectItem>
                        <SelectItem value="other">Other</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="expenseDate"
                render={({ field }) => (
                  <FormItem className="flex flex-col">
                    <FormLabel>Date of Expense</FormLabel>
                    <Popover>
                      <PopoverTrigger asChild>
                        <FormControl>
                          <Button
                            variant={'outline'}
                            className={cn(
                              'pl-3 text-left font-normal',
                              !field.value && 'text-muted-foreground'
                            )}
                          >
                            {field.value ? format(field.value, 'PPP') : <span>Pick a date</span>}
                            <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                          </Button>
                        </FormControl>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0" align="start">
                        <Calendar
                          mode="single"
                          selected={field.value}
                          onSelect={field.onChange}
                          initialFocus
                        />
                      </PopoverContent>
                    </Popover>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Description</FormLabel>
                    <FormControl>
                      <Textarea placeholder="e.g., 50 bags of cement from Supplier A" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <SheetFooter className="pt-4">
                <Button variant="outline" type="button" onClick={() => setIsOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  {expense ? 'Save Changes' : 'Log Expense'}
                </Button>
              </SheetFooter>
            </form>
          </Form>
        </div>
      </SheetContent>
    </Sheet>
  );
}
