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
