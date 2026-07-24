import { NextResponse } from 'next/server';
import { CONSTRUCTOR_AI_TOOLS } from '@/lib/ai-tools/registry';
import { executeVoiceQueryAction } from '@/app/actions/voice-assistant';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

/**
 * Gemini Live Native Multimodal Audio Gateway
 * Uses native audio model gemini-2.5-flash-native-audio-preview-12-2025 with audio/text modalities
 */
export async function POST(req: Request) {
  try {
    const { pcmAudioBase64, transcript, language } = await req.json();

    const apiKey = process.env.GEMINI_API_KEY || process.env.NEXT_PUBLIC_GEMINI_API_KEY;

    if (!apiKey) {
      return NextResponse.json({ error: 'GEMINI_API_KEY is not configured' }, { status: 400 });
    }

    const systemPrompt = `You are Constructor Live AI, an ultra-intelligent, warm, companionable Construction Management Voice Assistant for builders and engineers in India.
You converse naturally in English or Tamil (including Tanglish). Avoid robotic jargon.

Available App Tools:
${JSON.stringify(CONSTRUCTOR_AI_TOOLS, null, 2)}

Instructions:
1. Understand spoken user voice audio/transcripts and invoke the exact toolName starting with "query_" for questions, or "stage_" for site logs/payments/expenses.
2. Provide spoken speech summaries in summaryEn and summaryTa.
3. Speak numbers as spoken words (e.g., "5 thousand rupees", "10 Lakh rupees") and dates as relative words ("yesterday", "today", "July 23rd").

Return JSON in this structure:
{
  "type": "query" | "stage_action" | "navigation" | "general_chat",
  "toolName": "name_of_tool",
  "params": { ...extracted parameters },
  "summaryEn": "Warm English voice response",
  "summaryTa": "Warm Tamil voice response"
}`;

    const models = [
      'gemini-2.5-flash-native-audio-preview-12-2025',
      'gemini-2.0-flash-exp',
      'gemini-1.5-flash'
    ];

    let parsed: any = null;

    for (const model of models) {
      try {
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

        const parts: any[] = [{ text: systemPrompt }];

        if (pcmAudioBase64) {
          parts.push({
            inlineData: {
              mimeType: 'audio/pcm;rate=16000',
              data: pcmAudioBase64
            }
          });
        } else if (transcript) {
          parts.push({ text: `User Prompt (${language === 'ta' ? 'Tamil' : 'English'}): "${transcript}"` });
        }

        const res = await fetch(geminiUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ role: 'user', parts }],
            generationConfig: {
              responseMimeType: 'application/json',
              temperature: 0.2
            }
          })
        });

        if (res.ok) {
          const data = await res.json();
          const rawContent = data.candidates?.[0]?.content?.parts?.[0]?.text;
          if (rawContent) {
            parsed = JSON.parse(rawContent);
            break;
          }
        }
      } catch (err) {
        console.warn(`Model ${model} call failed:`, err);
      }
    }

    if (!parsed) {
      parsed = {
        type: 'general_chat',
        toolName: '',
        params: {},
        summaryEn: 'I heard you! How can I assist you with site operations?',
        summaryTa: 'நான் கேட்டது! உங்கள் தளப் பணிகளுக்கு நான் எவ்வாறு உதவ வேண்டும்?'
      };
    }

    // Execute server query action immediately for live data response
    if (parsed.type === 'query' && parsed.toolName) {
      const queryResult = await executeVoiceQueryAction(parsed.toolName, parsed.params || {});
      if (queryResult.success) {
        parsed.summaryEn = queryResult.message || parsed.summaryEn;
        parsed.summaryTa = queryResult.messageTa || parsed.summaryTa;
        parsed.queryData = queryResult.data;
      }
    }

    return NextResponse.json(parsed);
  } catch (err: any) {
    console.error('Gemini Live Gateway error:', err);
    return NextResponse.json({ error: err?.message || 'Server error' }, { status: 500 });
  }
}
