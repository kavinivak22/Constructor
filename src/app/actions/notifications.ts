'use server';

import { createClient } from '@/utils/supabase/server';
import { cookies } from 'next/headers';

export async function createNotification(
    userId: string,
    title: string,
    message: string,
    type: 'info' | 'warning' | 'success' | 'error' = 'info',
    link?: string,
    options?: {
        category?: string;
        icon?: string;
        project_id?: string;
    }
) {
    const supabase = await createClient();

    try {
        const { error } = await supabase
            .from('notifications')
            .insert({
                user_id: userId,
                title,
                body: message,   // DB column is "body"
                message,         // synced via trigger to body
                type,
                link: link || null,
                read: false,
                ...(options?.category && { category: options.category }),
                ...(options?.icon && { icon: options.icon }),
                ...(options?.project_id && { project_id: options.project_id }),
            });

        if (error) throw error;

        return { success: true };
    } catch (error: any) {
        console.error('Error creating notification:', error);
        return { success: false, error: error.message };
    }
}
