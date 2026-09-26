// Client action module

import { createClient } from '@/utils/supabase/client';
import { z } from 'zod';

const contractorSchema = z.object({
    name: z.string().min(1, "Name is required"),
    category: z.string().optional(),
    contactPerson: z.string().optional(),
    phone: z.string().min(1, "Phone is required"),
    email: z.string().email().optional().or(z.literal('')),
    accountDetails: z.string().optional().or(z.literal('')),
    gst: z.string().optional().or(z.literal('')),
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

export async function getMaterialSuppliers() {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser();
        if (!user) return { success: false, error: 'Unauthorized', data: [] };

        const { data: userData } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single();

        let suppliersList: { id?: string; name: string; contactPerson?: string; phone?: string; accountDetails?: string; gst?: string }[] = [];

        if (userData?.company_id) {
            const { data: contractors } = await supabase
                .from('contractors')
                .select('*')
                .eq('companyId', userData.company_id)
                .order('name');

            if (contractors) {
                suppliersList = contractors.map(c => ({
                    id: c.id,
                    name: c.name,
                    contactPerson: c.contactPerson || '',
                    phone: c.phone || '',
                    accountDetails: (c as any).accountDetails || '',
                    gst: (c as any).gst || ''
                }));
            }
        }

        // Also fetch distinct supplier names from materials
        const { data: materialsData } = await supabase
            .from('materials')
            .select('supplier_name, supplier_contact')
            .not('supplier_name', 'is', null);

        if (materialsData) {
            const existingNames = new Set(suppliersList.map(s => s.name.trim().toLowerCase()));
            materialsData.forEach(m => {
                const sName = (m.supplier_name || '').trim();
                if (sName && !existingNames.has(sName.toLowerCase())) {
                    existingNames.add(sName.toLowerCase());
                    suppliersList.push({
                        name: sName,
                        phone: m.supplier_contact || '',
                    });
                }
            });
        }

        return { success: true, data: suppliersList };
    } catch (error: any) {
        console.error('Error fetching material suppliers:', error);
        return { success: false, error: error.message, data: [] };
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

        const insertPayload: Record<string, any> = {
            name: data.name,
            category: data.category || 'Material Supplier',
            contactPerson: data.contactPerson || null,
            phone: data.phone,
            email: data.email || null,
            companyId: userData.company_id,
        };

        if (data.accountDetails) insertPayload.accountDetails = data.accountDetails;
        if (data.gst) insertPayload.gst = data.gst;

        let res = await supabase
            .from('contractors')
            .insert(insertPayload)
            .select()
            .single();

        if (res.error && (res.error.message?.includes('column') || res.error.message?.includes('accountDetails') || res.error.message?.includes('gst'))) {
            // Safe fallback if column is not yet migrated in Supabase
            delete insertPayload.accountDetails;
            delete insertPayload.gst;
            res = await supabase
                .from('contractors')
                .insert(insertPayload)
                .select()
                .single();
        }

        if (res.error) throw res.error;

        return { success: true, data: res.data };
    } catch (error: any) {
        console.error('Error creating contractor/supplier:', error);
        return { success: false, error: error.message };
    }
}

export async function updateContractor(contractorId: string, data: ContractorData) {
    const supabase = await createClient();

    try {
        const validation = contractorSchema.safeParse(data);
        if (!validation.success) {
            return { success: false, error: validation.error.errors[0].message };
        }

        const { data: { user } } = await supabase.auth.getUser();
        if (!user) return { success: false, error: 'Unauthorized' };

        const { data: updatedContractor, error } = await supabase
            .from('contractors')
            .update({
                name: data.name,
                category: data.category || null,
                contactPerson: data.contactPerson || null,
                phone: data.phone,
                email: data.email || null,
            })
            .eq('id', contractorId)
            .select()
            .single();

        if (error) throw error;

        return { success: true, data: updatedContractor };
    } catch (error: any) {
        console.error('Error updating contractor:', error);
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
