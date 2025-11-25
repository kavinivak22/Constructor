'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSupabase } from '@/supabase/provider';
import { queryKeys } from '@/lib/react-query';
import type { Project } from '@/lib/data';
import { useToast } from '@/hooks/use-toast';
import { projectCreateSchema, projectUpdateSchema, type ProjectCreateInput, type ProjectUpdateInput } from '@/lib/validations/project';

// Hook to fetch all user's projects
export function useProjects() {
    const { supabase, user } = useSupabase();

    return useQuery({
        queryKey: queryKeys.projects,
        queryFn: async () => {
            if (!user) return [];

            // Fetch user's project IDs
            const { data: userData, error: userError } = await supabase
                .from('users')
                .select('projectIds')
                .eq('id', user.id)
                .single();

            if (userError) throw userError;

            const projectIds = userData?.projectIds || [];
            if (projectIds.length === 0) return [];

            // Fetch projects using the IDs
            const { data, error } = await supabase
                .from('projects')
                .select('*')
                .in('id', projectIds);

            if (error) throw error;
            return data as Project[];
        },
        enabled: !!user,
    });
}

// Hook to fetch a single project by ID
export function useProject(projectId: string | undefined) {
    const { supabase, user } = useSupabase();

    return useQuery({
        queryKey: queryKeys.project(projectId || ''),
        queryFn: async () => {
            if (!user || !projectId) return null;

            // Check user has access to this project
            const { data: userData, error: userError } = await supabase
                .from('users')
                .select('projectIds')
                .eq('id', user.id)
                .single();

            if (userError) throw userError;

            const userProjectIds = userData?.projectIds || [];
            if (!userProjectIds.includes(projectId)) {
                throw new Error('You do not have access to this project');
            }

            // Fetch the project
            const { data, error } = await supabase
                .from('projects')
                .select('*')
                .eq('id', projectId)
                .single();

            if (error) throw error;
            return data as Project;
        },
        enabled: !!user && !!projectId,
        staleTime: 10 * 60 * 1000, // 10 minutes for individual projects
    });
}

// Hook to create a new project
export function useCreateProject() {
    const { supabase, user } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async (input: ProjectCreateInput) => {
            if (!user) throw new Error('User not authenticated');

            // Validate input
            const validated = projectCreateSchema.parse(input);

            // Create project
            const { data, error } = await supabase
                .from('projects')
                .insert([validated])
                .select()
                .single();

            if (error) throw error;

            // Add project to user's projectIds
            const { data: userData } = await supabase
                .from('users')
                .select('projectIds')
                .eq('id', user.id)
                .single();

            const currentProjectIds = userData?.projectIds || [];
            await supabase
                .from('users')
                .update({ projectIds: [...currentProjectIds, data.id] })
                .eq('id', user.id);

            return data as Project;
        },
        onSuccess: (newProject) => {
            // Invalidate and refetch projects list
            queryClient.invalidateQueries({ queryKey: queryKeys.projects });

            toast({
                title: 'Project created',
                description: `${newProject.name} has been created successfully.`,
            });
        },
        onError: (error: Error) => {
            toast({
                title: 'Error creating project',
                description: error.message,
                variant: 'destructive',
            });
        },
    });
}

// Hook to update an existing project
export function useUpdateProject() {
    const { supabase } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async (input: ProjectUpdateInput) => {
            // Validate input
            const validated = projectUpdateSchema.parse(input);
            const { id, ...updateData } = validated;

            // Update project
            const { data, error } = await supabase
                .from('projects')
                .update(updateData)
                .eq('id', id)
                .select()
                .single();

            if (error) throw error;
            return data as Project;
        },
        onMutate: async (input) => {
            // Cancel outgoing refetches
            await queryClient.cancelQueries({ queryKey: queryKeys.project(input.id!) });

            // Snapshot the previous value
            const previousProject = queryClient.getQueryData(queryKeys.project(input.id!));

            // Optimistically update the cache
            queryClient.setQueryData(queryKeys.project(input.id!), (old: Project | undefined) => {
                if (!old) return old;
                return { ...old, ...input };
            });

            return { previousProject };
        },
        onError: (error: Error, _variables, context) => {
            // Rollback on error
            if (context?.previousProject) {
                queryClient.setQueryData(
                    queryKeys.project(_variables.id!),
                    context.previousProject
                );
            }

            toast({
                title: 'Error updating project',
                description: error.message,
                variant: 'destructive',
            });
        },
        onSuccess: (updatedProject) => {
            // Invalidate to ensure fresh data
            queryClient.invalidateQueries({ queryKey: queryKeys.projects });
            queryClient.invalidateQueries({ queryKey: queryKeys.project(updatedProject.id) });

            toast({
                title: 'Project updated',
                description: `${updatedProject.name} has been updated successfully.`,
            });
        },
    });
}

// Hook to delete a project
export function useDeleteProject() {
    const { supabase, user } = useSupabase();
    const queryClient = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: async (projectId: string) => {
            if (!user) throw new Error('User not authenticated');

            // Delete project
            const { error } = await supabase
                .from('projects')
                .delete()
                .eq('id', projectId);

            if (error) throw error;

            // Remove from user's projectIds
            const { data: userData } = await supabase
                .from('users')
                .select('projectIds')
                .eq('id', user.id)
                .single();

            const currentProjectIds = userData?.projectIds || [];
            await supabase
                .from('users')
                .update({ projectIds: currentProjectIds.filter((id: string) => id !== projectId) })
                .eq('id', user.id);

            return projectId;
        },
        onSuccess: (projectId) => {
            // Remove from cache
            queryClient.removeQueries({ queryKey: queryKeys.project(projectId) });
            queryClient.invalidateQueries({ queryKey: queryKeys.projects });

            toast({
                title: 'Project deleted',
                description: 'The project has been deleted successfully.',
            });
        },
        onError: (error: Error) => {
            toast({
                title: 'Error deleting project',
                description: error.message,
                variant: 'destructive',
            });
        },
    });
}

// Hook to fetch project members count
export function useProjectMembersCount(projectId: string | undefined) {
    const { supabase } = useSupabase();

    return useQuery({
        queryKey: queryKeys.projectMembers(projectId || ''),
        queryFn: async () => {
            if (!projectId) return 0;

            const { count } = await supabase
                .from('users')
                .select('*', { count: 'exact', head: true })
                .contains('projectIds', [projectId]);

            return count || 0;
        },
        enabled: !!projectId,
    });
}
