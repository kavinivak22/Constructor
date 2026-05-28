'use server'

import { createClient } from '@/utils/supabase/server';
import { cookies } from 'next/headers';
import { PurchaseOrder } from '@/lib/data';

export async function getPurchaseOrders(projectId?: string) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        let projectIds: string[] = [];
        if (projectId) {
            const { data: member, error: memberError } = await supabase
                .from('project_members')
                .select('project_id')
                .eq('user_id', user.id)
                .eq('project_id', projectId)
                .maybeSingle();

            if (memberError) throw memberError;
            if (!member) return { success: true, data: [] };
            projectIds = [projectId];
        } else {
            const { data: membersData, error: membersError } = await supabase
                .from('project_members')
                .select('project_id')
                .eq('user_id', user.id);

            if (membersError) throw membersError;
            projectIds = membersData?.map(m => m.project_id) || [];
            if (projectIds.length === 0) return { success: true, data: [] };
        }

        const { data, error } = await supabase
            .from('purchase_orders')
            .select(`
                *,
                projects(name),
                creator:users!purchase_orders_created_by_fkey(display_name),
                approver:users!purchase_orders_approved_by_fkey(display_name),
                purchase_order_items(*)
            `)
            .in('project_id', projectIds)
            .order('created_at', { ascending: false });

        if (error) throw error;

        return { success: true, data: data as unknown as PurchaseOrder[] }
    } catch (error: any) {
        console.error('Error fetching purchase orders:', error)
        return { success: false, error: error.message }
    }
}

export async function createPurchaseOrder(payload: {
    projectId: string;
    supplierName: string;
    supplierContact?: string;
    deliveryDate?: string;
    specialInstructions?: string;
    items: Array<{
        materialId?: string | null;
        materialName: string;
        quantity: number;
        unitPrice: number;
    }>;
}) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Generate unique PO number: PO-YYYYMMDD-XXXX
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        const rand = Math.floor(1000 + Math.random() * 9000);
        const poNumber = `PO-${yyyy}${mm}${dd}-${rand}`;

        const totalAmount = payload.items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);

        // 1. Insert Purchase Order
        const { data: poData, error: poError } = await supabase
            .from('purchase_orders')
            .insert({
                project_id: payload.projectId,
                po_number: poNumber,
                supplier_name: payload.supplierName,
                supplier_contact: payload.supplierContact || '',
                total_amount: totalAmount,
                status: 'pending',
                delivery_date: payload.deliveryDate || null,
                special_instructions: payload.specialInstructions || '',
                created_by: user.id,
            })
            .select()
            .single();

        if (poError) throw poError;

        // 2. Insert items
        const itemsPayload = payload.items.map(item => ({
            po_id: poData.id,
            material_id: item.materialId || null,
            material_name: item.materialName,
            quantity: item.quantity,
            unit_price: item.unitPrice,
            total_price: item.quantity * item.unitPrice,
        }));

        const { error: itemsError } = await supabase
            .from('purchase_order_items')
            .insert(itemsPayload);

        if (itemsError) throw itemsError;

        return { success: true, data: poData }
    } catch (error: any) {
        console.error('Error creating purchase order:', error)
        return { success: false, error: error.message }
    }
}

export async function approvePurchaseOrder(poId: string) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // 1. Fetch the Purchase Order and its items
        const { data: po, error: poFetchError } = await supabase
            .from('purchase_orders')
            .select('*, purchase_order_items(*)')
            .eq('id', poId)
            .single();

        if (poFetchError || !po) throw new Error('Purchase order not found');
        if (po.status !== 'pending') throw new Error('Only pending purchase orders can be approved');

        // Fetch project's site_id
        const { data: projectData, error: projFetchError } = await supabase
            .from('projects')
            .select('site_id')
            .eq('id', po.project_id)
            .single();

        if (projFetchError || !projectData) throw new Error('Associated project or site not found');
        const siteId = projectData.site_id;

        // 2. Update status of the PO to approved
        const { error: poUpdateError } = await supabase
            .from('purchase_orders')
            .update({
                status: 'approved',
                approved_by: user.id,
                updated_at: new Date().toISOString()
            })
            .eq('id', poId);

        if (poUpdateError) throw poUpdateError;

        // 3. Increment stock levels in materials table
        const items = po.purchase_order_items || [];
        for (const item of items) {
            if (item.material_id) {
                // Fetch current stock level
                const { data: material, error: matFetchError } = await supabase
                    .from('materials')
                    .select('current_stock')
                    .eq('id', item.material_id)
                    .single();

                if (!matFetchError && material) {
                    const newStock = Number(material.current_stock || 0) + Number(item.quantity);
                    await supabase
                        .from('materials')
                        .update({ current_stock: newStock, updated_at: new Date().toISOString() })
                        .eq('id', item.material_id);
                }
            } else {
                // Try matching by name in same project
                const { data: existingMat } = await supabase
                    .from('materials')
                    .select('id, current_stock')
                    .eq('site_id', siteId)
                    .eq('name', item.material_name)
                    .maybeSingle();

                if (existingMat) {
                    await supabase
                        .from('purchase_order_items')
                        .update({ material_id: existingMat.id })
                        .eq('id', item.id);

                    const newStock = Number(existingMat.current_stock || 0) + Number(item.quantity);
                    await supabase
                        .from('materials')
                        .update({ current_stock: newStock, updated_at: new Date().toISOString() })
                        .eq('id', existingMat.id);
                } else {
                    // Create new material
                    const { data: newMat, error: createMatError } = await supabase
                        .from('materials')
                        .insert({
                            site_id: siteId,
                            name: item.material_name,
                            category: 'other',
                            unit_of_measurement: 'piece',
                            current_stock: Number(item.quantity),
                            minimum_stock_level: 0,
                            unit_cost: Number(item.unit_price),
                            supplier_name: po.supplier_name,
                            supplier_contact: po.supplier_contact || '',
                            image_url: ''
                        })
                        .select()
                        .single();

                    if (!createMatError && newMat) {
                        await supabase
                            .from('purchase_order_items')
                            .update({ material_id: newMat.id })
                            .eq('id', item.id);
                    }
                }
            }
        }

        return { success: true }
    } catch (error: any) {
        console.error('Error approving purchase order:', error)
        return { success: false, error: error.message }
    }
}
