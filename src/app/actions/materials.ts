// Client action module

import { createClient } from '@/utils/supabase/client';
import { createNotification } from './notifications'

export async function getProjectMaterials(projectId: string) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Fetch project's site_id first
        const { data: project, error: projectError } = await supabase
            .from('projects')
            .select('site_id')
            .eq('id', projectId)
            .single()

        if (projectError || !project) throw new Error('Project not found')
        const siteId = project.site_id

        const { data, error } = await supabase
            .from('materials')
            .select('*')
            .eq('site_id', siteId)
            .order('name', { ascending: true })

        if (error) throw error

        const mappedData = (data || []).map((m: any) => ({
            id: m.id,
            project_id: projectId, // Map to requested projectId for compatibility
            site_id: m.site_id,
            name: m.name,
            category: m.category || '',
            quantity: Number(m.current_stock || 0),
            min_quantity: Number(m.minimum_stock_level || 0),
            unit: m.unit_of_measurement,
            supplier: m.supplier_name || '',
            cost: Number(m.unit_cost || 0),
            created_at: m.created_at
        }));

        return { success: true, data: mappedData }
    } catch (error: any) {
        console.error('Error fetching project materials:', error)
        return { success: false, error: error.message }
    }
}

export interface MaterialPurchaseDetails {
    unitCost?: number;
    totalCost?: number;
    supplier?: string;
    invoiceNo?: string;
    date?: string;
    updateMaterialUnitCost?: boolean;
}

export interface ParsedMaterialLog {
    id: string;
    change_amount: number;
    purpose: string;
    raw_purpose: string;
    created_at: string;
    users?: {
        displayName: string;
        email: string;
    };
    isPurchase?: boolean;
    unitCost?: number;
    totalCost?: number;
    supplier?: string;
    invoiceNo?: string;
    purchaseDate?: string;
}

export interface MaterialPurchaseStats {
    latestPurchaseRate: number;
    avgPurchaseCost: number;
    totalPurchasedQty: number;
    totalPurchasedCost: number;
    purchaseCount: number;
    rateState: 'higher' | 'normal' | 'lower';
    percentageDiff: number;
}

export async function updateMaterialStock(
    materialId: string, 
    newQuantity: number, 
    purpose: string,
    purchaseDetails?: MaterialPurchaseDetails
) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // 1. Get current material details
        const { data: material, error: fetchError } = await supabase
            .from('materials')
            .select('*, site:sites(name, company_id)')
            .eq('id', materialId)
            .single()

        if (fetchError || !material) throw new Error('Material not found')

        const currentQuantity = Number(material.current_stock || 0)
        const changeAmount = newQuantity - currentQuantity

        // 2. Update the quantity & optionally unit_cost / supplier
        const updatePayload: Record<string, any> = { current_stock: newQuantity };
        if (purchaseDetails && purchaseDetails.updateMaterialUnitCost !== false && purchaseDetails.unitCost !== undefined && Number(purchaseDetails.unitCost) > 0) {
            updatePayload.unit_cost = Number(purchaseDetails.unitCost);
        }
        if (purchaseDetails?.supplier && purchaseDetails.supplier.trim()) {
            updatePayload.supplier_name = purchaseDetails.supplier.trim();
        }

        const { error: updateError } = await supabase
            .from('materials')
            .update(updatePayload)
            .eq('id', materialId)

        if (updateError) throw updateError

        // 3. Log the change with purchase metadata if restock/add
        let logPurpose = purpose;
        if (purchaseDetails && (purchaseDetails.unitCost !== undefined || purchaseDetails.totalCost !== undefined)) {
            const unitCost = Number(purchaseDetails.unitCost || 0);
            const totalCost = Number(purchaseDetails.totalCost !== undefined ? purchaseDetails.totalCost : (Math.max(0, changeAmount) * unitCost));
            logPurpose = JSON.stringify({
                action: 'purchase',
                unitCost,
                totalCost,
                supplier: purchaseDetails.supplier || material.supplier_name || '',
                invoiceNo: purchaseDetails.invoiceNo || '',
                purchaseDate: purchaseDetails.date || '',
                note: purpose || 'Restock purchase'
            });
        }

        const logInsertPayload: Record<string, any> = {
            material_id: materialId,
            user_id: user.id,
            change_amount: changeAmount,
            purpose: logPurpose
        };

        if (purchaseDetails?.date) {
            logInsertPayload.created_at = new Date(`${purchaseDetails.date}T12:00:00`).toISOString();
        }

        const { error: logError } = await supabase
            .from('material_logs')
            .insert(logInsertPayload);

        if (logError) {
            console.error('Error creating material log:', logError)
        }

        // 4. Check for low stock
        const minQuantity = Number(material.minimum_stock_level || 0)
        if (newQuantity <= minQuantity) {
            // Find a sibling project associated with this site to construct notification URL
            const { data: siblingProject } = await supabase
                .from('projects')
                .select('id')
                .eq('site_id', material.site_id)
                .limit(1)
                .maybeSingle();

            const linkProjectId = siblingProject?.id || '';

            await createNotification(
                user.id,
                'Low Stock Alert',
                `Stock for ${material.name} at site ${material.site?.name} is low (${newQuantity} ${material.unit_of_measurement}).`,
                'warning',
                linkProjectId ? `/projects/${linkProjectId}/materials` : '/inventory'
            )
        }

        return { success: true }
    } catch (error: any) {
        console.error('Error updating material stock:', error)
        return { success: false, error: error.message }
    }
}

export async function getMaterialLogs(materialId: string) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data, error } = await supabase
            .from('material_logs')
            .select('*, users(displayName:display_name, email)')
            .eq('material_id', materialId)
            .order('created_at', { ascending: false })

        if (error) throw error

        const mappedLogs: ParsedMaterialLog[] = (data || []).map((log: any) => {
            let parsed: any = null;
            if (typeof log.purpose === 'string' && log.purpose.trim().startsWith('{')) {
                try {
                    parsed = JSON.parse(log.purpose);
                } catch {
                    parsed = null;
                }
            }
            return {
                id: log.id,
                change_amount: log.change_amount,
                purpose: parsed?.note || log.purpose,
                raw_purpose: log.purpose,
                created_at: log.created_at,
                users: log.users,
                isPurchase: parsed?.action === 'purchase' || (parsed && parsed.unitCost !== undefined),
                unitCost: parsed?.unitCost !== undefined ? Number(parsed.unitCost) : undefined,
                totalCost: parsed?.totalCost !== undefined ? Number(parsed.totalCost) : undefined,
                supplier: parsed?.supplier,
                invoiceNo: parsed?.invoiceNo,
                purchaseDate: parsed?.purchaseDate,
            };
        });

        return { success: true, data: mappedLogs }
    } catch (error: any) {
        console.error('Error fetching material logs:', error)
        return { success: false, error: error.message }
    }
}

export async function getMaterialsPurchaseStats(materialIds: string[]): Promise<Record<string, MaterialPurchaseStats>> {
    if (!materialIds || materialIds.length === 0) return {};
    const supabase = await createClient();

    try {
        const { data: logs, error } = await supabase
            .from('material_logs')
            .select('material_id, change_amount, purpose, created_at')
            .in('material_id', materialIds)
            .order('created_at', { ascending: true }); // chronological

        if (error) {
            console.error('Error fetching material logs for stats:', error);
            return {};
        }

        const statsMap: Record<string, MaterialPurchaseStats> = {};
        const logsByMat: Record<string, any[]> = {};
        for (const log of logs || []) {
            if (!logsByMat[log.material_id]) logsByMat[log.material_id] = [];
            logsByMat[log.material_id].push(log);
        }

        for (const mId of materialIds) {
            const mLogs = logsByMat[mId] || [];
            let totalQty = 0;
            let totalCost = 0;
            let latestRate: number | null = null;
            let purchaseCount = 0;

            for (const log of mLogs) {
                let parsed: any = null;
                if (typeof log.purpose === 'string' && log.purpose.trim().startsWith('{')) {
                    try {
                        parsed = JSON.parse(log.purpose);
                    } catch {
                        parsed = null;
                    }
                }

                if (parsed && (parsed.action === 'purchase' || parsed.unitCost !== undefined)) {
                    const qty = Number(log.change_amount || 0);
                    const uCost = Number(parsed.unitCost || 0);
                    const batchCost = Number(parsed.totalCost !== undefined ? parsed.totalCost : qty * uCost);

                    if (qty > 0 && uCost > 0) {
                        totalQty += qty;
                        totalCost += batchCost;
                        latestRate = uCost;
                        purchaseCount++;
                    }
                }
            }

            if (purchaseCount > 0 && totalQty > 0) {
                const avgCost = totalCost / totalQty;
                const finalLatestRate = latestRate ?? avgCost;
                const diff = avgCost > 0 ? ((finalLatestRate - avgCost) / avgCost) * 100 : 0;

                let rateState: 'higher' | 'normal' | 'lower' = 'normal';
                if (diff > 2.0) rateState = 'higher';
                else if (diff < -2.0) rateState = 'lower';

                statsMap[mId] = {
                    latestPurchaseRate: finalLatestRate,
                    avgPurchaseCost: avgCost,
                    totalPurchasedQty: totalQty,
                    totalPurchasedCost: totalCost,
                    purchaseCount,
                    rateState,
                    percentageDiff: Math.round(diff * 10) / 10
                };
            }
        }

        return statsMap;
    } catch (err) {
        console.error('Error computing purchase stats:', err);
        return {};
    }
}

export async function getInventoryMaterials() {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Fetch project IDs user is member of
        const { data: membersData, error: membersError } = await supabase
            .from('project_members')
            .select('project_id')
            .eq('user_id', user.id);

        if (membersError) throw membersError;
        const projectIds = membersData?.map(m => m.project_id) || [];
        if (projectIds.length === 0) return { success: true, data: [] }

        // Fetch their associated site IDs
        const { data: projectsData, error: projectsError } = await supabase
            .from('projects')
            .select('site_id')
            .in('id', projectIds);

        if (projectsError) throw projectsError;

        const siteIds = Array.from(new Set(projectsData?.map(p => p.site_id).filter(Boolean))) as string[];
        if (siteIds.length === 0) return { success: true, data: [] }

        // Fetch materials for these sites
        const { data, error } = await supabase
            .from('materials')
            .select('*, site:sites(name)')
            .in('site_id', siteIds)
            .order('name', { ascending: true });

        if (error) throw error;

        // Map to client Material type
        const mappedData = data.map((m: any) => ({
            id: m.id,
            name: m.name,
            category: m.category || '',
            unit: m.unit_of_measurement,
            currentStock: Number(m.current_stock || 0),
            minStock: Number(m.minimum_stock_level || 0),
            costPerUnit: Number(m.unit_cost || 0),
            supplier: m.supplier_name || '',
            siteId: m.site_id,
            siteName: m.site?.name || '',
            projectId: m.site_id, // fallback for compatibility
            projectName: m.site?.name || '' // fallback for compatibility
        }));

        return { success: true, data: mappedData }
    } catch (error: any) {
        console.error('Error fetching inventory materials:', error)
        return { success: false, error: error.message }
    }
}

export async function addInventoryMaterial(payload: {
    name: string;
    category: string;
    supplier: string;
    unit: string;
    currentStock: number;
    minStock: number;
    costPerUnit: number;
    projectId: string;
}) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Fetch project's site_id first
        const { data: project, error: projectError } = await supabase
            .from('projects')
            .select('site_id')
            .eq('id', payload.projectId)
            .single();

        if (projectError || !project || !project.site_id) {
            throw new Error('Project or associated Site not found');
        }

        const dbPayload = {
            site_id: project.site_id,
            name: payload.name,
            category: payload.category,
            unit_of_measurement: payload.unit,
            current_stock: payload.currentStock,
            minimum_stock_level: payload.minStock,
            unit_cost: payload.costPerUnit,
            supplier_name: payload.supplier,
            supplier_contact: '',
            image_url: ''
        };

        const { data, error } = await supabase
            .from('materials')
            .insert([dbPayload])
            .select()
            .single();

        if (error) throw error;

        return { success: true, data }
    } catch (error: any) {
        console.error('Error adding inventory material:', error)
        return { success: false, error: error.message }
    }
}
