'use server'

import { createServerActionClient } from '@supabase/auth-helpers-nextjs'
import { cookies } from 'next/headers'
import { z } from 'zod'

// --- Schemas ---

const workerCountSchema = z.object({
    workerType: z.string().min(1, "Worker type is required"),
    count: z.number().min(0, "Count must be non-negative"),
})

const laborEntrySchema = z.object({
    contractorName: z.string().min(1, "Contractor name is required"),
    category: z.string().optional(),
    workDescription: z.string().optional(),
    paymentStatus: z.enum(['Paid', 'On Payday', 'Pending']).default('Pending'),
    workers: z.array(workerCountSchema).min(1, "At least one worker type is required"),
})

const materialEntrySchema = z.object({
    projectMaterialId: z.string().optional(), // Optional if it's an ad-hoc material
    materialName: z.string().min(1, "Material name is required"),
    quantityConsumed: z.number().min(0, "Quantity must be non-negative"),
    unit: z.string().optional(),
})

const photoEntrySchema = z.object({
    photoUrl: z.string().url(),
    caption: z.string().optional(),
})

const createWorklogSchema = z.object({
    projectId: z.string().uuid(),
    date: z.string().refine((val) => !isNaN(Date.parse(val)), "Invalid date"),
    labor: z.array(laborEntrySchema),
    materials: z.array(materialEntrySchema),
    photos: z.array(photoEntrySchema),
})

export type WorklogData = z.infer<typeof createWorklogSchema>

// --- Actions ---

export async function createWorklog(data: WorklogData) {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    const validated = createWorklogSchema.safeParse(data)

    if (!validated.success) {
        return {
            success: false,
            error: 'Invalid data',
            fieldErrors: validated.error.flatten().fieldErrors,
        }
    }

    const { projectId, date, labor, materials, photos } = validated.data

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // 1. Create Daily Worklog Entry
        const { data: worklog, error: worklogError } = await supabase
            .from('daily_worklogs')
            .insert({
                project_id: projectId,
                date: date,
                created_by: user.id,
            })
            .select()
            .single()

        if (worklogError) throw worklogError

        const worklogId = worklog.id

        // 2. Insert Labor Entries and Worker Counts
        for (const entry of labor) {
            const { data: laborEntry, error: laborError } = await supabase
                .from('worklog_labor_entries')
                .insert({
                    worklog_id: worklogId,
                    contractor_name: entry.contractorName,
                    category: entry.category,
                    work_description: entry.workDescription,
                    payment_status: entry.paymentStatus,
                })
                .select()
                .single()

            if (laborError) throw laborError

            const workerCounts = entry.workers.map(w => ({
                labor_entry_id: laborEntry.id,
                worker_type: w.workerType,
                count: w.count,
            }))

            if (workerCounts.length > 0) {
                const { error: countsError } = await supabase
                    .from('worklog_worker_counts')
                    .insert(workerCounts)

                if (countsError) throw countsError
            }
        }

        // 3. Insert Materials
        // Note: The DB trigger `deduct_material_inventory` will handle inventory updates
        const materialRecords = materials.map(m => ({
            worklog_id: worklogId,
            project_material_id: m.projectMaterialId || null,
            material_name: m.materialName,
            quantity_consumed: m.quantityConsumed,
            unit: m.unit,
        }))

        if (materialRecords.length > 0) {
            const { error: materialError } = await supabase
                .from('worklog_materials')
                .insert(materialRecords)

            if (materialError) throw materialError
        }

        // 4. Insert Photos
        const photoRecords = photos.map(p => ({
            worklog_id: worklogId,
            photo_url: p.photoUrl,
            caption: p.caption,
        }))

        if (photoRecords.length > 0) {
            const { error: photoError } = await supabase
                .from('worklog_photos')
                .insert(photoRecords)

            if (photoError) throw photoError
        }

        return { success: true, worklogId }

    } catch (error: any) {
        console.error('Error creating worklog:', error)
        // In a real production app, we might want to attempt a rollback here by deleting the worklogId
        return { success: false, error: error.message || 'Failed to create worklog' }
    }
}

export async function getWorklogs(projectId: string) {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data, error } = await supabase
            .from('daily_worklogs')
            .select(`
        *,
        labor:worklog_labor_entries (
          *,
          workers:worklog_worker_counts (*)
        ),
        materials:worklog_materials (*),
        photos:worklog_photos (*)
      `)
            .eq('project_id', projectId)
            .order('date', { ascending: false })

        if (error) throw error

        return { success: true, data }
    } catch (error: any) {
        console.error('Error fetching worklogs:', error)
        return { success: false, error: error.message }
    }
}
