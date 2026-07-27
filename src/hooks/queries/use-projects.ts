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

            // Fetch user's project IDs from project_members table
            const { data: membersData, error: membersError } = await supabase
                .from('project_members')
                .select('project_id')
                .eq('user_id', user.id);

            if (membersError) throw membersError;

            const projectIds = membersData?.map(m => m.project_id) || [];
            if (projectIds.length === 0) return [];

            // Fetch projects using the IDs
            const { data: projectsData, error } = await supabase
                .from('projects')
                .select('*')
                .in('id', projectIds);

            if (error) throw error;
            if (!projectsData || projectsData.length === 0) return [];

            // Fetch latest worklog timestamps for each project to sort by recent updates
            const { data: worklogsData } = await supabase
                .from('daily_worklogs')
                .select('project_id, date, created_at')
                .in('project_id', projectIds)
                .order('created_at', { ascending: false });

            const latestLogMap = new Map<string, number>();
            if (worklogsData) {
                for (const log of worklogsData) {
                    if (log.project_id && !latestLogMap.has(log.project_id)) {
                        const logTime = new Date(log.created_at || log.date).getTime();
                        latestLogMap.set(log.project_id, logTime);
                    }
                }
            }

            // Sort projects: projects recently updated with worklogs show up first
            const sortedProjects = [...projectsData].sort((a: any, b: any) => {
                const timeA = latestLogMap.has(a.id)
                    ? latestLogMap.get(a.id)!
                    : new Date(a.created_at || a.start_date || 0).getTime();
                const timeB = latestLogMap.has(b.id)
                    ? latestLogMap.get(b.id)!
                    : new Date(b.created_at || b.start_date || 0).getTime();

                return timeB - timeA;
            });

            return sortedProjects as Project[];
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

            // Check user has access to this project via project_members
            const { data: memberData, error: memberError } = await supabase
                .from('project_members')
                .select('project_id')
                .eq('user_id', user.id)
                .eq('project_id', projectId)
                .maybeSingle();

            if (memberError) throw memberError;
            if (!memberData) {
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

            const mapStatus = (status: string) => {
                if (status === 'planning' || status === 'in-progress') return 'active';
                if (status === 'completed') return 'completed';
                return 'on-hold';
            };

            const dbPayload = {
                name: validated.name,
                description: validated.description,
                start_date: validated.startDate || null,
                end_date: validated.endDate || null,
                status: mapStatus(validated.status || 'planning'),
                progress: validated.progress,
                budget: validated.budget,
                client_name: validated.clientName || null,
                client_contact: validated.clientContact || null,
                location: validated.location || null,
                project_type: validated.projectType || null,
                company_id: validated.companyId,
                created_by: user.id
            };

            // Create project
            const { data, error } = await supabase
                .from('projects')
                .insert([dbPayload])
                .select()
                .single();

            if (error) throw error;

            // Add project to user's project_members
            const { error: memberError } = await supabase
                .from('project_members')
                .insert({
                    project_id: data.id,
                    user_id: user.id,
                    role: 'admin'
                });

            if (memberError) throw memberError;

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

            const mapStatus = (status: string) => {
                if (status === 'planning' || status === 'in-progress') return 'active';
                if (status === 'completed') return 'completed';
                return 'on-hold';
            };

            const dbUpdatePayload: Record<string, any> = {};
            if (updateData.name !== undefined) dbUpdatePayload.name = updateData.name;
            if (updateData.description !== undefined) dbUpdatePayload.description = updateData.description;
            if (updateData.startDate !== undefined) dbUpdatePayload.start_date = updateData.startDate;
            if (updateData.endDate !== undefined) dbUpdatePayload.end_date = updateData.endDate;
            if (updateData.status !== undefined) dbUpdatePayload.status = mapStatus(updateData.status);
            if (updateData.progress !== undefined) dbUpdatePayload.progress = updateData.progress;
            if (updateData.budget !== undefined) dbUpdatePayload.budget = updateData.budget;
            if (updateData.clientName !== undefined) dbUpdatePayload.client_name = updateData.clientName;
            if (updateData.clientContact !== undefined) dbUpdatePayload.client_contact = updateData.clientContact;
            if (updateData.location !== undefined) dbUpdatePayload.location = updateData.location;
            if (updateData.projectType !== undefined) dbUpdatePayload.project_type = updateData.projectType;
            if (updateData.companyLogo !== undefined) dbUpdatePayload.thumbnail_url = updateData.companyLogo;

            // Update project
            const { data, error } = await supabase
                .from('projects')
                .update(dbUpdatePayload)
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
                .from('project_members')
                .select('*', { count: 'exact', head: true })
                .eq('project_id', projectId);

            return count || 0;
        },
        enabled: !!projectId,
    });
}
