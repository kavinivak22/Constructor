import { NextResponse } from 'next/server';
import { CONSTRUCTOR_AI_TOOLS } from '@/lib/ai-tools/registry';
import { executeVoiceQueryAction } from '@/app/actions/voice-assistant';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

// Warm, conversational fallback rule engine covering all domain tools if Gemini API Key is missing or temporarily unavailable
function fallbackIntentParser(transcript: string, language: string) {
  const text = transcript.toLowerCase();

  // 1. Contractor Payment / Transaction Query (Evaluated FIRST!)
  if (
    text.includes('transaction') ||
    text.includes('transactions') ||
    text.includes('paid') ||
    text.includes('payment') ||
    text.includes('payments') ||
    text.includes('wage') ||
    text.includes('wages') ||
    text.includes('transfer') ||
    text.includes('spend') ||
    text.includes('spent') ||
    text.includes('expense') ||
    text.includes('expenses') ||
    text.includes('payout') ||
    text.includes('சம்பளம்') ||
    text.includes('பணம்') ||
    text.includes('கொடுத்தோம்') ||
    text.includes('kuduthom')
  ) {
    let contractorName = '';
    if (text.includes('mani') || text.includes('மணி')) contractorName = 'Mani';
    if (text.includes('murugan') || text.includes('முருகன்')) contractorName = 'Murugan';
    if (text.includes('ramesh') || text.includes('ரமேஷ்')) contractorName = 'Ramesh';

    return {
      type: 'query',
      toolName: 'query_contractor_payments',
      params: { contractorName, period: 'this_week' },
      summaryEn: contractorName
        ? `Hey there! Checking payment details for ${contractorName}...`
        : `Hey there! Fetching the most recent payment transactions...`,
      summaryTa: contractorName
        ? `${contractorName} அவர்களின் சம்பளக் கணக்கை சரிபார்க்கிறேன்...`
        : `சமீபத்திய பணப் பரிவர்த்தனைகளை சரிபார்க்கிறேன்...`
    };
  }

  // 2. Material Stock Query
  if (
    text.includes('cement') ||
    text.includes('steel') ||
    text.includes('sand') ||
    text.includes('stock') ||
    text.includes('inventory') ||
    text.includes('சிமெண்ட்') ||
    text.includes('இரும்பு') ||
    text.includes('மணல்') ||
    text.includes('இருப்பு')
  ) {
    let materialName = '';
    if (text.includes('cement') || text.includes('சிமெண்ட்')) materialName = 'Cement';
    if (text.includes('steel') || text.includes('இரும்பு')) materialName = 'TMT Steel';
    if (text.includes('sand') || text.includes('மணல்')) materialName = 'M-Sand';

    return {
      type: 'query',
      toolName: 'query_material_stock',
      params: { materialName },
      summaryEn: materialName
        ? `Checking current stock level for ${materialName}...`
        : `Checking inventory stock levels...`,
      summaryTa: materialName
        ? `${materialName} இருப்பு நிலையை சரிபார்க்கிறேன்...`
        : `பொருட்களின் இருப்பு நிலையை சரிபார்க்கிறேன்...`
    };
  }

  // 3. Query Daily Worklogs / Recent Activity / Historical Logs (Explicit work activity keywords)
  if (
    text.includes('worklog') ||
    text.includes('worklogs') ||
    text.includes('activity') ||
    text.includes('activities') ||
    text.includes('work logged') ||
    text.includes('works logged') ||
    text.includes('logged work') ||
    text.includes('logged activity') ||
    text.includes('site work') ||
    text.includes('இன்று பணிப்பதிவு') ||
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

  // 4. Query Employees
  if (text.includes('employee') || text.includes('staff') || text.includes('engineer') || text.includes('supervisor') || text.includes('ஊழியர்கள்')) {
    return {
      type: 'query',
      toolName: 'query_employees',
      params: {},
      summaryEn: `Here is the staff roster and employee list for your company!`,
      summaryTa: `இதோ உங்கள் நிறுவனத்தின் பணியாளர் பட்டியல்!`
    };
  }

  // 5. Query Client Milestones
  if (text.includes('milestone') || text.includes('milestones') || text.includes('client payment') || text.includes('தவணை பணம்')) {
    return {
      type: 'query',
      toolName: 'query_client_milestones',
      params: {},
      summaryEn: `Checking client payment milestones for your projects!`,
      summaryTa: `உங்கள் திட்டங்களின் வாடிக்கையாளர் தவணை பணத்தை சரிபார்க்கிறேன்!`
    };
  }

  // 6. Query Purchase Orders
  if (text.includes('purchase order') || text.includes('po') || text.includes('supplier order') || text.includes('கொள்முதல்')) {
    return {
      type: 'query',
      toolName: 'query_purchase_orders',
      params: {},
      summaryEn: `Let me fetch your active purchase orders right away.`,
      summaryTa: `உங்கள் கொள்முதல் ஆணைகளை உடனடியாக எடுத்து வருகிறேன்.`
    };
  }

  // 7. Query Pouch Balances
  if (text.includes('pouch') || text.includes('petty cash') || text.includes('float') || text.includes('பணப்பை')) {
    return {
      type: 'query',
      toolName: 'query_pouch_balance',
      params: {},
      summaryEn: `Here is your current personal and project pouch cash balance.`,
      summaryTa: `இதோ உங்கள் தற்போதைய பணப்பை இருப்புத் தொகை.`
    };
  }

  // 8. Query Work Prep Tasks
  if (text.includes('prep') || text.includes('preparation') || text.includes('தயாரிப்பு')) {
    return {
      type: 'query',
      toolName: 'query_work_prep_tasks',
      params: {},
      summaryEn: `Let's review tomorrow's site work prep checklist!`,
      summaryTa: `நாளைக்கான பணி தயாரிப்பு பட்டியலை பார்ப்போம்!`
    };
  }

  // 9. Query Analytics Summary
  if (text.includes('analytics') || text.includes('report') || text.includes('performance') || text.includes('பகுப்பாய்வு')) {
    return {
      type: 'query',
      toolName: 'query_analytics_summary',
      params: {},
      summaryEn: `Here is your overall site performance and cost analytics summary.`,
      summaryTa: `இதோ உங்கள் தளத்தின் ஒட்டுமொத்த செயல்பாட்டு பகுப்பாய்வு சுருக்கம்.`
    };
  }

  // General query fallback to worklogs
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

    const apiKey = process.env.GEMINI_API_KEY || process.env.NEXT_PUBLIC_GEMINI_API_KEY;
    let parsedResult: any = null;

    if (apiKey) {
      const models = ['gemini-2.5-flash-native-audio-preview-12-2025', 'gemini-2.0-flash-exp', 'gemini-1.5-flash'];

      for (const model of models) {
        try {
          const systemPrompt = `You are Constructor Voice AI, a warm, friendly, highly empathetic AI co-pilot and companion for construction engineers and builders in India.
Speak in a warm, conversational, friendly tone—like a trusted work friend! Avoid robotic explanations.

Available Tools:
${JSON.stringify(CONSTRUCTOR_AI_TOOLS, null, 2)}

Instructions:
1. If the user asks a data query (e.g. "How much did Mani get paid?", "What is the recent payment/transaction made", "What is cement stock?", "What's the recent activity logged", "Show employees"), set toolName starting with "query_".
2. If the user wants to log attendance, work, materials, payments, expenses, create project, add employee, set toolName starting with "stage_".
3. If the user wants to navigate (e.g., "Take me to contractor accounts"), return "navigate_app_page".
4. Provide warm, friendly conversational speech responses in summaryEn (English) and summaryTa (Tamil).
5. DO NOT default or insert fake example contractor names (like "Mani") unless the user explicitly speaks that name.

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
