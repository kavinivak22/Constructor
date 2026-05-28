'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSupabase } from '@/supabase/provider';
import { queryKeys } from '@/lib/react-query';
import { useToast } from '@/hooks/use-toast';

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
                  user:user_id (
                    id,
                    displayName:display_name,
                    email,
                    photoURL:photo_url
                  )
                `)
                .eq('project_id', projectId)
                .order('expense_date', { ascending: false });

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
        mutationFn: async (input: {
            project_id: string;
            amount: number;
            category: string;
            description: string;
            expense_date: string;
            payment_status?: 'pending' | 'paid' | 'overdue' | 'cancelled';
            notes?: string;
            receiver?: string;
            receipt_url?: string;
        }) => {
            if (!user) throw new Error('User not authenticated');

            const { data, error } = await supabase
                .from('expenses')
                .insert([{
                    ...input,
                    user_id: user.id,
                    created_by: user.id,
                    payment_status: input.payment_status || 'pending',
                }])
                .select()
                .single();

            if (error) throw error;
            return data;
        },
        onSuccess: (_newExpense, variables) => {
            queryClient.invalidateQueries({
                queryKey: queryKeys.projectExpenses(variables.project_id),
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
        mutationFn: async (input: {
            id: string;
            project_id?: string;
            amount?: number;
            category?: string;
            description?: string;
            expense_date?: string;
            payment_status?: 'pending' | 'paid' | 'overdue' | 'cancelled';
            notes?: string;
            receiver?: string;
            receipt_url?: string;
        }) => {
            const { id, ...updateData } = input;

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
            if (updatedExpense.project_id) {
                queryClient.invalidateQueries({
                    queryKey: queryKeys.projectExpenses(updatedExpense.project_id),
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
                .select('amount, category, payment_status')
                .eq('project_id', projectId);

            if (error) throw error;

            const total = data.reduce((sum, expense) => sum + (expense.amount || 0), 0);
            const byCategory = data.reduce((acc, expense) => {
                const category = expense.category || 'other';
                acc[category] = (acc[category] || 0) + (expense.amount || 0);
                return acc;
            }, {} as Record<string, number>);

            const paid = data
                .filter((e) => e.payment_status === 'paid')
                .reduce((sum, expense) => sum + (expense.amount || 0), 0);

            const pending = data
                .filter((e) => e.payment_status === 'pending')
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
