'use server'

import { createClient } from '@/utils/supabase/server';
import { cookies } from 'next/headers'

export type TaskItem = {
    id: string;
    project_id: string | null;
    title: string;
    description: string | null;
    assigned_to: string | null;
    status: string;
    priority: string | null;
    due_date: string | null;
    is_upcoming: boolean;
    duration: string | null;
    created_by: string | null;
    created_at: string | null;
    assigned_user?: {
        display_name: string;
        email: string;
    } | null;
}

export async function getPersonalTasks() {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data, error } = await supabase
            .from('tasks')
            .select('*')
            .is('project_id', null)
            .eq('created_by', user.id)
            .order('created_at', { ascending: false });

        if (error) throw error;
        return data as TaskItem[];
    } catch (error) {
        console.error('Error fetching personal tasks:', error)
        return [];
    }
}

export async function createPersonalTask(title: string) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data, error } = await supabase
            .from('tasks')
            .insert({
                title,
                status: 'pending',
                project_id: null,
                is_upcoming: false,
                created_by: user.id
            })
            .select()
            .single();

        if (error) throw error;
        return { success: true, task: data };
    } catch (error: any) {
        console.error('Error creating personal task:', error)
        return { success: false, error: error.message };
    }
}

export async function toggleTaskStatus(taskId: string, currentStatus: string) {
    const supabase = await createClient();

    try {
        const newStatus = currentStatus === 'completed' ? 'pending' : 'completed';
        const completedAt = newStatus === 'completed' ? new Date().toISOString() : null;

        const { data, error } = await supabase
            .from('tasks')
            .update({
                status: newStatus,
                completed_at: completedAt,
                updated_at: new Date().toISOString()
            })
            .eq('id', taskId)
            .select()
            .single();

        if (error) throw error;
        return { success: true, task: data };
    } catch (error: any) {
        console.error('Error toggling task status:', error)
        return { success: false, error: error.message };
    }
}

export async function getProjectTasks(projectId: string) {
    const supabase = await createClient();

    try {
        const { data, error } = await supabase
            .from('tasks')
            .select('*, users(display_name, email)')
            .eq('project_id', projectId)
            .eq('is_upcoming', false)
            .order('created_at', { ascending: false });

        if (error) throw error;

        // Map assigned_to user details
        return (data || []).map((item: any) => ({
            ...item,
            assigned_user: item.users ? {
                display_name: item.users.display_name,
                email: item.users.email
            } : null
        })) as TaskItem[];
    } catch (error) {
        console.error('Error fetching project tasks:', error)
        return [];
    }
}

export async function createProjectTask(data: {
    projectId: string;
    title: string;
    assignedTo?: string;
    dueDate?: string;
    priority?: string;
}) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: createdTask, error } = await supabase
            .from('tasks')
            .insert({
                project_id: data.projectId,
                title: data.title,
                assigned_to: data.assignedTo || null,
                due_date: data.dueDate || null,
                priority: data.priority || 'medium',
                status: 'pending',
                is_upcoming: false,
                created_by: user.id
            })
            .select()
            .single();

        if (error) throw error;
        return { success: true, task: createdTask };
    } catch (error: any) {
        console.error('Error creating project task:', error)
        return { success: false, error: error.message };
    }
}

export async function getUpcomingWorks(projectId: string) {
    const supabase = await createClient();

    try {
        const { data, error } = await supabase
            .from('tasks')
            .select('*')
            .eq('project_id', projectId)
            .eq('is_upcoming', true)
            .order('due_date', { ascending: true });

        if (error) throw error;
        return data as TaskItem[];
    } catch (error) {
        console.error('Error fetching upcoming works:', error)
        return [];
    }
}

export async function createUpcomingWork(data: {
    projectId: string;
    title: string;
    dueDate: string;
    duration: string;
    priority: string;
}) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: createdWork, error } = await supabase
            .from('tasks')
            .insert({
                project_id: data.projectId,
                title: data.title,
                due_date: data.dueDate,
                duration: data.duration,
                priority: data.priority,
                status: 'pending',
                is_upcoming: true,
                created_by: user.id
            })
            .select()
            .single();

        if (error) throw error;
        return { success: true, work: createdWork };
    } catch (error: any) {
        console.error('Error creating upcoming work:', error)
        return { success: false, error: error.message };
    }
}

export async function deleteTaskAction(taskId: string) {
    const supabase = await createClient();

    try {
        const { error } = await supabase
            .from('tasks')
            .delete()
            .eq('id', taskId);

        if (error) throw error;
        return { success: true };
    } catch (error: any) {
        console.error('Error deleting task:', error);
        return { success: false, error: error.message };
    }
}

export async function editTaskAction(taskId: string, data: {
    title: string;
    assignedTo?: string | null;
    dueDate?: string | null;
    priority?: string;
}) {
    const supabase = await createClient();

    try {
        const { data: updatedTask, error } = await supabase
            .from('tasks')
            .update({
                title: data.title,
                assigned_to: data.assignedTo || null,
                due_date: data.dueDate || null,
                priority: data.priority || 'medium',
                updated_at: new Date().toISOString()
            })
            .eq('id', taskId)
            .select()
            .single();

        if (error) throw error;
        return { success: true, task: updatedTask };
    } catch (error: any) {
        console.error('Error editing task:', error);
        return { success: false, error: error.message };
    }
}

