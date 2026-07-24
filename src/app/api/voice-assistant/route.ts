import { NextResponse } from 'next/server';
import { CONSTRUCTOR_AI_TOOLS } from '@/lib/ai-tools/registry';
import { executeVoiceQueryAction } from '@/app/actions/voice-assistant';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

/**
 * Pure Gemini Multimodal AI Engine Route
 * Zero default fallback tools, zero hardcoded responses, 100% native Gemini tool execution.
 */
export async function POST(req: Request) {
  try {
    const { transcript, language } = await req.json();

    if (!transcript || typeof transcript !== 'string' || !transcript.trim()) {
      return NextResponse.json({ error: 'Transcript is required' }, { status: 400 });
    }

    const apiKey = process.env.GEMINI_API_KEY || process.env.NEXT_PUBLIC_GEMINI_API_KEY;

    if (!apiKey) {
      return NextResponse.json({
        error: 'Gemini API Key is missing. Please set GEMINI_API_KEY in Vercel environment variables.'
      }, { status: 400 });
    }

    const systemPrompt = `You are Constructor Personal AI, an intelligent, warm, highly capable AI Personal Assistant for construction builders and engineers in India.
You converse naturally like a trusted colleague in English or Tamil (including Tanglish).

CRITICAL INSTRUCTIONS FOR TOOL CALLING:
1. NAVIGATION: When the user asks to navigate, open, go to, or view any page (e.g., "navigate me to daily worklog page", "open contractor payments", "go to inventory", "show projects"), ALWAYS return toolName = "navigate_app_page" with params.targetPage set to:
   - Daily Worklog -> /worklog
   - Contractor Accounts / Payments -> /financials/contractors
   - Weekly Pay-Day -> /financials/payday
   - Materials / Inventory / Reconciliation -> /materials/reconciliation or /inventory
   - Projects -> /projects
   - Employees / Staff -> /employees
   - Client Milestones -> /projects/milestones
   - Work Prep Board -> /work-prep
   - Expenses -> /expenses
   - Personal Pouch -> /personal-pouch
   - Project Pouch -> /project-pouch
   - Team Hub / Messages -> /team-hub

2. DATA QUERIES: When the user asks for data (payments, worklogs, materials, employees, budget), invoke the exact matching "query_" tool. DO NOT invent or assume contractor names, material names, or numbers unless explicitly stated by the user. If no contractor or material name is stated, leave parameter empty so the system queries recent records overall.

3. STAGING MUTATIONS: When the user asks to log, record, stage, or add anything, invoke the matching "stage_" tool.

4. Provide a warm, concise conversational speech summary in summaryEn (English) and summaryTa (Tamil).

Available AI Tools:
${JSON.stringify(CONSTRUCTOR_AI_TOOLS, null, 2)}

Return JSON in this exact schema:
{
  "type": "query" | "stage_action" | "navigation" | "general_chat",
  "toolName": "exact_tool_name",
  "params": { ...extracted parameters },
  "summaryEn": "Warm conversational English response",
  "summaryTa": "Warm conversational Tamil response"
}`;

    const models = [
      'gemini-2.5-flash-native-audio-preview-12-2025',
      'gemini-2.0-flash-exp',
      'gemini-1.5-flash'
    ];

    let parsedResult: any = null;

    for (const model of models) {
      try {
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
        const userPrompt = `User Spoken Language: ${language === 'ta' ? 'Tamil' : 'English'}\nUser Voice Request: "${transcript}"`;

        const res = await fetch(geminiUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [
              { role: 'user', parts: [{ text: `${systemPrompt}\n\n${userPrompt}` }] }
            ],
            generationConfig: {
              responseMimeType: 'application/json',
              temperature: 0.1
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

    if (!parsedResult) {
      return NextResponse.json({ error: 'Failed to process voice request with Gemini model.' }, { status: 500 });
    }

    // Execute server query action immediately for live data response
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
