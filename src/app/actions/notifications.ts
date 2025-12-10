'use server';

import { createServerActionClient } from '@supabase/auth-helpers-nextjs';
import { cookies } from 'next/headers';

export async function createNotification(
    userId: string,
    title: string,
    message: string,
    type: 'info' | 'warning' | 'success' | 'error' = 'info',
    link?: string
) {
    const supabase = createServerActionClient({ cookies });

    try {
        const { error } = await supabase
            .from('notifications')
            .insert({
                user_id: userId,
                title,
                message,
                type,
                link,
                read: false,
            });

        if (error) throw error;

        return { success: true };
    } catch (error: any) {
        console.error('Error creating notification:', error);
        return { success: false, error: error.message };
    }
}
