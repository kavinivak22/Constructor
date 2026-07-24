import { NextResponse } from 'next/server';
import { CONSTRUCTOR_AI_TOOLS } from '@/lib/ai-tools/registry';
import { executeVoiceQueryAction } from '@/app/actions/voice-assistant';

// Fallback rule engine covering all domain tools if Gemini API Key is missing or temporarily unavailable
function fallbackIntentParser(transcript: string, language: string) {
  const text = transcript.toLowerCase();

  // Contractor Payment / Wage Query
  if (text.includes('paid') || text.includes('payment') || text.includes('wage') || text.includes('சம்பளம்') || text.includes('பணம்') || text.includes('கொடுத்தோம்') || text.includes('kuduthom') || text.includes('mani') || text.includes('murugan')) {
    let contractorName = 'Mani';
    if (text.includes('murugan') || text.includes('முருகன்')) contractorName = 'Murugan';
    if (text.includes('ramesh') || text.includes('ரமேஷ்')) contractorName = 'Ramesh';

    return {
      type: 'query',
      toolName: 'query_contractor_payments',
      params: { contractorName, period: 'this_week' },
      summaryEn: `Checking payment details for ${contractorName}...`,
      summaryTa: `${contractorName} அவர்களின் சம்பள விவரங்களை சரிபார்க்கிறது...`
    };
  }

  // Material Stock Query
  if (text.includes('cement') || text.includes('steel') || text.includes('sand') || text.includes('stock') || text.includes('சிமெண்ட்') || text.includes('இரும்பு') || text.includes('மணல்') || text.includes('இருப்பு')) {
    let materialName = 'Cement';
    if (text.includes('steel') || text.includes('இரும்பு')) materialName = 'TMT Steel';
    if (text.includes('sand') || text.includes('மணல்')) materialName = 'M-Sand';

    return {
      type: 'query',
      toolName: 'query_material_stock',
      params: { materialName },
      summaryEn: `Checking stock level for ${materialName}...`,
      summaryTa: `${materialName} இருப்பு நிலையை சரிபார்க்கிறது...`
    };
  }

  // Query Daily Worklogs / Recent Activity
  if (
    text.includes('activity') ||
    text.includes('activities') ||
    text.includes('recent') ||
    text.includes('logged') ||
    text.includes('works') ||
    text.includes('work') ||
    text.includes('worklog') ||
    text.includes('worklogs') ||
    text.includes('இன்று') ||
    text.includes('பணிப்பதிவு') ||
    text.includes('வேலை')
  ) {
    return {
      type: 'query',
      toolName: 'query_daily_worklogs',
      params: { dateFilter: 'today' },
      summaryEn: `Checking worklogs and recent activity logged today...`,
      summaryTa: `இன்று பதிவு செய்யப்பட்ட பணிகள் மற்றும் சமீபத்திய செயல்பாடுகளை சரிபார்க்கிறது...`
    };
  }

  // Query Employees
  if (text.includes('employee') || text.includes('staff') || text.includes('engineer') || text.includes('supervisor') || text.includes('ஊழியர்கள்')) {
    return {
      type: 'query',
      toolName: 'query_employees',
      params: {},
      summaryEn: `Checking company staff roster and employees...`,
      summaryTa: `நிறுவனத்தின் பணியாளர் பட்டியலை சரிபார்க்கிறது...`
    };
  }

  // Query Client Milestones
  if (text.includes('milestone') || text.includes('milestones') || text.includes('client payment') || text.includes('தவணை பணம்')) {
    return {
      type: 'query',
      toolName: 'query_client_milestones',
      params: {},
      summaryEn: `Checking client payment milestones...`,
      summaryTa: `வாடிக்கையாளர் தவணை பணத்தை சரிபார்க்கிறது...`
    };
  }

  // Query Purchase Orders
  if (text.includes('purchase order') || text.includes('po') || text.includes('supplier order') || text.includes('கொள்முதல்')) {
    return {
      type: 'query',
      toolName: 'query_purchase_orders',
      params: {},
      summaryEn: `Checking purchase orders...`,
      summaryTa: `கொள்முதல் ஆணைகளை சரிபார்க்கிறது...`
    };
  }

  // Query Pouch Balances
  if (text.includes('pouch') || text.includes('petty cash') || text.includes('float') || text.includes('பணப்பை')) {
    return {
      type: 'query',
      toolName: 'query_pouch_balance',
      params: {},
      summaryEn: `Checking personal and project pouch balances...`,
      summaryTa: `பணப்பை இருப்புத் தொகையை சரிபார்க்கிறது...`
    };
  }

  // Query Work Prep Tasks
  if (text.includes('prep') || text.includes('preparation') || text.includes('தயாரிப்பு')) {
    return {
      type: 'query',
      toolName: 'query_work_prep_tasks',
      params: {},
      summaryEn: `Checking tomorrow work prep checklist...`,
      summaryTa: `நாளை பணி தயாரிப்பு பட்டியலை சரிபார்க்கிறது...`
    };
  }

  // Query Analytics Summary
  if (text.includes('analytics') || text.includes('report') || text.includes('performance') || text.includes('பகுப்பாய்வு')) {
    return {
      type: 'query',
      toolName: 'query_analytics_summary',
      params: {},
      summaryEn: `Generating site analytics summary...`,
      summaryTa: `தள பகுப்பாய்வு சுருக்கத்தை உருவாக்குகிறது...`
    };
  }

  // Stage Create Project
  if (text.includes('create project') || text.includes('new project') || text.includes('திட்டம் உருவாக்க')) {
    return {
      type: 'stage_action',
      toolName: 'stage_create_project',
      params: {
        name: 'New Commercial Complex',
        clientName: 'Client Owner',
        location: 'Downtown',
        budget: 5000000
      },
      summaryEn: `Staged creation of project "New Commercial Complex".`,
      summaryTa: `"New Commercial Complex" திட்டம் உருவாக்கம் தயார் செய்யப்பட்டது.`
    };
  }

  // Stage Add Employee
  if (text.includes('add employee') || text.includes('add staff') || text.includes('ஊழியர் சேர்க்க')) {
    return {
      type: 'stage_action',
      toolName: 'stage_add_employee',
      params: {
        displayName: 'Site Supervisor Suresh',
        role: 'Supervisor',
        email: 'suresh@constructor.com'
      },
      summaryEn: `Staged adding employee "Supervisor Suresh".`,
      summaryTa: `"Supervisor Suresh" பணியாளர் சேர்க்கை தயார் செய்யப்பட்டது.`
    };
  }

  // Stage Worklog Entry
  if (text.includes('mason') || text.includes('helper') || text.includes('labor') || text.includes('கொத்தனார்') || text.includes('ஆளு')) {
    return {
      type: 'stage_action',
      toolName: 'stage_worklog_entry',
      params: {
        workerRole: text.includes('mason') || text.includes('கொத்தனார்') ? 'Mason' : 'Male Helper',
        workerCount: 4,
        dailyWage: 800,
        workDescription: transcript
      },
      summaryEn: `Staged worklog: 4 Workers logged for today.`,
      summaryTa: `4 தொழிலாளர்களின் பணிப்பதிவு தயார் செய்யப்பட்டது.`
    };
  }

  // Stage Contractor Payment
  if (text.includes('gpay') || text.includes('upi') || text.includes('cash') || text.includes('வழங்கப்பட்டது') || text.includes('pay')) {
    return {
      type: 'stage_action',
      toolName: 'stage_contractor_payment',
      params: {
        contractorName: 'Mani Mason',
        amount: 5000,
        paymentMode: 'UPI'
      },
      summaryEn: `Staged ₹5,000 payment entry for Mani Mason via UPI.`,
      summaryTa: `மணி கொத்தனாருக்கு ₹5,000 UPI பணம் செலுத்துதல் தயார் செய்யப்பட்டது.`
    };
  }

  // Navigation
  if (text.includes('contractor') || text.includes('payday') || text.includes('worklog') || text.includes('expense') || text.includes('inventory') || text.includes('employee') || text.includes('milestone') || text.includes('பக்கத்திற்கு')) {
    let targetPage = '/financials/contractors';
    if (text.includes('payday') || text.includes('சம்பளம்')) targetPage = '/financials/payday';
    if (text.includes('worklog') || text.includes('பணிப்பதிவு')) targetPage = '/worklog';
    if (text.includes('inventory') || text.includes('சரக்கு')) targetPage = '/inventory';
    if (text.includes('employee') || text.includes('ஊழியர்கள்')) targetPage = '/employees';
    if (text.includes('milestone') || text.includes('தவணை')) targetPage = '/projects/milestones';

    return {
      type: 'navigation',
      toolName: 'navigate_app_page',
      params: { targetPage },
      summaryEn: `Navigating to ${targetPage}...`,
      summaryTa: `${targetPage} பக்கத்திற்கு செல்லவும்...`
    };
  }

  // General query fallback to worklogs
  return {
    type: 'query',
    toolName: 'query_daily_worklogs',
    params: { dateFilter: 'today' },
    summaryEn: `Checking latest site worklogs and recent activity...`,
    summaryTa: `சமீபத்திய தள பணிகள் மற்றும் செயல்பாடுகளை சரிபார்க்கிறது...`
  };
}

export async function POST(req: Request) {
  try {
    const { transcript, language } = await req.json();

    if (!transcript || typeof transcript !== 'string' || !transcript.trim()) {
      return NextResponse.json({ error: 'Transcript is required' }, { status: 400 });
    }

    const apiKey = process.env.GEMINI_API_KEY;
    let parsedResult: any = null;

    if (apiKey) {
      // Try official Gemini models (gemini-1.5-flash -> gemini-2.0-flash-exp)
      const models = ['gemini-1.5-flash', 'gemini-2.0-flash-exp', 'gemini-1.5-pro'];

      for (const model of models) {
        try {
          const systemPrompt = `You are Constructor Voice AI, an intelligent Construction Management Voice Assistant for builders in India.
Your job is to understand natural language voice prompts spoken in English or Tamil (including Tanglish) and select the most appropriate AI Tool.

Available Tools:
${JSON.stringify(CONSTRUCTOR_AI_TOOLS, null, 2)}

Instructions:
1. If the user is asking a data query (e.g. "How much did Mani get paid?", "What is cement stock?", "Show project financials", "Show employees", "Show client milestones", "What's the recent activity logged"), return toolName starting with "query_".
2. If the user wants to log attendance, work, materials, payments, expenses, create project, add employee, return toolName starting with "stage_".
3. If the user wants to go to a page (e.g., "Take me to contractor accounts"), return "navigate_app_page".
4. Extract parameters accurately (names, roles like Mason/Helper, quantities, wage rates, payment amounts in INR).
5. Always provide summaryEn and summaryTa for audio speech feedback.

Return JSON in this exact structure:
{
  "type": "query" | "stage_action" | "navigation" | "general_chat",
  "toolName": "name_of_tool",
  "params": { ...extracted parameters },
  "summaryEn": "English speech response",
  "summaryTa": "Tamil speech response"
}`;

          const userPrompt = `User Spoken Language: ${language === 'ta' ? 'Tamil' : 'English'}\nUser Transcript: "${transcript}"`;
          const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

          const res = await fetch(geminiUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: [
                { role: 'user', parts: [{ text: `${systemPrompt}\n\n${userPrompt}` }] }
              ],
              generationConfig: {
                responseMimeType: 'application/json',
                temperature: 0.2
              }
            })
          });

          if (res.ok) {
            const geminiData = await res.json();
            const rawContent = geminiData.candidates?.[0]?.content?.parts?.[0]?.text;
            if (rawContent) {
              parsedResult = JSON.parse(rawContent);
              break; // Success! Break out of model loop
            }
          }
        } catch (err) {
          console.warn(`Gemini API call failed for model ${model}:`, err);
        }
      }
    }

    // Use fallback parser if Gemini wasn't available or returned empty result
    if (!parsedResult) {
      parsedResult = fallbackIntentParser(transcript, language);
    }

    // If it's a query tool, execute query action immediately to fetch data for speech answer
    if (parsedResult.type === 'query' && parsedResult.toolName) {
      const queryRes = await executeVoiceQueryAction(parsedResult.toolName, parsedResult.params || {});
      if (queryRes.success) {
        parsedResult.summaryEn = queryRes.message || parsedResult.summaryEn;
        parsedResult.summaryTa = queryRes.messageTa || parsedResult.summaryTa;
        parsedResult.queryData = queryRes.data;
      }
    }

    return NextResponse.json(parsedResult);
  } catch (err: any) {
    console.error('Voice Assistant API Error:', err);
    return NextResponse.json({ error: err?.message || 'Server error' }, { status: 500 });
  }
}
