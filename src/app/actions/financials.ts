'use server'

import { createClient } from '@/utils/supabase/server'
import { cookies } from 'next/headers'
import { revalidatePath } from 'next/cache'

// ==========================================
// Salary Profiles Actions
// ==========================================

export async function getSalaryProfiles() {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single()

        if (!userProfile?.company_id) return []

        const { data: profiles, error } = await supabase
            .from('salary_profiles')
            .select('*, users:user_id(display_name, email, role)')
            .eq('company_id', userProfile.company_id)
            .order('created_at', { ascending: false })

        if (error) throw error
        return profiles || []
    } catch (error) {
        console.error('Error fetching salary profiles:', error)
        return []
    }
}

export async function saveSalaryProfile(data: {
    id?: string
    user_id?: string | null
    worker_name?: string | null
    payment_type: 'monthly' | 'daily_wage' | 'hourly'
    rate: number
    bank_name?: string
    account_number?: string
    ifsc_code?: string
}) {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('company_id, role')
            .eq('id', user.id)
            .single()

        if (!userProfile?.company_id || userProfile.role !== 'admin') {
            throw new Error('Only admins can manage salary profiles')
        }

        const payload = {
            company_id: userProfile.company_id,
            user_id: data.user_id || null,
            worker_name: data.worker_name || null,
            payment_type: data.payment_type,
            rate: data.rate,
            bank_name: data.bank_name || null,
            account_number: data.account_number || null,
            ifsc_code: data.ifsc_code || null,
            updated_at: new Date().toISOString()
        }

        if (data.id) {
            const { error } = await supabase
                .from('salary_profiles')
                .update(payload)
                .eq('id', data.id)
            if (error) throw error
        } else {
            const { error } = await supabase
                .from('salary_profiles')
                .insert({
                    ...payload,
                    created_at: new Date().toISOString()
                })
            if (error) throw error
        }

        revalidatePath('/financials/salary-profiles')
        return { success: true }
    } catch (error: any) {
        console.error('Error saving salary profile:', error)
        return { success: false, error: error.message }
    }
}

export async function deleteSalaryProfile(id: string) {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('role')
            .eq('id', user.id)
            .single()

        if (userProfile?.role !== 'admin') {
            throw new Error('Only admins can delete salary profiles')
        }

        const { error } = await supabase
            .from('salary_profiles')
            .delete()
            .eq('id', id)

        if (error) throw error

        revalidatePath('/financials/salary-profiles')
        return { success: true }
    } catch (error: any) {
        console.error('Error deleting salary profile:', error)
        return { success: false, error: error.message }
    }
}

// ==========================================
// Weekly Payouts Actions
// ==========================================

export async function getWeeklyPayouts() {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single()

        if (!userProfile?.company_id) return []

        const { data: payouts, error } = await supabase
            .from('weekly_payouts')
            .select('*, creator:created_by(display_name)')
            .eq('company_id', userProfile.company_id)
            .order('week_start_date', { ascending: false })

        if (error) throw error
        return payouts || []
    } catch (error) {
        console.error('Error fetching weekly payouts:', error)
        return []
    }
}

export async function getPayoutItems(payoutId: string) {
    const supabase = await createClient()

    try {
        const { data: items, error } = await supabase
            .from('payout_items')
            .select('*, project:project_id(name)')
            .eq('payout_id', payoutId)
            .order('created_at', { ascending: true })

        if (error) throw error
        return items || []
    } catch (error) {
        console.error('Error fetching payout items:', error)
        return []
    }
}

export async function createWeeklyPayoutRun(weekStartDate: string, weekEndDate: string) {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('company_id, role')
            .eq('id', user.id)
            .single()

        if (!userProfile?.company_id || userProfile.role !== 'admin') {
            throw new Error('Only admins can create weekly payout runs')
        }

        const companyId = userProfile.company_id

        // 1. Create the Weekly Payout header
        const { data: payout, error: payoutError } = await supabase
            .from('weekly_payouts')
            .insert({
                company_id: companyId,
                week_start_date: weekStartDate,
                week_end_date: weekEndDate,
                status: 'draft',
                total_amount: 0,
                created_by: user.id,
                created_at: new Date().toISOString()
            })
            .select()
            .single()

        if (payoutError) {
            if (payoutError.message.includes('unique_week_per_company')) {
                throw new Error('A payout run already exists for this week date range.')
            }
            throw payoutError
        }

        const payoutId = payout.id
        const payoutItems: any[] = []

        // 2. Fetch all active salary profiles
        const { data: profiles } = await supabase
            .from('salary_profiles')
            .select('*, users:user_id(display_name)')
            .eq('company_id', companyId)

        // 3. Fetch daily labor log entries for attendance calculation
        // Find daily worklogs in range
        const { data: worklogs } = await supabase
            .from('daily_worklogs')
            .select('id, project_id')
            .gte('date', weekStartDate)
            .lte('date', weekEndDate)

        const worklogIds = worklogs?.map(w => w.id) || []
        let laborEntries: any[] = []

        if (worklogIds.length > 0) {
            const { data: lEntries } = await supabase
                .from('worklog_labor_entries')
                .select('*')
                .in('worklog_id', worklogIds)
            laborEntries = lEntries || []
        }

        // 4. Auto-generate payout items for Salary / Wage profiles
        if (profiles) {
            for (const profile of profiles) {
                const recipientName = profile.worker_name || profile.users?.display_name || 'Worker'
                let amountDue = 0
                let details = ''

                if (profile.payment_type === 'monthly') {
                    // For monthly salary, default to monthly rate (can be adjusted)
                    amountDue = Number(profile.rate)
                    details = 'Monthly Salary Rate'
                } else if (profile.payment_type === 'daily_wage') {
                    // Count days worked in the daily worklogs
                    const nameToMatch = (profile.worker_name || profile.users?.display_name || '').toLowerCase().trim()
                    const daysWorked = laborEntries.filter(entry => {
                        const contractor = (entry.contractor_name || '').toLowerCase().trim()
                        return contractor.includes(nameToMatch) || nameToMatch.includes(contractor)
                    }).length

                    amountDue = daysWorked * Number(profile.rate)
                    details = `Daily Wage: ${daysWorked} days worked @ ₹${profile.rate}/day`
                } else {
                    amountDue = Number(profile.rate)
                    details = 'Hourly Wage'
                }

                // Find a project ID from worklogs to link this labor expense to (optional)
                const linkedWorklog = worklogs?.find(w => 
                    laborEntries.some(le => le.worklog_id === w.id && le.contractor_name?.toLowerCase().includes(recipientName.toLowerCase()))
                )

                payoutItems.push({
                    payout_id: payoutId,
                    recipient_type: profile.payment_type === 'monthly' ? 'employee_salary' : 'labor_wage',
                    recipient_id: profile.user_id,
                    recipient_name: recipientName,
                    amount_due: amountDue,
                    amount_paid: amountDue,
                    status: 'pending',
                    project_id: linkedWorklog?.project_id || null,
                    reference_details: details,
                    notes: `Bank: ${profile.bank_name || 'N/A'}, Acc: ${profile.account_number || 'N/A'}`
                })
            }
        }

        // 5. Fetch approved/delivered Purchase Orders in the date range
        const { data: pos } = await supabase
            .from('purchase_orders')
            .select('*')
            .eq('company_id', companyId)
            .in('status', ['approved', 'delivered'])
            .gte('created_at', weekStartDate)
            .lte('created_at', weekEndDate)

        if (pos) {
            for (const po of pos) {
                payoutItems.push({
                    payout_id: payoutId,
                    recipient_type: 'vendor_payment',
                    recipient_id: po.id,
                    recipient_name: po.supplier_name,
                    amount_due: Number(po.total_amount),
                    amount_paid: Number(po.total_amount),
                    status: 'pending',
                    project_id: po.project_id,
                    reference_details: `PO Number: ${po.po_number || 'N/A'}`,
                    notes: `Contact: ${po.supplier_contact || 'N/A'}`
                })
            }
        }

        // 6. Insert all generated payout items
        if (payoutItems.length > 0) {
            const { error: itemsError } = await supabase
                .from('payout_items')
                .insert(payoutItems)

            if (itemsError) throw itemsError

            // Update total sum of payout run
            const totalSum = payoutItems.reduce((sum, item) => sum + item.amount_due, 0)
            await supabase
                .from('weekly_payouts')
                .update({ total_amount: totalSum })
                .eq('id', payoutId)
        }

        revalidatePath('/financials/payday')
        return { success: true, payoutId }
    } catch (error: any) {
        console.error('Error creating payout run:', error)
        return { success: false, error: error.message }
    }
}

export async function updatePayoutItem(id: string, data: {
    amount_paid?: number
    status?: 'pending' | 'paid' | 'held'
    notes?: string
}) {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('role')
            .eq('id', user.id)
            .single()

        if (userProfile?.role !== 'admin') {
            throw new Error('Only admins can edit payout items')
        }

        const { data: item, error: fetchError } = await supabase
            .from('payout_items')
            .select('payout_id')
            .eq('id', id)
            .single()

        if (fetchError) throw fetchError

        // Update item details
        const { error: updateError } = await supabase
            .from('payout_items')
            .update(data)
            .eq('id', id)

        if (updateError) throw updateError

        // Recompute the Weekly Payout total amount
        const { data: allItems } = await supabase
            .from('payout_items')
            .select('amount_paid')
            .eq('payout_id', item.payout_id)

        const totalPaid = (allItems || []).reduce((sum, i) => sum + Number(i.amount_paid), 0)

        await supabase
            .from('weekly_payouts')
            .update({ total_amount: totalPaid })
            .eq('id', item.payout_id)

        revalidatePath('/financials/payday')
        return { success: true }
    } catch (error: any) {
        console.error('Error updating payout item:', error)
        return { success: false, error: error.message }
    }
}

export async function createCustomPayoutItem(payoutId: string, data: {
    recipient_type: 'employee_salary' | 'labor_wage' | 'vendor_payment' | 'other'
    recipient_name: string
    amount_due: number
    project_id?: string | null
    reference_details?: string
    notes?: string
}) {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('role')
            .eq('id', user.id)
            .single()

        if (userProfile?.role !== 'admin') {
            throw new Error('Only admins can add custom payout items')
        }

        const { error } = await supabase
            .from('payout_items')
            .insert({
                payout_id: payoutId,
                recipient_type: data.recipient_type,
                recipient_name: data.recipient_name,
                amount_due: data.amount_due,
                amount_paid: data.amount_due, // Defaults to full amount due
                status: 'pending',
                project_id: data.project_id || null,
                reference_details: data.reference_details || null,
                notes: data.notes || null,
                created_at: new Date().toISOString()
            })

        if (error) throw error

        // Recompute the Weekly Payout total amount
        const { data: allItems } = await supabase
            .from('payout_items')
            .select('amount_paid')
            .eq('payout_id', payoutId)

        const totalPaid = (allItems || []).reduce((sum, i) => sum + Number(i.amount_paid), 0)

        await supabase
            .from('weekly_payouts')
            .update({ total_amount: totalPaid })
            .eq('id', payoutId)

        revalidatePath('/financials/payday')
        return { success: true }
    } catch (error: any) {
        console.error('Error creating custom payout item:', error)
        return { success: false, error: error.message }
    }
}

export async function processWeeklyPayout(payoutId: string, status: 'approved' | 'paid') {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('role')
            .eq('id', user.id)
            .single()

        if (userProfile?.role !== 'admin') {
            throw new Error('Only admins can process weekly payouts')
        }

        // 1. Update status of the weekly payout run
        const { error: payoutError } = await supabase
            .from('weekly_payouts')
            .update({ status })
            .eq('id', payoutId)

        if (payoutError) throw payoutError

        // 2. If status is set to 'paid', update all pending items to 'paid'
        if (status === 'paid') {
            const { data: items } = await supabase
                .from('payout_items')
                .select('*')
                .eq('payout_id', payoutId)
                .eq('status', 'pending')

            if (items && items.length > 0) {
                // Update all items to paid
                await supabase
                    .from('payout_items')
                    .update({ status: 'paid' })
                    .eq('payout_id', payoutId)
                    .eq('status', 'pending')

                // 3. For each paid item that has a project_id, automatically record it in the expenses table!
                const expensesToLog: any[] = []

                for (const item of items) {
                    if (item.project_id) {
                        let category = 'Labor'
                        if (item.recipient_type === 'vendor_payment') {
                            category = 'Materials'
                        } else if (item.recipient_type === 'other') {
                            category = 'Other'
                        }

                        expensesToLog.push({
                            project_id: item.project_id,
                            category: category,
                            amount: Number(item.amount_paid),
                            description: `Payout: ${item.recipient_name} (${item.reference_details || 'Weekly Payout'})`,
                            expense_date: new Date().toISOString().split('T')[0],
                            created_by: user.id,
                            payment_status: 'paid',
                            notes: `Auto-generated from Pay-Day run. ${item.notes || ''}`,
                            created_at: new Date().toISOString()
                        })
                    }
                }

                if (expensesToLog.length > 0) {
                    const { error: expenseError } = await supabase
                        .from('expenses')
                        .insert(expensesToLog)
                    
                    if (expenseError) {
                        console.error('Error logging expenses automatically:', expenseError)
                    }
                }
            }
        }

        revalidatePath('/financials/payday')
        return { success: true }
    } catch (error: any) {
        console.error('Error processing weekly payout:', error)
        return { success: false, error: error.message }
    }
}

export async function deleteWeeklyPayout(id: string) {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('role')
            .eq('id', user.id)
            .single()

        if (userProfile?.role !== 'admin') {
            throw new Error('Only admins can delete weekly payouts')
        }

        const { error } = await supabase
            .from('weekly_payouts')
            .delete()
            .eq('id', id)

        if (error) throw error

        revalidatePath('/financials/payday')
        return { success: true }
    } catch (error: any) {
        console.error('Error deleting weekly payout:', error)
        return { success: false, error: error.message }
    }
}

export async function getCompanyUsers() {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single()

        if (!userProfile?.company_id) return []

        const { data: users, error } = await supabase
            .from('users')
            .select('id, display_name, email, role')
            .eq('company_id', userProfile.company_id)
            .order('display_name', { ascending: true })

        if (error) throw error
        return users || []
    } catch (error) {
        console.error('Error fetching company users:', error)
        return []
    }
}

export async function getProjects() {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: userProfile } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single()

        if (!userProfile?.company_id) return []

        const { data: projects, error } = await supabase
            .from('projects')
            .select('id, name')
            .eq('company_id', userProfile.company_id)
            .order('name', { ascending: true })

        if (error) throw error
        return projects || []
    } catch (error) {
        console.error('Error fetching projects:', error)
        return []
    }
}

