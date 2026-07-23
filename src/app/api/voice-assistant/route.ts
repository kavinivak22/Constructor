import { NextResponse } from 'next/server';
import { CONSTRUCTOR_AI_TOOLS } from '@/lib/ai-tools/registry';
import { executeVoiceQueryAction } from '@/app/actions/voice-assistant';

export async function POST(req: Request) {
  try {
    const { transcript, language } = await req.json();

    if (!transcript || typeof transcript !== 'string') {
      return NextResponse.json({ error: 'Transcript is required' }, { status: 400 });
    }

    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      return NextResponse.json({ error: 'GEMINI_API_KEY is not set' }, { status: 500 });
    }

    const systemPrompt = `You are Constructor Voice AI, an intelligent Construction Management Voice Assistant for builders in India.
Your job is to understand natural language voice prompts spoken in English or Tamil (including Tanglish) and select the most appropriate AI Tool.

Available Tools:
${JSON.stringify(CONSTRUCTOR_AI_TOOLS, null, 2)}

Instructions:
1. If the user is asking a data query (e.g. "How much did Mani get paid?", "What is cement stock?", "Show project financials"), return toolName starting with "query_".
2. If the user wants to log attendance, work, materials, payments, expenses, return toolName starting with "stage_".
3. If the user wants to go to a page (e.g., "Take me to contractor accounts"), return "navigate_app_page".
4. Extract parameters accurately (names, roles like Mason/Helper, quantities, wage rates, payment amounts in INR).
5. Always provide summary En and summary Ta for audio speech feedback.

Return JSON in this exact structure:
{
  "type": "query" | "stage_action" | "navigation" | "general_chat",
  "toolName": "name_of_tool",
  "params": { ...extracted parameters },
  "summaryEn": "English speech response",
  "summaryTa": "Tamil speech response"
}`;

    const userPrompt = `User Spoken Language: ${language === 'ta' ? 'Tamil' : 'English'}\nUser Transcript: "${transcript}"`;

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;

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

    if (!res.ok) {
      const errText = await res.text();
      console.error('Gemini API Error:', errText);
      return NextResponse.json({ error: 'Failed to call Gemini API' }, { status: 500 });
    }

    const geminiData = await res.json();
    const rawContent = geminiData.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!rawContent) {
      return NextResponse.json({ error: 'Empty response from AI model' }, { status: 500 });
    }

    const parsed = JSON.parse(rawContent);

    // If it's a query tool, execute query action immediately to fetch data for speech answer
    if (parsed.type === 'query' && parsed.toolName) {
      const queryRes = await executeVoiceQueryAction(parsed.toolName, parsed.params || {});
      if (queryRes.success) {
        parsed.summaryEn = queryRes.message || parsed.summaryEn;
        parsed.summaryTa = queryRes.messageTa || parsed.summaryTa;
        parsed.queryData = queryRes.data;
      }
    }

    return NextResponse.json(parsed);
  } catch (err: any) {
    console.error('Voice Assistant API Error:', err);
    return NextResponse.json({ error: err?.message || 'Server error' }, { status: 500 });
  }
}
