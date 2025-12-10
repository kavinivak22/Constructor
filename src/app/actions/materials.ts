'use server'

import { createServerActionClient } from '@supabase/auth-helpers-nextjs'
import { cookies } from 'next/headers'
import { createNotification } from './notifications'

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

export async function updateMaterialStock(materialId: string, newQuantity: number, purpose: string) {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // 1. Get current material details
        const { data: material, error: fetchError } = await supabase
            .from('project_materials')
            .select('*, projects(name, userIds, companyId)')
            .eq('id', materialId)
            .single()

        if (fetchError || !material) throw new Error('Material not found')

        const currentQuantity = Number(material.quantity)
        const changeAmount = newQuantity - currentQuantity

        // 2. Update the quantity
        const { error: updateError } = await supabase
            .from('project_materials')
            .update({ quantity: newQuantity })
            .eq('id', materialId)

        if (updateError) throw updateError

        // 3. Log the change
        const { error: logError } = await supabase
            .from('material_logs')
            .insert({
                material_id: materialId,
                user_id: user.id,
                change_amount: changeAmount,
                purpose: purpose
            })

        if (logError) {
            console.error('Error creating material log:', logError)
            // We don't throw here to avoid rolling back the stock update, but ideally we should use a transaction.
            // For now, we'll just log the error.
        }

        // 4. Check for low stock
        if (newQuantity <= material.min_quantity) {
            await createNotification(
                user.id,
                'Low Stock Alert',
                `Stock for ${material.name} in project ${material.projects?.name} is low (${newQuantity} ${material.unit}).`,
                'warning',
                `/projects/${material.project_id}/materials`
            )
        }

        return { success: true }
    } catch (error: any) {
        console.error('Error updating material stock:', error)
        return { success: false, error: error.message }
    }
}

export async function getMaterialLogs(materialId: string) {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data, error } = await supabase
            .from('material_logs')
            .select('*, users(displayName, email)')
            .eq('material_id', materialId)
            .order('created_at', { ascending: false })

        if (error) throw error

        return { success: true, data }
    } catch (error: any) {
        console.error('Error fetching material logs:', error)
        return { success: false, error: error.message }
    }
}
