import { NextResponse } from 'next/server';
import { CONSTRUCTOR_AI_TOOLS } from '@/lib/ai-tools/registry';
import { executeVoiceQueryAction } from '@/app/actions/voice-assistant';

// Warm, conversational fallback rule engine covering all domain tools if Gemini API Key is missing or temporarily unavailable
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
      summaryEn: `Hey there! Let me pull up ${contractorName}'s payment account for you right away.`,
      summaryTa: `வணக்கம்! ${contractorName} அவர்களின் சம்பளக் கணக்கை உடனடியாக எடுத்துத் தருகிறேன்.`
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
      summaryEn: `Sure thing! Checking current stock level for ${materialName} now.`,
      summaryTa: `கண்டிப்பாக! ${materialName} பொருட்களின் தற்போதைய இருப்பை சரிபார்க்கிறேன்.`
    };
  }

  // Query Daily Worklogs / Recent Activity / Historical Logs
  if (
    text.includes('activity') ||
    text.includes('activities') ||
    text.includes('recent') ||
    text.includes('logged') ||
    text.includes('last') ||
    text.includes('previous') ||
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
      summaryEn: `Gladly! Fetching recent site worklogs and site activities for you.`,
      summaryTa: `மகிழ்ச்சியுடன்! சமீபத்திய பணிப்பதிவுகள் மற்றும் தள செயல்பாடுகளை எடுத்து வருகிறேன்.`
    };
  }

  // Query Employees
  if (text.includes('employee') || text.includes('staff') || text.includes('engineer') || text.includes('supervisor') || text.includes('ஊழியர்கள்')) {
    return {
      type: 'query',
      toolName: 'query_employees',
      params: {},
      summaryEn: `Here is the staff roster and employee list for your company!`,
      summaryTa: `இதோ உங்கள் நிறுவனத்தின் பணியாளர் பட்டியல்!`
    };
  }

  // Query Client Milestones
  if (text.includes('milestone') || text.includes('milestones') || text.includes('client payment') || text.includes('தவணை பணம்')) {
    return {
      type: 'query',
      toolName: 'query_client_milestones',
      params: {},
      summaryEn: `Checking client payment milestones for your projects!`,
      summaryTa: `உங்கள் திட்டங்களின் வாடிக்கையாளர் தவணை பணத்தை சரிபார்க்கிறேன்!`
    };
  }

  // Query Purchase Orders
  if (text.includes('purchase order') || text.includes('po') || text.includes('supplier order') || text.includes('கொள்முதல்')) {
    return {
      type: 'query',
      toolName: 'query_purchase_orders',
      params: {},
      summaryEn: `Let me fetch your active purchase orders right away.`,
      summaryTa: `உங்கள் கொள்முதல் ஆணைகளை உடனடியாக எடுத்து வருகிறேன்.`
    };
  }

  // Query Pouch Balances
  if (text.includes('pouch') || text.includes('petty cash') || text.includes('float') || text.includes('பணப்பை')) {
    return {
      type: 'query',
      toolName: 'query_pouch_balance',
      params: {},
      summaryEn: `Here is your current personal and project pouch cash balance.`,
      summaryTa: `இதோ உங்கள் தற்போதைய பணப்பை இருப்புத் தொகை.`
    };
  }

  // Query Work Prep Tasks
  if (text.includes('prep') || text.includes('preparation') || text.includes('தயாரிப்பு')) {
    return {
      type: 'query',
      toolName: 'query_work_prep_tasks',
      params: {},
      summaryEn: `Let's review tomorrow's site work prep checklist!`,
      summaryTa: `நாளைக்கான பணி தயாரிப்பு பட்டியலை பார்ப்போம்!`
    };
  }

  // Query Analytics Summary
  if (text.includes('analytics') || text.includes('report') || text.includes('performance') || text.includes('பகுப்பாய்வு')) {
    return {
      type: 'query',
      toolName: 'query_analytics_summary',
      params: {},
      summaryEn: `Here is your overall site performance and cost analytics summary.`,
      summaryTa: `இதோ உங்கள் தளத்தின் ஒட்டுமொத்த செயல்பாட்டு பகுப்பாய்வு சுருக்கம்.`
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
      summaryEn: `Got it! I've staged the new project "New Commercial Complex" for your confirmation.`,
      summaryTa: `சரிங்க! "New Commercial Complex" புதிய திட்டத்தை தயார் செய்து வைத்துள்ளேன்.`
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
      summaryEn: `Awesome! Staged Supervisor Suresh to your team roster.`,
      summaryTa: `சிறப்பு! மேலாளர் சுரேஷ் அவர்களை குழுவில் சேர்க்க தயார் செய்துள்ளேன்.`
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
      summaryEn: `Done! Staged 4 Workers present today for your review.`,
      summaryTa: `சரிங்க! இன்று 4 தொழிலாளர்களின் பணிப்பதிவை தயார் செய்துள்ளேன்.`
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
      summaryEn: `Perfect! Staged ₹5,000 UPI payment voucher for Mani Mason.`,
      summaryTa: `அருமை! மணி கொத்தனாருக்கு ₹5,000 UPI பணம் செலுத்துதலை தயார் செய்துள்ளேன்.`
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
      summaryEn: `Taking you to ${targetPage} right now!`,
      summaryTa: `உங்களை ${targetPage} பக்கத்திற்கு அழைத்துச் செல்கிறேன்!`
    };
  }

  // General warm query fallback
  return {
    type: 'query',
    toolName: 'query_daily_worklogs',
    params: { dateFilter: 'today' },
    summaryEn: `Hey friend! Let me pull up recent site worklogs and site activities for you.`,
    summaryTa: `வணக்கம் நண்பா! சமீபத்திய தள பணிகள் மற்றும் செயல்பாடுகளை எடுத்து வருகிறேன்.`
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
      const models = ['gemini-1.5-flash', 'gemini-2.0-flash-exp', 'gemini-1.5-pro'];

      for (const model of models) {
        try {
          const systemPrompt = `You are Constructor Voice AI, a warm, friendly, highly empathetic AI co-pilot and companion for construction engineers and builders in India.
Speak in a warm, conversational, friendly tone—like a trusted work friend! Avoid robotic explanations.

Available Tools:
${JSON.stringify(CONSTRUCTOR_AI_TOOLS, null, 2)}

Instructions:
1. If the user asks a data query (e.g. "How much did Mani get paid?", "What is cement stock?", "What's the recent activity logged", "Show employees"), set toolName starting with "query_".
2. If the user wants to log attendance, work, materials, payments, expenses, create project, add employee, set toolName starting with "stage_".
3. If the user wants to navigate (e.g., "Take me to contractor accounts"), return "navigate_app_page".
4. Provide warm, friendly conversational speech responses in summaryEn (English) and summaryTa (Tamil).

Return JSON in this exact structure:
{
  "type": "query" | "stage_action" | "navigation" | "general_chat",
  "toolName": "name_of_tool",
  "params": { ...extracted parameters },
  "summaryEn": "Warm conversational English response",
  "summaryTa": "Warm conversational Tamil response"
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
                temperature: 0.3
              }
            })
          });

          if (res.ok) {
            const geminiData = await res.json();
            const rawContent = geminiData.candidates?.[0]?.content?.parts?.[0]?.text;
            if (rawContent) {
              parsedResult = JSON.parse(rawContent);
              break;
            }
          }
        } catch (err) {
          console.warn(`Gemini API call failed for model ${model}:`, err);
        }
      }
    }

    if (!parsedResult) {
      parsedResult = fallbackIntentParser(transcript, language);
    }

    if (parsedResult.type === 'query' && parsedResult.toolName) {
      const queryRes = await executeVoiceQueryAction(parsedResult.toolName, parsedResult.params || {});
      if (queryRes.success) {
        // Prepend friendly conversational intro if not present
        const prefixEn = language === 'ta' ? '' : 'Hey there! ';
        const prefixTa = language === 'ta' ? 'வணக்கம்! ' : '';
        parsedResult.summaryEn = prefixEn + (queryRes.message || parsedResult.summaryEn);
        parsedResult.summaryTa = prefixTa + (queryRes.messageTa || parsedResult.summaryTa);
        parsedResult.queryData = queryRes.data;
      }
    }

    return NextResponse.json(parsedResult);
  } catch (err: any) {
    console.error('Voice Assistant API Error:', err);
    return NextResponse.json({ error: err?.message || 'Server error' }, { status: 500 });
  }
}
