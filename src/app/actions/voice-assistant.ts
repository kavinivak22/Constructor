'use server';

import { createClient } from '@/utils/supabase/server';
import type { StagedVoiceAction } from '@/lib/ai-tools/registry';

/**
 * Executes a data query requested by voice (e.g. "How much did Mani Mason get paid this week?")
 */
export async function executeVoiceQueryAction(toolName: string, params: Record<string, any>) {
  const supabase = await createClient();

  try {
    if (toolName === 'query_contractor_payments') {
      const contractorName = (params.contractorName || '').toLowerCase().trim();

      // 1. Fetch contractor accounts matching name
      const { data: contractors } = await supabase
        .from('contractors')
        .select('*');

      const matchedContractors = (contractors || []).filter((c: any) => 
        c.name.toLowerCase().includes(contractorName) || 
        (c.category && c.category.toLowerCase().includes(contractorName))
      );

      if (matchedContractors.length === 0) {
        return {
          success: true,
          message: `No contractor found matching "${params.contractorName}".`,
          messageTa: `"${params.contractorName}" பெயரில் ஒப்பந்ததாரர் எதுவும் கிடைக்கவில்லை.`,
          data: { totalPaid: 0, totalEarned: 0, outstanding: 0 }
        };
      }

      const contractorIds = matchedContractors.map((c: any) => c.id);

      // Fetch contractor salary / payday entries
      const { data: payEntries } = await supabase
        .from('contractor_pay_entries')
        .select('*')
        .in('contractor_id', contractorIds);

      const totalPaid = (payEntries || []).reduce((sum: number, item: any) => sum + (Number(item.paid_amount) || 0), 0);
      const totalEarned = (payEntries || []).reduce((sum: number, item: any) => sum + (Number(item.rate_amount) || 0) + (Number(item.nmr_amount) || 0), 0);
      const outstanding = Math.max(0, totalEarned - totalPaid);

      const cName = matchedContractors[0].name;

      return {
        success: true,
        message: `${cName} has earned ₹${totalEarned.toLocaleString('en-IN')}, received ₹${totalPaid.toLocaleString('en-IN')}, with an outstanding balance of ₹${outstanding.toLocaleString('en-IN')}.`,
        messageTa: `${cName} அவர்களுக்கு மொத்தம் ₹${totalEarned.toLocaleString('en-IN')} வருமானத்தில் ₹${totalPaid.toLocaleString('en-IN')} வழங்கப்பட்டுள்ளது. நிலுவை தொகை ₹${outstanding.toLocaleString('en-IN')}.`,
        data: { contractorName: cName, totalEarned, totalPaid, outstanding }
      };
    }

    if (toolName === 'query_material_stock') {
      const matName = (params.materialName || '').toLowerCase().trim();

      const { data: inventory } = await supabase
        .from('materials')
        .select('*');

      const matched = (inventory || []).filter((m: any) => 
        m.name.toLowerCase().includes(matName)
      );

      if (matched.length === 0) {
        return {
          success: true,
          message: `No inventory records found for material "${params.materialName}".`,
          messageTa: `"${params.materialName}" பொருட்களின் கணக்கு எதுவும் கிடைக்கவில்லை.`,
          data: {}
        };
      }

      const item = matched[0];
      return {
        success: true,
        message: `${item.name} stock level is currently ${item.quantity || 0} ${item.unit || 'units'} at ₹${item.unit_price || 0} per unit.`,
        messageTa: `${item.name} இருப்பு தற்போது ${item.quantity || 0} ${item.unit || 'அலகுகள்'} உள்ளது. அலகு விலை ₹${item.unit_price || 0}.`,
        data: item
      };
    }

    if (toolName === 'query_project_financials') {
      const { data: projects } = await supabase
        .from('projects')
        .select('*');

      const pName = (params.projectName || '').toLowerCase().trim();
      const matched = (projects || []).filter((p: any) => !pName || p.name.toLowerCase().includes(pName));

      if (matched.length === 0) {
        return {
          success: true,
          message: `No matching project found.`,
          messageTa: `திட்டம் எதுவும் கிடைக்கவில்லை.`,
          data: {}
        };
      }

      const p = matched[0];
      return {
        success: true,
        message: `Project ${p.name} has a budget of ₹${(p.budget || 0).toLocaleString('en-IN')} with progress at ${p.progress || 0}%.`,
        messageTa: `${p.name} திட்டத்தின் பட்ஜெட் ₹${(p.budget || 0).toLocaleString('en-IN')}. முன்னேற்றம் ${p.progress || 0}%.`,
        data: p
      };
    }

    return {
      success: true,
      message: 'Query executed successfully.',
      messageTa: 'கேள்வி வெற்றிகரமாக செயலாக்கப்பட்டது.',
      data: {}
    };
  } catch (err: any) {
    return {
      success: false,
      error: err?.message || 'Failed to execute query.',
      messageTa: 'பிழை ஏற்பட்டது.'
    };
  }
}

/**
 * Batch Executes all Voice Actions confirmed by user when closing Assistant Mode
 */
export async function executeBatchVoiceActions(actions: StagedVoiceAction[]) {
  const supabase = await createClient();
  const results: { id: string; success: boolean; error?: string }[] = [];

  for (const action of actions) {
    try {
      if (action.toolName === 'stage_worklog_entry') {
        const { data: userAuth } = await supabase.auth.getUser();
        if (userAuth?.user) {
          const { data: userProfile } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', userAuth.user.id)
            .single();

          const companyId = userProfile?.company_id;

          // Fetch first project if not specified
          const { data: projects } = await supabase.from('projects').select('id, name');
          const matchedProj = projects?.find((p: any) => p.name.toLowerCase().includes((action.params.projectName || '').toLowerCase())) || projects?.[0];

          if (matchedProj && companyId) {
            await supabase.from('daily_worklogs').insert({
              company_id: companyId,
              project_id: matchedProj.id,
              date: new Date().toISOString().split('T')[0],
              title: action.params.workDescription || `Voice Log: ${action.params.workerRole}`,
              created_by: userAuth.user.id
            });
          }
        }
      } else if (action.toolName === 'stage_contractor_payment') {
        // Record payment in contractor pay entries
        const { data: contractors } = await supabase.from('contractors').select('id, name');
        const matchedC = contractors?.find((c: any) => c.name.toLowerCase().includes((action.params.contractorName || '').toLowerCase())) || contractors?.[0];

        if (matchedC) {
          await supabase.from('contractor_pay_entries').insert({
            contractor_id: matchedC.id,
            paid_amount: action.params.amount,
            notes: action.params.notes || `Voice Payment (${action.params.paymentMode || 'UPI'})`,
            created_at: new Date().toISOString()
          });
        }
      }

      results.push({ id: action.id, success: true });
    } catch (err: any) {
      results.push({ id: action.id, success: false, error: err?.message });
    }
  }

  return { success: true, results };
}
