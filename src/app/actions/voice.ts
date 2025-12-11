'use server';

import { google } from '@ai-sdk/google';
import { generateText } from 'ai';
import { z } from 'zod';
import { v4 as uuidv4 } from 'uuid';

type VoiceResponse = {
    success: boolean;
    intent?: string;
    data?: any;
    message?: string;
    error?: string;
    transcript?: string;
};

export async function processVoiceCommand(formData: FormData): Promise<VoiceResponse> {
    try {
        const audioFile = formData.get('audio') as Blob;
        if (!audioFile) {
            return { success: false, error: 'No audio file provided' };
        }

        const buffer = Buffer.from(await audioFile.arrayBuffer());
        // Convert Buffer to Base64
        const audioBase64 = buffer.toString('base64');

        const runId = uuidv4 ? uuidv4() : Math.random().toString(36).substring(7);
        console.log(`[Voice] ${runId}: Processing native audio command...`);

        // Interpret Intent AND Transcribe with Gemini Native Audio
        const { text: resultText } = await generateText({
            model: google('gemini-2.5-flash'),
            messages: [
                {
                    role: 'user',
                    content: [
                        {
                            type: 'text', text: `
                            You are an AI assistant for a construction app. DO NOT HALLUCINATE.
                            
                            Step 1: Transcribe the audio exactly.
                            Step 2: Extract the structured intent.

                            Current Date: ${new Date().toISOString().split('T')[0]}

                            Always return a valid JSON object with:
                            - transcript: string (The exact transcription of the audio)
                            - intent: "ADD_WORKLOG" | "ADD_EXPENSE" | "VIEW_MATERIALS" | "UNKNOWN"
                            - data: object with extracted fields
                            - message: string (Confirmation or follow-up question)

                            Intents:
                            - ADD_WORKLOG: Fields: description, date, hours, project_name.
                            - ADD_EXPENSE: Fields: amount, category, description, merchant.
                            - VIEW_MATERIALS: Fields: project_name, filter.

                            Example JSON:
                            { 
                                "transcript": "Add 50 dollars for food",
                                "intent": "ADD_EXPENSE", 
                                "data": { "amount": 50, "category": "Food" }, 
                                "message": "Added 50 for food." 
                            }
                        ` },
                        { type: 'file', data: audioBase64, mediaType: 'audio/webm' }
                    ]
                }
            ]
        });

        console.log(`[Voice] ${runId}: Raw AI response:`, resultText);

        let result;
        try {
            const jsonStr = resultText.replace(/```json\n?|\n?```/g, '').trim();
            result = JSON.parse(jsonStr);
        } catch (e) {
            console.error(`[Voice] ${runId}: JSON Parse Error`, e);
            return {
                success: false,
                error: "Failed to parse AI response",
                message: "I heard you, but I couldn't understand the structure."
            };
        }

        return {
            success: true,
            intent: result.intent || 'UNKNOWN',
            data: result.data || {},
            message: result.message || "Processed.",
            transcript: result.transcript || ""
        };

    } catch (error) {
        console.error('Voice processing error:', error);
        return { success: false, error: error instanceof Error ? error.message : 'Unknown error' };
    }
}
