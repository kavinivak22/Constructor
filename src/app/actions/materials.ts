'use server'

import { createServerActionClient } from '@supabase/auth-helpers-nextjs'
import { cookies } from 'next/headers'

export async function getProjectMaterials(projectId: string) {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data, error } = await supabase
            .from('project_materials')
            .select('*')
            .eq('project_id', projectId)
            .order('name', { ascending: true })

        if (error) throw error

        return { success: true, data }
    } catch (error: any) {
        console.error('Error fetching project materials:', error)
        return { success: false, error: error.message }
    }
}
