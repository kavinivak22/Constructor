'use server';

import { createClient } from '@/utils/supabase/server';
import { cookies } from 'next/headers';
import { z } from 'zod';

const contractorSchema = z.object({
    name: z.string().min(1, "Name is required"),
    category: z.string().optional(),
    contactPerson: z.string().optional(),
    phone: z.string().min(1, "Phone is required"),
    email: z.string().email().optional().or(z.literal('')),
});

export type ContractorData = z.infer<typeof contractorSchema>;

export async function getContractors() {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser();
        if (!user) return { success: false, error: 'Unauthorized' };

        // Get user's companyId
        const { data: userData } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single();

        if (!userData?.company_id) return { success: false, error: 'No company found' };

        const { data, error } = await supabase
            .from('contractors')
            .select('*')
            .eq('companyId', userData.company_id)
            .order('name');

        if (error) throw error;

        return { success: true, data };
    } catch (error: any) {
        console.error('Error fetching contractors:', error);
        return { success: false, error: error.message };
    }
}

export async function createContractor(data: ContractorData) {
    const supabase = await createClient();

    try {
        const validation = contractorSchema.safeParse(data);
        if (!validation.success) {
            return { success: false, error: validation.error.errors[0].message };
        }

        const { data: { user } } = await supabase.auth.getUser();
        if (!user) return { success: false, error: 'Unauthorized' };

        // Get user's companyId
        const { data: userData } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single();

        if (!userData?.company_id) return { success: false, error: 'No company found' };

        const { data: newContractor, error } = await supabase
            .from('contractors')
            .insert({
                ...data,
                companyId: userData.company_id,
            })
            .select()
            .single();

        if (error) throw error;

        return { success: true, data: newContractor };
    } catch (error: any) {
        console.error('Error creating contractor:', error);
        return { success: false, error: error.message };
    }
}

export async function getContractorAccounts() {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser();
        if (!user) return { success: false, error: 'Unauthorized' };

        // Get user's companyId
        const { data: userData } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single();

        if (!userData?.company_id) return { success: false, error: 'No company found' };

        // Fetch contractors
        const { data: contractors, error: cError } = await supabase
            .from('contractors')
            .select('*')
            .eq('companyId', userData.company_id)
            .order('name');

        if (cError) throw cError;
        if (!contractors || contractors.length === 0) return { success: true, data: [] };

        // Fetch all payout items linked to these contractors
        const contractorIds = contractors.map(c => c.id);
        const { data: payoutItems, error: pError } = await supabase
            .from('payout_items')
            .select('*, project:project_id(name), payout:payout_id(week_start_date, week_end_date)')
            .in('recipient_id', contractorIds);

        if (pError) throw pError;

        // Compute accounts summary for each contractor
        const accountsData = contractors.map(contractor => {
            const items = (payoutItems || []).filter(item => item.recipient_id === contractor.id);
            
            // Filter rate payouts
            const rateItems = items.filter(item => item.payout_class === 'rate');
            // Filter NMR payouts
            const nmrItems = items.filter(item => item.payout_class === 'nmr' || !item.payout_class);

            const rateTotalDue = rateItems.reduce((sum, item) => sum + Number(item.amount_due), 0);
            const rateTotalPaid = rateItems.filter(item => item.status === 'paid').reduce((sum, item) => sum + Number(item.amount_paid), 0);
            const rateTotalPending = rateItems.filter(item => item.status === 'pending').reduce((sum, item) => sum + Number(item.amount_paid), 0);

            const nmrTotalDue = nmrItems.reduce((sum, item) => sum + Number(item.amount_due), 0);
            const nmrTotalPaid = nmrItems.filter(item => item.status === 'paid').reduce((sum, item) => sum + Number(item.amount_paid), 0);
            const nmrTotalPending = nmrItems.filter(item => item.status === 'pending').reduce((sum, item) => sum + Number(item.amount_paid), 0);

            return {
                contractor,
                rateAccount: {
                    totalDue: rateTotalDue,
                    totalPaid: rateTotalPaid,
                    totalPending: rateTotalPending,
                    items: rateItems
                },
                nmrAccount: {
                    totalDue: nmrTotalDue,
                    totalPaid: nmrTotalPaid,
                    totalPending: nmrTotalPending,
                    items: nmrItems
                },
                allTransactions: items.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime())
            };
        });

        return { success: true, data: accountsData };
    } catch (error: any) {
        console.error('Error fetching contractor accounts:', error);
        return { success: false, error: error.message };
    }
}
