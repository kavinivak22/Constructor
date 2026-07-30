'use server';

import { createClient } from '@/utils/supabase/server';
import { cookies } from 'next/headers';

export type TransferRecord = {
  id: string;
  transfer_number: string;
  source_type: 'warehouse' | 'project';
  source_id: string | null;
  source_name?: string;
  destination_type: 'warehouse' | 'project';
  destination_id: string | null;
  destination_name?: string;
  item_name: string;
  category: 'material' | 'tool' | 'machinery';
  quantity: number;
  unit: string;
  notes?: string;
  created_at: string;
};

export async function executeStockTransfer(data: {
  sourceType: 'warehouse' | 'project';
  sourceId?: string | null;
  destinationType: 'warehouse' | 'project';
  destinationId?: string | null;
  category: 'material' | 'tool' | 'machinery';
  itemName: string;
  quantity: number;
  unit?: string;
  notes?: string;
}) {
  const supabase = await createClient();

  try {
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Unauthorized');

    if (!data.itemName || data.quantity <= 0) {
      throw new Error('Valid item name and positive quantity are required');
    }

    const transferNum = `TRF-${Math.floor(100000 + Math.random() * 900000)}`;

    // 1. DEDUCT FROM SOURCE
    if (data.sourceType === 'warehouse') {
      // Deduct from global materials table where site_id is null or master warehouse
      const { data: existingMat } = await supabase
        .from('materials')
        .select('id, current_stock')
        .eq('name', data.itemName)
        .is('site_id', null)
        .single();

      if (existingMat) {
        const newQty = Math.max(0, Number(existingMat.current_stock || 0) - data.quantity);
        await supabase
          .from('materials')
          .update({ current_stock: newQty })
          .eq('id', existingMat.id);
      }
    } else if (data.sourceType === 'project' && data.sourceId) {
      // Deduct from project site
      const { data: project } = await supabase
        .from('projects')
        .select('site_id')
        .eq('id', data.sourceId)
        .single();

      if (project?.site_id) {
        const { data: existingSiteMat } = await supabase
          .from('materials')
          .select('id, current_stock')
          .eq('name', data.itemName)
          .eq('site_id', project.site_id)
          .single();

        if (existingSiteMat) {
          const newQty = Math.max(0, Number(existingSiteMat.current_stock || 0) - data.quantity);
          await supabase
            .from('materials')
            .update({ current_stock: newQty })
            .eq('id', existingSiteMat.id);
        }
      }
    }

    // 2. ADD TO DESTINATION
    if (data.destinationType === 'warehouse') {
      const { data: existingWarehouseMat } = await supabase
        .from('materials')
        .select('id, current_stock')
        .eq('name', data.itemName)
        .is('site_id', null)
        .single();

      if (existingWarehouseMat) {
        const newQty = Number(existingWarehouseMat.current_stock || 0) + data.quantity;
        await supabase
          .from('materials')
          .update({ current_stock: newQty })
          .eq('id', existingWarehouseMat.id);
      } else {
        await supabase
          .from('materials')
          .insert({
            name: data.itemName,
            category: data.category,
            current_stock: data.quantity,
            unit_of_measurement: data.unit || 'Units',
            site_id: null
          });
      }
    } else if (data.destinationType === 'project' && data.destinationId) {
      const { data: project } = await supabase
        .from('projects')
        .select('site_id')
        .eq('id', data.destinationId)
        .single();

      if (project?.site_id) {
        const { data: existingSiteMat } = await supabase
          .from('materials')
          .select('id, current_stock')
          .eq('name', data.itemName)
          .eq('site_id', project.site_id)
          .single();

        if (existingSiteMat) {
          const newQty = Number(existingSiteMat.current_stock || 0) + data.quantity;
          await supabase
            .from('materials')
            .update({ current_stock: newQty })
            .eq('id', existingSiteMat.id);
        } else {
          await supabase
            .from('materials')
            .insert({
              name: data.itemName,
              category: data.category,
              current_stock: data.quantity,
              unit_of_measurement: data.unit || 'Units',
              site_id: project.site_id
            });
        }
      }
    }

    // 3. Insert Audit Record into stock_transfers / material_logs
    await supabase.from('material_logs').insert({
      material_id: null,
      user_id: user.id,
      change_amount: data.quantity,
      purpose: `${transferNum}: Transferred ${data.quantity} ${data.unit || 'Units'} of ${data.itemName} (${data.sourceType} -> ${data.destinationType}). ${data.notes || ''}`
    });

    return { success: true, transferNumber: transferNum };
  } catch (error: any) {
    console.error('Error executing stock transfer:', error);
    return { success: false, error: error.message };
  }
}
