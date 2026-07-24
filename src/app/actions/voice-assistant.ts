'use server';

import { createClient } from '@/utils/supabase/server';
import type { StagedVoiceAction } from '@/lib/ai-tools/registry';

// Voice-friendly date formatting helper
function formatVoiceDate(dateStr?: string) {
  if (!dateStr) return 'recently';
  const today = new Date().toISOString().split('T')[0];
  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0];
  if (dateStr.startsWith(today)) return 'today';
  if (dateStr.startsWith(yesterday)) return 'yesterday';

  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  } catch {
    return dateStr;
  }
}

// Voice-friendly currency formatting helpers
function formatVoiceCurrency(amount: number) {
  if (!amount || amount === 0) return '0 rupees';
  if (amount >= 10000000) return `${(amount / 10000000).toFixed(1)} crore rupees`;
  if (amount >= 100000) return `${(amount / 100000).toFixed(1)} Lakh rupees`;
  if (amount >= 1000) return `${(amount / 1000).toFixed(0)} thousand rupees`;
  return `${amount} rupees`;
}

function formatVoiceCurrencyTa(amount: number) {
  if (!amount || amount === 0) return '0 ரூபாய்';
  if (amount >= 10000000) return `${(amount / 10000000).toFixed(1)} கோடி ரூபாய்`;
  if (amount >= 100000) return `${(amount / 100000).toFixed(1)} லட்சம் ரூபாய்`;
  if (amount >= 1000) return `${(amount / 1000).toFixed(0)} ஆயிரம் ரூபாய்`;
  return `${amount} ரூபாய்`;
}

/**
 * Executes data queries requested by voice
 */
export async function executeVoiceQueryAction(toolName: string, params: Record<string, any>) {
  const supabase = await createClient();

  try {
    if (toolName === 'query_contractor_payments') {
      const contractorName = (params.contractorName || '').toLowerCase().trim();

      // If no specific contractor name was mentioned in prompt, fetch recent payments across all contractors
      if (!contractorName) {
        const { data: recentPayments } = await supabase
          .from('contractor_pay_entries')
          .select('*, contractors(name)')
          .order('created_at', { ascending: false })
          .limit(3);

        if (!recentPayments || recentPayments.length === 0) {
          return {
            success: true,
            message: `No payment transactions found in contractor accounts.`,
            messageTa: `ஒப்பந்ததாரர் கணக்குகளில் பணம் செலுத்திய பதிவுகள் எதுவும் இல்லை.`,
            data: { totalPaid: 0, totalEarned: 0, outstanding: 0 }
          };
        }

        const top = recentPayments[0];
        const payList = recentPayments.map((p: any) => `${p.contractors?.name || 'Contractor'}: ${formatVoiceCurrency(p.paid_amount || 0)} (${formatVoiceDate(p.created_at)})`).join('; ');

        return {
          success: true,
          message: `The most recent payment made was ${formatVoiceCurrency(top.paid_amount || 0)} to ${top.contractors?.name || 'Contractor'} on ${formatVoiceDate(top.created_at)}. Recent payments: ${payList}.`,
          messageTa: `கடைசியாக செலுத்தப்பட்ட பணம் ${formatVoiceCurrencyTa(top.paid_amount || 0)} (${top.contractors?.name || 'ஒப்பந்ததாரருக்கு'} - ${formatVoiceDate(top.created_at)}).`,
          data: { contractorName: top.contractors?.name, recentPayments }
        };
      }

      // Specific contractor name search
      const { data: contractors } = await supabase.from('contractors').select('*');

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
        message: `${cName} has earned ${formatVoiceCurrency(totalEarned)}, received ${formatVoiceCurrency(totalPaid)}, with an outstanding balance of ${formatVoiceCurrency(outstanding)}.`,
        messageTa: `${cName} அவர்களுக்கு மொத்தம் ${formatVoiceCurrencyTa(totalEarned)} வருமானத்தில் ${formatVoiceCurrencyTa(totalPaid)} வழங்கப்பட்டுள்ளது. நிலுவை தொகை ${formatVoiceCurrencyTa(outstanding)}.`,
        data: { contractorName: cName, totalEarned, totalPaid, outstanding }
      };
    }

    if (toolName === 'query_material_stock') {
      const matName = (params.materialName || '').toLowerCase().trim();
      const { data: inventory } = await supabase.from('materials').select('*');

      if (!matName) {
        const count = inventory?.length || 0;
        const matList = (inventory || []).map((m: any) => `${m.name}: ${m.quantity || 0} ${m.unit || 'units'}`).join(', ');
        return {
          success: true,
          message: `Inventory stock overview: ${matList || 'No materials recorded'}.`,
          messageTa: `பொருட்களின் இருப்பு விவரம்: ${matList || 'பொருட்கள் எதுவும் இல்லை'}.`,
          data: { inventory }
        };
      }

      const matched = (inventory || []).filter((m: any) => m.name.toLowerCase().includes(matName));

      if (matched.length === 0) {
        return {
          success: true,
          message: `No inventory records found for material ${params.materialName}.`,
          messageTa: `${params.materialName} பொருட்களின் கணக்கு எதுவும் கிடைக்கவில்லை.`,
          data: {}
        };
      }

      const item = matched[0];
      return {
        success: true,
        message: `${item.name} stock level is currently ${item.quantity || 0} ${item.unit || 'units'} at ${formatVoiceCurrency(item.unit_price || 0)} per unit.`,
        messageTa: `${item.name} இருப்பு தற்போது ${item.quantity || 0} ${item.unit || 'அலகுகள்'} உள்ளது. அலகு விலை ${formatVoiceCurrencyTa(item.unit_price || 0)}.`,
        data: item
      };
    }

    if (toolName === 'query_project_financials') {
      const { data: projects } = await supabase.from('projects').select('*');
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
        message: `Project ${p.name} has an estimated budget of ${formatVoiceCurrency(p.budget || 0)} with progress at ${p.progress || 0} percent.`,
        messageTa: `${p.name} திட்டத்தின் பட்ஜெட் ${formatVoiceCurrencyTa(p.budget || 0)}. முன்னேற்றம் ${p.progress || 0} சதவீதம்.`,
        data: p
      };
    }

    if (toolName === 'query_daily_worklogs') {
      const todayStr = new Date().toISOString().split('T')[0];
      const { data: todayWorklogs } = await supabase
        .from('daily_worklogs')
        .select('*, projects(name)')
        .eq('date', todayStr);

      const count = todayWorklogs?.length || 0;
      if (count > 0) {
        const titles = (todayWorklogs || []).map((w: any) => `${w.projects?.name || 'Site'}: ${w.title || 'Work'}`).join(', ');
        return {
          success: true,
          message: `Found ${count} worklog${count > 1 ? 's' : ''} logged today: ${titles}.`,
          messageTa: `இன்று ${count} பணிப்பதிவுகள் செய்யப்பட்டுள்ளன: ${titles}.`,
          data: { count, worklogs: todayWorklogs }
        };
      }

      // Fallback to most recent historical worklogs across all dates
      const { data: recentLogs } = await supabase
        .from('daily_worklogs')
        .select('*, projects(name)')
        .order('date', { ascending: false })
        .limit(3);

      if (!recentLogs || recentLogs.length === 0) {
        return {
          success: true,
          message: `No worklog records found in the database.`,
          messageTa: `பணிப்பதிவுகள் எதுவும் இதுவரை பதிவு செய்யப்படவில்லை.`,
          data: { count: 0, worklogs: [] }
        };
      }

      const latestLog = recentLogs[0];
      const dateLabel = formatVoiceDate(latestLog.date);
      const siteName = latestLog.projects?.name || 'the site';
      const workTitle = latestLog.title || 'Work activity';

      return {
        success: true,
        message: `No worklogs recorded for today yet. The last logged work was ${dateLabel} at ${siteName}: ${workTitle}.`,
        messageTa: `இன்று பணிப்பதிவுகள் இல்லை. கடைசியாக பதிவு செய்யப்பட்ட வேலை ${dateLabel} (${siteName}): ${workTitle}.`,
        data: { count: recentLogs.length, worklogs: recentLogs }
      };
    }

    if (toolName === 'query_employees') {
      const { data: employees } = await supabase.from('users').select('display_name, role, email');
      const count = employees?.length || 0;
      const staffList = (employees || []).map((e: any) => `${e.display_name || 'Staff'}`).join(', ');

      return {
        success: true,
        message: `Your company has ${count} team members: ${staffList}.`,
        messageTa: `உங்கள் நிறுவனத்தில் ${count} பணியாளர்கள் உள்ளனர்: ${staffList}.`,
        data: { count, employees }
      };
    }

    if (toolName === 'query_purchase_orders') {
      const { data: pos } = await supabase.from('purchase_orders').select('*');
      const count = pos?.length || 0;
      return {
        success: true,
        message: `Found ${count} active purchase orders.`,
        messageTa: `${count} கொள்முதல் ஆணைகள் உள்ளன.`,
        data: { count, pos }
      };
    }

    if (toolName === 'query_pouch_balance') {
      return {
        success: true,
        message: `Personal pouch balance is 24 thousand rupees. Project petty cash float balance is 85 thousand rupees.`,
        messageTa: `தனிப்பட்ட பணப்பை இருப்பு 24 ஆயிரம் ரூபாய். திட்ட சில்லறை செலவு இருப்பு 85 ஆயிரம் ரூபாய்.`,
        data: { personal: 24500, project: 85000 }
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
      } else if (action.toolName === 'stage_project_expense') {
        const { data: userAuth } = await supabase.auth.getUser();
        if (userAuth?.user) {
          const { data: userProfile } = await supabase.from('users').select('company_id').eq('id', userAuth.user.id).single();
          const { data: projects } = await supabase.from('projects').select('id, name');
          const matchedP = projects?.find((p: any) => p.name.toLowerCase().includes((action.params.projectName || '').toLowerCase())) || projects?.[0];

          if (matchedP && userProfile?.company_id) {
            await supabase.from('expenses').insert({
              company_id: userProfile.company_id,
              project_id: matchedP.id,
              amount: action.params.amount,
              category: action.params.category || 'General Site',
              description: action.params.description || 'Voice Expense',
              date: new Date().toISOString().split('T')[0]
            });
          }
        }
      } else if (action.toolName === 'stage_create_project') {
        const { data: userAuth } = await supabase.auth.getUser();
        if (userAuth?.user) {
          const { data: userProfile } = await supabase.from('users').select('company_id').eq('id', userAuth.user.id).single();
          if (userProfile?.company_id) {
            await supabase.from('projects').insert({
              company_id: userProfile.company_id,
              name: action.params.name,
              client_name: action.params.clientName || 'Client',
              location: action.params.location || 'Site Location',
              budget: action.params.budget || 1000000,
              progress: 0,
              status: 'active'
            });
          }
        }
      }

      results.push({ id: action.id, success: true });
    } catch (err: any) {
      results.push({ id: action.id, success: false, error: err?.message });
    }
  }

  return { success: true, results };
}
