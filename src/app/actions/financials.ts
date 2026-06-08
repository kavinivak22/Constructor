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
            .select('*, users:user_id(display_name, email, role), contractors:contractor_id(name)')
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
    contractor_id?: string | null
    rates?: Record<string, number> | null
    payment_type: 'monthly' | 'daily_wage' | 'hourly'
    rate?: number
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
            contractor_id: data.contractor_id || null,
            rates: data.rates || {},
            payment_type: data.payment_type,
            rate: data.rate || 0,
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
            .select('*, users:user_id(display_name), contractors:contractor_id(name)')
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
        let workerCounts: any[] = []

        if (worklogIds.length > 0) {
            const { data: lEntries } = await supabase
                .from('worklog_labor_entries')
                .select('*')
                .in('worklog_id', worklogIds)
                .neq('payment_status', 'paid') // ONLY compile unpaid or partial ones
            laborEntries = lEntries || []

            const laborEntryIds = laborEntries.map(le => le.id)
            if (laborEntryIds.length > 0) {
                const { data: wCounts } = await supabase
                    .from('worklog_worker_counts')
                    .select('*')
                    .in('labor_entry_id', laborEntryIds)
                workerCounts = wCounts || []
            }
        }

        // 4. Auto-generate payout items for Salary / Wage profiles
        if (profiles) {
            for (const profile of profiles) {
                if (profile.contractor_id) {
                    // Contractor worker profile: combine all category rates
                    const rates = (profile.rates as Record<string, number>) || {}
                    const breakdown: any[] = []
                    const descriptionsSet = new Set<string>()
                    
                    const contractorName = (profile.contractors?.name || '').toLowerCase().trim()
                    
                    // Filter matching labor entries for this contractor once
                    const matchingLaborEntries = laborEntries.filter(entry => {
                        const name = (entry.contractor_name || '').toLowerCase().trim()
                        return name.includes(contractorName) || contractorName.includes(name)
                    })
                    const matchingLaborEntryIds = matchingLaborEntries.map(le => le.id)

                    // Collect work descriptions
                    matchingLaborEntries.forEach(entry => {
                        if (entry.work_description) {
                            descriptionsSet.add(entry.work_description.trim())
                        }
                    })

                    // Calculate and update amount_due for each matching labor entry
                    for (const entry of matchingLaborEntries) {
                        let entryDue = 0
                        const entryCounts = workerCounts.filter(wc => wc.labor_entry_id === entry.id)
                        for (const wc of entryCounts) {
                            const wcTypeLower = (wc.worker_type || '').toLowerCase().trim()
                            // Find matching rate in profile keys
                            const matchingKey = Object.keys(rates).find(k => k.toLowerCase().trim() === wcTypeLower)
                            const rate = matchingKey ? Number(rates[matchingKey]) : 0
                            entryDue += Number(wc.count || 0) * rate
                        }
                        
                        // Save amount_due to database
                        await supabase
                            .from('worklog_labor_entries')
                            .update({ amount_due: entryDue })
                            .eq('id', entry.id)
                        
                        entry.amount_due = entryDue
                    }

                    // Calculate outstanding sum across matching entries
                    let contractorOutstandingSum = 0
                    const laborEntryIdsLinked: string[] = []
                    
                    matchingLaborEntries.forEach(entry => {
                        const outstanding = Number(entry.amount_due || 0) - Number(entry.amount_paid || 0)
                        if (outstanding > 0) {
                            contractorOutstandingSum += outstanding
                            laborEntryIdsLinked.push(entry.id)
                        }
                    })

                    // Build standard category count breakdown for UI summary
                    for (const [targetWorkerType, workerRateVal] of Object.entries(rates)) {
                        const workerRate = Number(workerRateVal)
                        const targetWorkerTypeLower = targetWorkerType.toLowerCase().trim()

                        const totalManDays = workerCounts
                            .filter(wc => matchingLaborEntryIds.includes(wc.labor_entry_id) && (wc.worker_type || '').toLowerCase().trim() === targetWorkerTypeLower)
                            .reduce((sum, wc) => sum + Number(wc.count || 0), 0)

                        const categoryAmount = totalManDays * workerRate

                        if (totalManDays > 0) {
                            breakdown.push({
                                category: targetWorkerType,
                                days: totalManDays,
                                rate: workerRate,
                                amount: categoryAmount
                            })
                        }
                    }

                    if (contractorOutstandingSum > 0) {
                        const recipientName = profile.contractors?.name || 'Contractor'
                        const recipientId = profile.contractor_id
                        const descriptions = Array.from(descriptionsSet)

                        const detailsJson = JSON.stringify({
                            type: 'contractor_wages',
                            descriptions: descriptions,
                            breakdown: breakdown,
                            labor_entry_ids: laborEntryIdsLinked
                        })

                        // Find a project ID from worklogs to link this labor expense to (optional)
                        const linkedWorklog = worklogs?.find(w => 
                            laborEntries.some(le => le.worklog_id === w.id && le.contractor_name?.toLowerCase().includes((profile.contractors?.name || '').toLowerCase()))
                        )

                        payoutItems.push({
                            payout_id: payoutId,
                            recipient_type: 'labor_wage',
                            recipient_id: recipientId,
                            recipient_name: recipientName,
                            amount_due: contractorOutstandingSum,
                            amount_paid: contractorOutstandingSum,
                            status: 'pending',
                            project_id: linkedWorklog?.project_id || null,
                            reference_details: detailsJson,
                            payout_class: 'nmr',
                            notes: `Bank: ${profile.bank_name || 'N/A'}, Acc: ${profile.account_number || 'N/A'}`
                        })
                    }
                } else {
                    // Employee / External worker profile
                    const recipientName = profile.worker_name || profile.users?.display_name || 'Worker'
                    const recipientId = profile.user_id
                    let amountDue = 0
                    let details = ''
                    let laborEntryIdsLinked: string[] = []

                    if (profile.payment_type === 'monthly') {
                        amountDue = Number(profile.rate)
                        details = 'Monthly Salary Rate'
                    } else if (profile.payment_type === 'daily_wage') {
                        const nameToMatch = recipientName.toLowerCase().trim()
                        const matchingEntries = laborEntries.filter(entry => {
                            const contractor = (entry.contractor_name || '').toLowerCase().trim()
                            return contractor.includes(nameToMatch) || nameToMatch.includes(contractor)
                        })

                        // Calculate and update entries
                        let outstandingSum = 0
                        for (const entry of matchingEntries) {
                            const entryDue = Number(profile.rate)
                            await supabase
                                .from('worklog_labor_entries')
                                .update({ amount_due: entryDue })
                                .eq('id', entry.id)
                            
                            entry.amount_due = entryDue
                            const outstanding = entryDue - Number(entry.amount_paid || 0)
                            if (outstanding > 0) {
                                outstandingSum += outstanding
                                laborEntryIdsLinked.push(entry.id)
                            }
                        }

                        amountDue = outstandingSum
                        details = JSON.stringify({
                            type: 'worker_wages',
                            text: `Daily Wage: ${laborEntryIdsLinked.length} days outstanding @ ₹${profile.rate}/day`,
                            labor_entry_ids: laborEntryIdsLinked
                        })
                    } else {
                        amountDue = Number(profile.rate)
                        details = 'Hourly Wage'
                    }

                    if (amountDue > 0) {
                        // Find a project ID from worklogs to link this labor expense to (optional)
                        const linkedWorklog = worklogs?.find(w => 
                            laborEntries.some(le => le.worklog_id === w.id && le.contractor_name?.toLowerCase().includes((profile.contractors?.name || recipientName).toLowerCase()))
                        )

                        payoutItems.push({
                            payout_id: payoutId,
                            recipient_type: (profile.payment_type === 'monthly' && !profile.contractor_id) ? 'employee_salary' : 'labor_wage',
                            recipient_id: recipientId,
                            recipient_name: recipientName,
                            amount_due: amountDue,
                            amount_paid: amountDue,
                            status: 'pending',
                            project_id: linkedWorklog?.project_id || null,
                            reference_details: details,
                            payout_class: 'nmr',
                            notes: `Bank: ${profile.bank_name || 'N/A'}, Acc: ${profile.account_number || 'N/A'}`
                        })
                    }
                }
            }
        }

        // 5. Fetch approved/delivered Purchase Orders in the date range that are not paid
        const { data: pos } = await supabase
            .from('purchase_orders')
            .select('*')
            .eq('company_id', companyId)
            .in('status', ['approved', 'delivered'])
            .neq('payment_status', 'paid')
            .gte('created_at', weekStartDate)
            .lte('created_at', weekEndDate)

        if (pos) {
            for (const po of pos) {
                const outstandingPO = Number(po.total_amount || 0) - Number(po.amount_paid || 0)
                if (outstandingPO > 0) {
                    payoutItems.push({
                        payout_id: payoutId,
                        recipient_type: 'vendor_payment',
                        recipient_id: po.id,
                        recipient_name: po.supplier_name,
                        amount_due: outstandingPO,
                        amount_paid: outstandingPO,
                        status: 'pending',
                        project_id: po.project_id,
                        reference_details: `PO Number: ${po.po_number || 'N/A'} (Outstanding: ₹${outstandingPO})`,
                        payout_class: 'nmr',
                        notes: `Contact: ${po.supplier_contact || 'N/A'}`
                    })
                }
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
    payout_class?: 'rate' | 'nmr'
    recipient_name?: string
    reference_details?: string | null
    project_id?: string | null
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
            .select('*')
            .eq('id', id)
            .single()

        if (fetchError || !item) throw new Error('Payout item not found')

        const originalStatus = item.status
        const originalAmountPaid = Number(item.amount_paid)
        const originalProjectId = item.project_id

        // Update item details
        const { error: updateError } = await supabase
            .from('payout_items')
            .update(data)
            .eq('id', id)

        if (updateError) throw updateError

        // Fetch the updated item to pass to the helper
        const { data: updatedItem } = await supabase
            .from('payout_items')
            .select('*')
            .eq('id', id)
            .single()

        if (updatedItem) {
            await syncExpenseForPayoutItem(
                supabase,
                updatedItem,
                originalStatus,
                originalAmountPaid,
                originalProjectId
            )
        }

        // Trigger payment reconciliation for linked labor logs or PO
        const statusToApply = data.status || item.status

        if (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') {
            if (item.reference_details?.startsWith('{')) {
                try {
                    const parsed = JSON.parse(item.reference_details)
                    const laborEntryIds = parsed.labor_entry_ids || []
                    if (laborEntryIds.length > 0) {
                        await reconcileLaborPayments(supabase, laborEntryIds)
                    }
                } catch (e) {
                    // ignore
                }
            }
        } else if (item.recipient_type === 'vendor_payment' && item.recipient_id) {
            await reconcilePOPayment(supabase, item.recipient_id)
        }

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

export async function deletePayoutItem(id: string) {
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
            throw new Error('Only admins can delete payout items')
        }

        // 1. Fetch the item first to get its parent payout_id and linked details for reconciliation
        const { data: item, error: fetchError } = await supabase
            .from('payout_items')
            .select('*')
            .eq('id', id)
            .single()

        if (fetchError || !item) throw new Error('Payout item not found')

        // Delete associated expense if it exists
        await syncExpenseForPayoutItem(
            supabase,
            { ...item, status: 'pending' },
            item.status,
            Number(item.amount_paid),
            item.project_id
        )

        // 2. Delete the item
        const { error: deleteError } = await supabase
            .from('payout_items')
            .delete()
            .eq('id', id)

        if (deleteError) throw deleteError

        // 3. Trigger payment reconciliation for linked labor logs or PO
        if (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') {
            if (item.reference_details?.startsWith('{')) {
                try {
                    const parsed = JSON.parse(item.reference_details)
                    const laborEntryIds = parsed.labor_entry_ids || []
                    if (laborEntryIds.length > 0) {
                        await reconcileLaborPayments(supabase, laborEntryIds)
                    }
                } catch (e) {
                    // ignore
                }
            }
        } else if (item.recipient_type === 'vendor_payment' && item.recipient_id) {
            await reconcilePOPayment(supabase, item.recipient_id)
        }

        // 4. Recompute the Weekly Payout total amount
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
        console.error('Error deleting payout item:', error)
        return { success: false, error: error.message }
    }
}

export async function bulkUpdatePayoutItems(ids: string[], updates: {
    status?: 'pending' | 'paid' | 'held'
    project_id?: string | null
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

        if (ids.length === 0) return { success: true }

        // Fetch all items to get payout_id and linked items for reconciliation
        const { data: items, error: fetchError } = await supabase
            .from('payout_items')
            .select('*')
            .in('id', ids)

        if (fetchError || !items) throw new Error('Payout items not found')

        // Update items in database
        const payload: any = {}
        if (updates.status !== undefined) payload.status = updates.status
        if (updates.project_id !== undefined) payload.project_id = updates.project_id

        const { error: updateError } = await supabase
            .from('payout_items')
            .update(payload)
            .in('id', ids)

        if (updateError) throw updateError

        // Fetch fresh updated items and sync expenses
        const { data: updatedItems } = await supabase
            .from('payout_items')
            .select('*')
            .in('id', ids)

        if (updatedItems) {
            for (const updatedItem of updatedItems) {
                const original = items.find(i => i.id === updatedItem.id)
                if (original) {
                    await syncExpenseForPayoutItem(
                        supabase,
                        updatedItem,
                        original.status,
                        Number(original.amount_paid),
                        original.project_id
                    )
                }
            }
        }

        // Trigger reconciliation for each affected item
        const affectedLaborEntryIds = new Set<string>()
        const affectedPOIds = new Set<string>()

        for (const item of items) {
            if (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') {
                if (item.reference_details?.startsWith('{')) {
                    try {
                        const parsed = JSON.parse(item.reference_details)
                        const laborEntryIds = parsed.labor_entry_ids || []
                        laborEntryIds.forEach((id: string) => affectedLaborEntryIds.add(id))
                    } catch (e) {
                        // ignore
                    }
                }
            } else if (item.recipient_type === 'vendor_payment' && item.recipient_id) {
                affectedPOIds.add(item.recipient_id)
            }
        }

        if (affectedLaborEntryIds.size > 0) {
            await reconcileLaborPayments(supabase, Array.from(affectedLaborEntryIds))
        }

        for (const poId of affectedPOIds) {
            await reconcilePOPayment(supabase, poId)
        }

        // Recompute the Weekly Payout total amount
        const payoutId = items[0].payout_id
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
        console.error('Error bulk updating payout items:', error)
        return { success: false, error: error.message }
    }
}

export async function bulkDeletePayoutItems(ids: string[]) {
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
            throw new Error('Only admins can delete payout items')
        }

        if (ids.length === 0) return { success: true }

        // Fetch all items to get payout_id and linked items for reconciliation
        const { data: items, error: fetchError } = await supabase
            .from('payout_items')
            .select('*')
            .in('id', ids)

        if (fetchError || !items) throw new Error('Payout items not found')

        // Delete associated expenses first
        for (const item of items) {
            await syncExpenseForPayoutItem(
                supabase,
                { ...item, status: 'pending' },
                item.status,
                Number(item.amount_paid),
                item.project_id
            )
        }

        // Delete items from database
        const { error: deleteError } = await supabase
            .from('payout_items')
            .delete()
            .in('id', ids)

        if (deleteError) throw deleteError

        // Trigger reconciliation for each affected item
        const affectedLaborEntryIds = new Set<string>()
        const affectedPOIds = new Set<string>()

        for (const item of items) {
            if (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') {
                if (item.reference_details?.startsWith('{')) {
                    try {
                        const parsed = JSON.parse(item.reference_details)
                        const laborEntryIds = parsed.labor_entry_ids || []
                        laborEntryIds.forEach((id: string) => affectedLaborEntryIds.add(id))
                    } catch (e) {
                        // ignore
                    }
                }
            } else if (item.recipient_type === 'vendor_payment' && item.recipient_id) {
                affectedPOIds.add(item.recipient_id)
            }
        }

        if (affectedLaborEntryIds.size > 0) {
            await reconcileLaborPayments(supabase, Array.from(affectedLaborEntryIds))
        }

        for (const poId of affectedPOIds) {
            await reconcilePOPayment(supabase, poId)
        }

        // Recompute the Weekly Payout total amount
        const payoutId = items[0].payout_id
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
        console.error('Error bulk deleting payout items:', error)
        return { success: false, error: error.message }
    }
}

export async function createCustomPayoutItem(payoutId: string, data: {
    recipient_type: 'employee_salary' | 'labor_wage' | 'vendor_payment' | 'other'
    recipient_name: string
    recipient_id?: string | null
    amount_due: number
    project_id?: string | null
    reference_details?: string
    notes?: string
    payout_class?: 'rate' | 'nmr'
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
                recipient_id: data.recipient_id || null,
                amount_due: data.amount_due,
                amount_paid: data.amount_due, // Defaults to full amount due
                status: 'pending',
                project_id: data.project_id || null,
                reference_details: data.reference_details || null,
                notes: data.notes || null,
                payout_class: data.payout_class || 'nmr',
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

        // 2. If status is set to 'paid', update all pending items to 'paid' and run reconciliation
        if (status === 'paid') {
            const { data: items } = await supabase
                .from('payout_items')
                .select('*')
                .eq('payout_id', payoutId)
                .eq('status', 'pending')

            if (items && items.length > 0) {
                // Update and reconcile each item individually
                for (const item of items) {
                    await supabase
                        .from('payout_items')
                        .update({ status: 'paid' })
                        .eq('id', item.id)

                    // Run payment reconciliation for this item
                    if (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') {
                        if (item.reference_details?.startsWith('{')) {
                            try {
                                const parsed = JSON.parse(item.reference_details)
                                const laborEntryIds = parsed.labor_entry_ids || []
                                if (laborEntryIds.length > 0) {
                                    await reconcileLaborPayments(supabase, laborEntryIds)
                                }
                            } catch (e) {
                                // ignore
                            }
                        }
                    } else if (item.recipient_type === 'vendor_payment' && item.recipient_id) {
                        await reconcilePOPayment(supabase, item.recipient_id)
                    }
                }

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

                        let displayDetails = item.reference_details || 'Weekly Payout'
                        if (item.reference_details?.startsWith('{')) {
                            try {
                                const parsed = JSON.parse(item.reference_details)
                                if (parsed && parsed.type === 'contractor_wages') {
                                    const categories = (parsed.breakdown || []).map((b: any) => b.category).join(', ')
                                    displayDetails = `Contractor Wages: ${categories}`
                                }
                            } catch (e) {
                                // fallback
                            }
                        }

                        expensesToLog.push({
                            project_id: item.project_id,
                            category: category,
                            amount: Number(item.amount_paid),
                            description: `Payout: ${item.recipient_name} (${displayDetails})`,
                            expense_date: new Date().toISOString().split('T')[0],
                            created_by: user.id,
                            payment_status: 'paid',
                            notes: `Auto-generated from Pay-Day run. Item ID: ${item.id}. ${item.notes || ''}`,
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

export async function getPayoutItemBreakdown(payoutItemId: string) {
    const supabase = await createClient()

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // 1. Fetch the payout item
        const { data: item, error: itemError } = await supabase
            .from('payout_items')
            .select(`
                *,
                payout:payout_id(week_start_date, week_end_date, company_id)
            `)
            .eq('id', payoutItemId)
            .single()

        if (itemError || !item) throw new Error('Payout item not found')

        const payout = item.payout
        const weekStart = payout.week_start_date
        const weekEnd = payout.week_end_date

        if (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') {
            // Find daily worklog entries in range
            const { data: worklogs } = await supabase
                .from('daily_worklogs')
                .select('id, date, title, project:project_id(name)')
                .gte('date', weekStart)
                .lte('date', weekEnd)
                .order('date', { ascending: true })

            const worklogIds = worklogs?.map(w => w.id) || []
            let laborEntries: any[] = []
            let workerCounts: any[] = []

            if (worklogIds.length > 0) {
                // Fetch labor entries
                const { data: lEntries } = await supabase
                    .from('worklog_labor_entries')
                    .select('*')
                    .in('worklog_id', worklogIds)
                laborEntries = lEntries || []

                const laborEntryIds = laborEntries.map(le => le.id)
                if (laborEntryIds.length > 0) {
                    const { data: wCounts } = await supabase
                        .from('worklog_worker_counts')
                        .select('*')
                        .in('labor_entry_id', laborEntryIds)
                    workerCounts = wCounts || []
                }
            }

            // Now filter entries matching this recipient
            let matchedLogs: any[] = []

            // Contractor match or worker name match
            let contractorName = ''
            let workerCategoryFilter = ''

            if (item.recipient_name.includes('(')) {
                const parts = item.recipient_name.split('(')
                contractorName = parts[0].trim().toLowerCase()
                workerCategoryFilter = parts[1].replace(')', '').trim().toLowerCase()
            } else {
                contractorName = item.recipient_name.trim().toLowerCase()
            }

            for (const entry of laborEntries) {
                const entryContractor = (entry.contractor_name || '').toLowerCase().trim()
                const isContractorMatch = entryContractor.includes(contractorName) || contractorName.includes(entryContractor)
                
                if (isContractorMatch) {
                    const worklog = worklogs?.find(w => w.id === entry.worklog_id)
                    if (!worklog) continue

                    // Get counts for this entry
                    const entryCounts = workerCounts.filter(wc => wc.labor_entry_id === entry.id)
                    
                    // Filter counts by category if it's a specific contractor worker type
                    const filteredCounts = workerCategoryFilter 
                        ? entryCounts.filter(wc => (wc.worker_type || '').toLowerCase().trim() === workerCategoryFilter)
                        : entryCounts

                    if (filteredCounts.length > 0 || !workerCategoryFilter) {
                        let projectName = 'General'
                        if (worklog.project) {
                            if (Array.isArray(worklog.project)) {
                                projectName = (worklog.project as any[])[0]?.name || 'General'
                            } else {
                                projectName = (worklog.project as any).name || 'General'
                            }
                        }

                        matchedLogs.push({
                            date: worklog.date,
                            worklogTitle: worklog.title,
                            projectName: projectName,
                            workDescription: entry.work_description,
                            category: entry.category,
                            workers: filteredCounts.map(c => ({
                                type: c.worker_type,
                                count: c.count
                            }))
                        })
                    }
                }
            }

            return {
                type: 'labor',
                recipientName: item.recipient_name,
                details: matchedLogs
            }
        } else if (item.recipient_type === 'vendor_payment') {
            if (!item.recipient_id) return { type: 'vendor', details: [] }

            const { data: poItems, error: poError } = await supabase
                .from('purchase_order_items')
                .select('*')
                .eq('po_id', item.recipient_id)

            if (poError) throw poError

            return {
                type: 'vendor',
                recipientName: item.recipient_name,
                details: poItems || []
            }
        }

        return { type: 'other', details: [] }
    } catch (error) {
        console.error('Error fetching payout item breakdown:', error)
        return { type: 'error', details: [] }
    }
}

// ==========================================
// Payment Reconciliation & Payout Splitting Helpers
// ==========================================

export async function splitPayoutItem(itemId: string, rateAmount: number, nmrAmount: number) {
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
            throw new Error('Only admins can split payout items')
        }

        // 1. Fetch the original item
        const { data: item, error: fetchError } = await supabase
            .from('payout_items')
            .select('*')
            .eq('id', itemId)
            .single()

        if (fetchError || !item) throw new Error('Payout item not found')

        // 2. Parse original reference details to copy them
        let baseRef = {}
        try {
            if (item.reference_details) {
                baseRef = JSON.parse(item.reference_details)
            }
        } catch (e) {
            // fallback
        }

        // Create reference details for both parts
        const rateDetails = JSON.stringify({
            ...baseRef,
            split_original_id: item.id,
            split_type: 'rate'
        })
        const nmrDetails = JSON.stringify({
            ...baseRef,
            split_original_id: item.id,
            split_type: 'nmr'
        })

        // 3. Update the original item to be the RATE part
        const { error: updateError } = await supabase
            .from('payout_items')
            .update({
                amount_due: rateAmount,
                amount_paid: rateAmount,
                payout_class: 'rate',
                reference_details: rateDetails
            })
            .eq('id', itemId)

        if (updateError) throw updateError

        // 4. Create a new sibling item for the NMR part
        const { error: insertError } = await supabase
            .from('payout_items')
            .insert({
                payout_id: item.payout_id,
                recipient_type: item.recipient_type,
                recipient_id: item.recipient_id,
                recipient_name: item.recipient_name,
                amount_due: nmrAmount,
                amount_paid: nmrAmount,
                status: item.status,
                project_id: item.project_id,
                reference_details: nmrDetails,
                payout_class: 'nmr',
                notes: `Split from original payout. ${item.notes || ''}`
            })

        if (insertError) throw insertError

        // Trigger reconciliation for both parts
        if (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') {
            const laborEntryIds = (baseRef as any).labor_entry_ids || []
            if (laborEntryIds.length > 0) {
                await reconcileLaborPayments(supabase, laborEntryIds)
            }
        }

        // 5. Recompute weekly payout total
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
        console.error('Error splitting payout item:', error)
        return { success: false, error: error.message }
    }
}

export async function reconcileLaborPayments(supabase: any, affectedEntryIds: string[]) {
    if (!affectedEntryIds || affectedEntryIds.length === 0) return

    try {
        // Reset all affected entries to unpaid/0
        await supabase
            .from('worklog_labor_entries')
            .update({ amount_paid: 0, payment_status: 'unpaid' })
            .in('id', affectedEntryIds)

        // Fetch the full details of these entries to have their amount_due in memory
        const { data: entries } = await supabase
            .from('worklog_labor_entries')
            .select('id, amount_due, amount_paid, payment_status')
            .in('id', affectedEntryIds)

        if (!entries || entries.length === 0) return
        
        const entryMap = new Map<string, any>()
        entries.forEach((e: any) => entryMap.set(e.id, { ...e, amount_paid: 0, payment_status: 'unpaid' }))

        // Fetch all paid payout items that reference any of these entry IDs
        const { data: paidItems } = await supabase
            .from('payout_items')
            .select('id, amount_paid, status, reference_details')
            .eq('status', 'paid')

        if (!paidItems) return

        // Filter paid items that reference our affected entries
        const referencingItems = paidItems.filter((item: any) => {
            if (!item.reference_details) return false
            try {
                const parsed = JSON.parse(item.reference_details)
                const ids = parsed.labor_entry_ids || []
                return ids.some((id: string) => affectedEntryIds.includes(id))
            } catch (err) {
                return false
            }
        })

        // Sort items by id to ensure deterministic FIFO distribution
        referencingItems.sort((a: any, b: any) => a.id.localeCompare(b.id))

        // For each paid item, distribute its amount_paid
        for (const item of referencingItems) {
            const parsed = JSON.parse(item.reference_details)
            const itemEntryIds = parsed.labor_entry_ids || []
            
            let remainingPayment = Number(item.amount_paid)

            // Distribute FIFO among the entries linked to this item
            for (const entryId of itemEntryIds) {
                const entry = entryMap.get(entryId)
                if (!entry) continue

                const outstanding = Number(entry.amount_due) - Number(entry.amount_paid)
                if (outstanding <= 0) continue

                if (remainingPayment >= outstanding) {
                    entry.amount_paid = Number(entry.amount_paid) + outstanding
                    entry.payment_status = 'paid'
                    remainingPayment -= outstanding
                } else {
                    entry.amount_paid = Number(entry.amount_paid) + remainingPayment
                    entry.payment_status = 'partial'
                    remainingPayment = 0
                    break
                }
            }
        }

        // Save the reconciled states back to the database
        for (const [id, entry] of entryMap.entries()) {
            await supabase
                .from('worklog_labor_entries')
                .update({
                    amount_paid: entry.amount_paid,
                    payment_status: entry.payment_status
                })
                .eq('id', id)
        }
    } catch (err) {
        console.error('Error in reconcileLaborPayments:', err)
    }
}

export async function reconcilePOPayment(supabase: any, poId: string) {
    if (!poId) return

    try {
        // Reset PO payment status/amount
        await supabase
            .from('purchase_orders')
            .update({ amount_paid: 0, payment_status: 'unpaid' })
            .eq('id', poId)

        // Fetch the PO total_amount
        const { data: po } = await supabase
            .from('purchase_orders')
            .select('total_amount')
            .eq('id', poId)
            .single()

        if (!po) return

        // Fetch all paid payout items linked to this PO
        const { data: paidItems } = await supabase
            .from('payout_items')
            .select('amount_paid')
            .eq('recipient_id', poId)
            .eq('status', 'paid')

        const totalPaid = (paidItems || []).reduce((sum: number, item: any) => sum + Number(item.amount_paid), 0)
        const poTotal = Number(po.total_amount || 0)

        let status = 'unpaid'
        if (totalPaid >= poTotal && poTotal > 0) {
            status = 'paid'
        } else if (totalPaid > 0) {
            status = 'partial'
        }

        await supabase
            .from('purchase_orders')
            .update({
                amount_paid: totalPaid,
                payment_status: status
            })
            .eq('id', poId)
    } catch (err) {
        console.error('Error in reconcilePOPayment:', err)
    }
}

export async function syncExpenseForPayoutItem(
    supabase: any,
    item: any,
    originalStatus: string,
    originalAmountPaid: number,
    originalProjectId: string | null
) {
    try {
        // Find existing expense if any
        // Search by Item ID in notes first
        const { data: expensesByNote } = await supabase
            .from('expenses')
            .select('*')
            .like('notes', `%Item ID: ${item.id}%`)
        
        let existingExpense = expensesByNote?.[0] || null

        // Fallback search by description, project, and amount for legacy runs
        if (!existingExpense && originalProjectId) {
            let displayDetails = item.reference_details || 'Weekly Payout'
            if (item.reference_details?.startsWith('{')) {
                try {
                    const parsed = JSON.parse(item.reference_details)
                    if (parsed && parsed.type === 'contractor_wages') {
                        const categories = (parsed.breakdown || []).map((b: any) => b.category).join(', ')
                        displayDetails = `Contractor Wages: ${categories}`
                    }
                } catch (e) {}
            }
            const expectedDesc = `Payout: ${item.recipient_name} (${displayDetails})`
            
            const { data: legacyExpenses } = await supabase
                .from('expenses')
                .select('*')
                .eq('project_id', originalProjectId)
                .eq('amount', originalAmountPaid)
                .eq('description', expectedDesc)
            
            existingExpense = legacyExpenses?.[0] || null
        }

        const shouldHaveExpense = item.status === 'paid' && item.project_id

        if (shouldHaveExpense) {
            let category = 'Labor'
            if (item.recipient_type === 'vendor_payment') {
                category = 'Materials'
            } else if (item.recipient_type === 'other') {
                category = 'Other'
            }

            let displayDetails = item.reference_details || 'Weekly Payout'
            if (item.reference_details?.startsWith('{')) {
                try {
                    const parsed = JSON.parse(item.reference_details)
                    if (parsed && parsed.type === 'contractor_wages') {
                        const categories = (parsed.breakdown || []).map((b: any) => b.category).join(', ')
                        displayDetails = `Contractor Wages: ${categories}`
                    }
                } catch (e) {}
            }

            const expensePayload = {
                project_id: item.project_id,
                category: category,
                amount: Number(item.amount_paid),
                description: `Payout: ${item.recipient_name} (${displayDetails})`,
                notes: `Auto-generated from Pay-Day run. Item ID: ${item.id}. ${item.notes || ''}`,
                payment_status: 'paid'
            }

            if (existingExpense) {
                // Update existing expense
                await supabase
                    .from('expenses')
                    .update(expensePayload)
                    .eq('id', existingExpense.id)
            } else {
                // Fetch the creator user from the payout run to assign it
                const { data: payout } = await supabase
                    .from('weekly_payouts')
                    .select('created_by')
                    .eq('id', item.payout_id)
                    .single()

                // Insert new expense
                await supabase
                    .from('expenses')
                    .insert({
                        ...expensePayload,
                        expense_date: new Date().toISOString().split('T')[0],
                        created_by: payout?.created_by || null,
                        created_at: new Date().toISOString()
                    })
            }
        } else {
            // Delete existing expense if it shouldn't exist anymore
            if (existingExpense) {
                await supabase
                    .from('expenses')
                    .delete()
                    .eq('id', existingExpense.id)
            }
        }
    } catch (err) {
        console.error('Error in syncExpenseForPayoutItem:', err)
    }
}



