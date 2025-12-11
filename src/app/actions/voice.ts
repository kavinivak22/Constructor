'use server';

import { pipeline } from '@xenova/transformers';
import { google } from '@ai-sdk/google';
import { generateText } from 'ai';
import { z } from 'zod';
import ffmpeg from 'fluent-ffmpeg';
import ffmpegPath from '@ffmpeg-installer/ffmpeg';
import fs from 'fs';
import path from 'path';
import os from 'os';
import { v4 as uuidv4 } from 'uuid';
import { WaveFile } from 'wavefile';

// Configure ffmpeg path
ffmpeg.setFfmpegPath(ffmpegPath.path);

// Cache the model globally in development to avoid reloading on every request
// In production serverless, this might reload, but caching helps in persistent containers
let transcriber: any = null;

async function getTranscriber() {
    if (!transcriber) {
        transcriber = await pipeline('automatic-speech-recognition', 'Xenova/whisper-tiny.en');
    }
    return transcriber;
}

// Intent Schema
const IntentSchema = z.object({
    intent: z.enum(['ADD_WORKLOG', 'ADD_EXPENSE', 'VIEW_MATERIALS', 'UNKNOWN']),
    data: z.record(z.any()).describe('Extracted data fields relevant to the intent'),
    message: z.string().describe('A friendly confirmation message or question if missing info'),
});

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

        // 1. Convert Blob to Buffer
        const buffer = Buffer.from(await audioFile.arrayBuffer());

        // 2. Save to temp file (webm)
        const tempDir = os.tmpdir();
        // Using crypto for random string if uuid not available, but 'uuid' is common. 
        // I'll assume uuid is not installed, so I'll use crypto.randomUUID() if available or Math.
        const runId = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(7);

        const inputPath = path.join(tempDir, `input-${runId}.webm`);
        const outputPath = path.join(tempDir, `output-${runId}.wav`);

        await fs.promises.writeFile(inputPath, buffer);

        console.log('Converting audio...', inputPath);

        // 3. Convert WebM to WAV (16kHz, mono, float32 compatible usually, but Whisper handles wav)
        await new Promise((resolve, reject) => {
            ffmpeg(inputPath)
                .toFormat('wav')
                .audioFrequency(16000)
                .audioChannels(1)
                .on('end', resolve)
                .on('error', (err: any) => reject(err))
                .save(outputPath);
        });

        console.log('Audio converted. Transcribing...');

        // 4. Transcribe
        // Read the WAV file manually since transformers.js in Node doesn't support file paths well without AudioContext
        const wavBuffer = await fs.promises.readFile(outputPath);
        const wav = new WaveFile(wavBuffer);

        // Ensure conversion to 32-bit float
        wav.toBitDepth('32f');

        // Get samples
        let audioData = wav.getSamples();

        // If stereo, take the first channel, but we forced mono via ffmpeg so it should be fine.
        // WaveFile getSamples returns a Float64Array (or similar) or array of them?
        // documentation says: "return the samples as a Float64Array" (if interleaved) or "Array of Float64Arrays" (if not).
        // For mono file, it might be just one array or array of 1 array.
        // Let's normalize it.
        if (Array.isArray(audioData)) {
            if (audioData.length > 0) {
                audioData = audioData[0];
            } else {
                throw new Error("No audio samples found");
            }
        }
        // Transformers.js expects Float32Array
        // WaveFile.toBitDepth('32f') makes it 32-bit float in the buffer, getSamples returns the values.
        // We can create a Float32Array from it.
        const audioTensor = new Float32Array(audioData as any);

        const transcriber = await getTranscriber();
        const output = await transcriber(audioTensor);
        const text = output.text;

        console.log('Transcription:', text);

        // Cleanup temp files
        try {
            await fs.promises.unlink(inputPath);
            await fs.promises.unlink(outputPath);
        } catch (e) {
            console.error("Cleanup error", e);
        }

        if (!text || text.length < 2) {
            return { success: false, error: 'Could not understand audio' };
        }

        // 5. Interpret Intent with Gemini
        // Using generateText with manual JSON parsing to avoid specific SDK schema/mode errors
        const { text: resultText } = await generateText({
            model: google('gemini-2.5-flash'),
            system: `You are an AI assistant for a construction app. You extract structured intents from voice commands.
            Current Date: ${new Date().toISOString().split('T')[0]}
            
            Always return a valid JSON object with:
            - intent: "ADD_WORKLOG" | "ADD_EXPENSE" | "VIEW_MATERIALS" | "UNKNOWN"
            - data: object with extracted fields
            - message: string (confirmation or follow-up question)

            Intents:
            - ADD_WORKLOG: Fields: description, date, hours, project_name.
            - ADD_EXPENSE: Fields: amount, category, description, merchant.
            - VIEW_MATERIALS: Fields: project_name, filter.
            
            Example JSON:
            { "intent": "ADD_EXPENSE", "data": { "amount": 50, "category": "Food" }, "message": "Added 50 for food." }
            `,
            prompt: text,
        });

        console.log(`[Voice] ${runId}: Usage result raw:`, resultText);

        let result;
        try {
            // Clean up markdown code blocks if present
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
            transcript: text
        };

    } catch (error) {
        console.error('Voice processing error:', error);
        return { success: false, error: error instanceof Error ? error.message : 'Unknown error' };
    }
}
