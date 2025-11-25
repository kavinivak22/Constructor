'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSupabase } from '@/supabase/provider';
import { queryKeys } from '@/lib/react-query';
import { useToast } from '@/hooks/use-toast';
import {
    expenseCreateSchema,
    expenseUpdateSchema,
    type ExpenseCreateInput,
    type ExpenseUpdateInput,
} from '@/lib/validations/expense';

// Hook to fetch expenses for a project
export function useExpenses(projectId: string | undefined) {
    const { supabase } = useSupabase();

    return useQuery({
        queryKey: queryKeys.projectExpenses(projectId || ''),
        queryFn: async () => {
            if (!projectId) return [];

            const { data, error } = await supabase
                .from('expenses')
                .select(`
          *,
          user:userId (
            id,
            displayName,
            email
          )
        `)
                .eq('projectId', projectId)
                .order('date', { ascending: false });

            if (error) throw error;
            return data;
        },
        enabled: !!projectId,
    });
}

// Hook to create a new expense
export function useCreateExpense() {
    const { supabase, user } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async (input: ExpenseCreateInput) => {
            if (!user) throw new Error('User not authenticated');

            // Validate input
            const validated = expenseCreateSchema.parse({
                ...input,
                userId: user.id,
            });

            // Create expense
            const { data, error } = await supabase
                .from('expenses')
                .insert([validated])
                .select()
                .single();

            if (error) throw error;
            return data;
        },
        onSuccess: (_newExpense, variables) => {
            // Invalidate project expenses to refetch with new expense
            queryClient.invalidateQueries({
                queryKey: queryKeys.projectExpenses(variables.projectId),
            });

            toast({
                title: 'Expense added',
                description: 'The expense has been added successfully.',
            });
        },
        onError: (error: Error) => {
            toast({
                title: 'Error adding expense',
                description: error.message,
                variant: 'destructive',
            });
        },
    });
}

// Hook to update an existing expense
export function useUpdateExpense() {
    const { supabase } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async (input: ExpenseUpdateInput) => {
            // Validate input
            const validated = expenseUpdateSchema.parse(input);
            const { id, ...updateData } = validated;

            // Update expense
            const { data, error } = await supabase
                .from('expenses')
                .update(updateData)
                .eq('id', id)
                .select()
                .single();

            if (error) throw error;
            return data;
        },
        onSuccess: (updatedExpense) => {
            // Invalidate project expenses
            if (updatedExpense.projectId) {
                queryClient.invalidateQueries({
                    queryKey: queryKeys.projectExpenses(updatedExpense.projectId),
                });
            }

            toast({
                title: 'Expense updated',
                description: 'The expense has been updated successfully.',
            });
        },
        onError: (error: Error) => {
            toast({
                title: 'Error updating expense',
                description: error.message,
                variant: 'destructive',
            });
        },
    });
}

// Hook to delete an expense
export function useDeleteExpense() {
    const { supabase } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async ({ id, projectId }: { id: string; projectId: string }) => {
            const { error } = await supabase.from('expenses').delete().eq('id', id);

            if (error) throw error;
            return { id, projectId };
        },
        onSuccess: ({ projectId }) => {
            queryClient.invalidateQueries({
                queryKey: queryKeys.projectExpenses(projectId),
            });

            toast({
                title: 'Expense deleted',
                description: 'The expense has been deleted successfully.',
            });
        },
        onError: (error: Error) => {
            toast({
                title: 'Error deleting expense',
                description: error.message,
                variant: 'destructive',
            });
        },
    });
}

// Hook to get expense statistics for a project
export function useExpenseStats(projectId: string | undefined) {
    const { supabase } = useSupabase();

    return useQuery({
        queryKey: [...queryKeys.projectExpenses(projectId || ''), 'stats'],
        queryFn: async () => {
            if (!projectId) return null;

            const { data, error } = await supabase
                .from('expenses')
                .select('amount, category, paymentStatus')
                .eq('projectId', projectId);

            if (error) throw error;

            const total = data.reduce((sum, expense) => sum + (expense.amount || 0), 0);
            const byCategory = data.reduce((acc, expense) => {
                const category = expense.category || 'other';
                acc[category] = (acc[category] || 0) + (expense.amount || 0);
                return acc;
            }, {} as Record<string, number>);

            const paid = data
                .filter((e) => e.paymentStatus === 'paid')
                .reduce((sum, expense) => sum + (expense.amount || 0), 0);

            const pending = data
                .filter((e) => e.paymentStatus === 'pending')
                .reduce((sum, expense) => sum + (expense.amount || 0), 0);

            return {
                total,
                byCategory,
                paid,
                pending,
                count: data.length,
            };
        },
        enabled: !!projectId,
    });
}
