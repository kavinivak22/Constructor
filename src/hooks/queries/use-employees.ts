'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSupabase } from '@/supabase/provider';
import { queryKeys } from '@/lib/react-query';
import { useToast } from '@/hooks/use-toast';
import {
    employeeInviteSchema,
    employeeUpdateSchema,
    type EmployeeInviteInput,
    type EmployeeUpdateInput,
} from '@/lib/validations/employee';

// Hook to fetch all employees in the company
export function useEmployees() {
    const { supabase, user } = useSupabase();

    return useQuery({
        queryKey: queryKeys.employees,
        queryFn: async () => {
            if (!user) return [];

            // Get user's company ID
            const { data: userData, error: userError } = await supabase
                .from('users')
                .select('companyId')
                .eq('id', user.id)
                .single();

            if (userError) throw userError;

            const companyId = userData?.companyId;
            if (!companyId) return [];

            // Fetch all employees in the company
            const { data, error } = await supabase
                .from('users')
                .select('*')
                .eq('companyId', companyId);

            if (error) throw error;
            return data;
        },
        enabled: !!user,
    });
}

// Hook to fetch current user's profile
export function useUserProfile() {
    const { supabase, user } = useSupabase();

    return useQuery({
        queryKey: queryKeys.userProfile,
        queryFn: async () => {
            if (!user) return null;

            const { data, error } = await supabase
                .from('users')
                .select('*')
                .eq('id', user.id)
                .single();

            if (error) throw error;
            return data;
        },
        enabled: !!user,
        staleTime: 15 * 60 * 1000, // 15 minutes for user profile
    });
}

// Hook to invite a new employee
export function useInviteEmployee() {
    const { supabase, user } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async (input: EmployeeInviteInput) => {
            if (!user) throw new Error('User not authenticated');

            // Validate input
            const validated = employeeInviteSchema.parse(input);

            // Check if user is admin
            const { data: userData } = await supabase
                .from('users')
                .select('role')
                .eq('id', user.id)
                .single();

            if (userData?.role !== 'admin') {
                throw new Error('Only admins can invite employees');
            }

            // Create invite
            const { data, error } = await supabase
                .from('invites')
                .insert([validated])
                .select()
                .single();

            if (error) throw error;
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.invites });

            toast({
                title: 'Invitation sent',
                description: 'The employee invitation has been sent successfully.',
            });
        },
        onError: (error: Error) => {
            toast({
                title: 'Error sending invitation',
                description: error.message,
                variant: 'destructive',
            });
        },
    });
}

// Hook to update employee details
export function useUpdateEmployee() {
    const { supabase, user } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async (input: EmployeeUpdateInput) => {
            if (!user) throw new Error('User not authenticated');

            // Validate input
            const validated = employeeUpdateSchema.parse(input);
            const { id, ...updateData } = validated;

            // Check if user is admin or updating their own profile
            const { data: currentUser } = await supabase
                .from('users')
                .select('role')
                .eq('id', user.id)
                .single();

            if (currentUser?.role !== 'admin' && id !== user.id) {
                throw new Error('You do not have permission to update this employee');
            }

            // Update employee
            const { data, error } = await supabase
                .from('users')
                .update(updateData)
                .eq('id', id)
                .select()
                .single();

            if (error) throw error;
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.employees });
            queryClient.invalidateQueries({ queryKey: queryKeys.userProfile });

            toast({
                title: 'Employee updated',
                description: 'Employee details have been updated successfully.',
            });
        },
        onError: (error: Error) => {
            toast({
                title: 'Error updating employee',
                description: error.message,
                variant: 'destructive',
            });
        },
    });
}

// Hook to fetch all invites (admin only)
export function useInvites() {
    const { supabase, user } = useSupabase();

    return useQuery({
        queryKey: queryKeys.invites,
        queryFn: async () => {
            if (!user) return [];

            // Get user's company ID and check role
            const { data: userData, error: userError } = await supabase
                .from('users')
                .select('companyId, role')
                .eq('id', user.id)
                .single();

            if (userError) throw userError;

            const { companyId, role } = userData;
            if (!companyId || role !== 'admin') return [];

            // Fetch all invites for the company
            const { data, error } = await supabase
                .from('invites')
                .select('*')
                .eq('companyId', companyId);

            if (error) throw error;
            return data;
        },
        enabled: !!user,
    });
}
