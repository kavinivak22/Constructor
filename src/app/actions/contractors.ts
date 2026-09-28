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

        if (!userData?.company_id) {
            return { success: true, data: [] };
        }

        const companyId = userData.company_id;

        // 1. Identify labor contractors (those who have daily trade wage rate cards in salary_profiles)
        const { data: salaryProfiles } = await supabase
            .from('salary_profiles')
            .select('contractor_id')
            .eq('company_id', companyId)
            .not('contractor_id', 'is', null);

        const laborContractorIds = new Set(
            (salaryProfiles || []).map((sp: any) => sp.contractor_id).filter(Boolean)
        );

        // Standard labor trades to exclude from suppliers list
        const laborTrades = new Set([
            'masonry', 'mason', 'carpentry', 'carpenter', 'plumbing', 'plumber', 
            'electrical', 'electrician', 'painting', 'painter', 'welding', 'welder', 
            'bar bending', 'bar bender', 'labor', 'labour', 'coolie', 'helper', 
            'civil', 'flooring', 'tiles', 'tile', 'roofing', 'fabrication', 
            'scaffolding', 'false ceiling', 'pop', 'waterproofing', 'general contractor', 'contractor', 'general'
        ]);

        const isSupplierCategory = (cat?: string | null) => {
            if (!cat) return false;
            const c = cat.toLowerCase().trim();
            if (laborTrades.has(c)) return false;
            return c === 'material supplier' ||
                   c === 'supplier' ||
                   c === 'vendor' ||
                   c.includes('supplier') ||
                   c.includes('vendor') ||
                   c.includes('dealer') ||
                   c.includes('hardware') ||
                   c.includes('distributor') ||
                   c.includes('building materials');
        };

        // 2. Fetch contractors strictly belonging to THIS company
        const { data: companyContractors, error: contractorsError } = await supabase
            .from('contractors')
            .select('*')
            .eq('companyId', companyId)
            .order('name');

        if (contractorsError) {
            console.error('Error fetching contractors for suppliers:', contractorsError);
        }

        let suppliersList: { id?: string; name: string; contactPerson?: string; phone?: string; accountDetails?: string; gst?: string }[] = [];

        if (companyContractors) {
            // Keep ONLY entries that are genuine material suppliers, NOT trade labor contractors
            const filteredSuppliers = companyContractors.filter((c: any) => {
                // If they have a labor wage profile in salary_profiles, they are a labor contractor, not a supplier
                if (laborContractorIds.has(c.id)) return false;

                const cat = (c.category || '').toLowerCase().trim();
                // If categorized as a labor trade, exclude
                if (laborTrades.has(cat)) return false;

                // Explicit supplier category
                if (isSupplierCategory(c.category)) return true;

                // Has GST or vendor account details and is not a labor trade
                if ((c.gst || c.accountDetails) && !laborTrades.has(cat)) return true;

                return false;
            });

            suppliersList = filteredSuppliers.map((c: any) => ({
                id: c.id,
                name: c.name,
                contactPerson: c.contactPerson || '',
                phone: c.phone || '',
                accountDetails: c.accountDetails || '',
                gst: c.gst || ''
            }));
        }

        // 3. Also fetch previous supplier names from materials, BUT ONLY for THIS company's sites!
        const { data: companySites } = await supabase
            .from('sites')
            .select('id')
            .eq('company_id', companyId);

        const siteIds = (companySites || []).map((s: any) => s.id);

        if (siteIds.length > 0) {
            const { data: materialsData } = await supabase
                .from('materials')
                .select('supplier_name, supplier_contact')
                .in('site_id', siteIds)
                .not('supplier_name', 'is', null);

            if (materialsData) {
                const existingNames = new Set(suppliersList.map(s => s.name.trim().toLowerCase()));
                const laborContractorNames = new Set(
                    (companyContractors || [])
                        .filter((c: any) => laborContractorIds.has(c.id) || laborTrades.has((c.category || '').toLowerCase().trim()))
                        .map((c: any) => c.name.trim().toLowerCase())
                );

                materialsData.forEach((m: any) => {
                    const sName = (m.supplier_name || '').trim();
                    if (sName && !existingNames.has(sName.toLowerCase()) && !laborContractorNames.has(sName.toLowerCase())) {
                        existingNames.add(sName.toLowerCase());
                        suppliersList.push({
                            name: sName,
                            phone: m.supplier_contact || '',
                        });
                    }
                });
            }
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
            category: data.category?.trim() || null,
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
