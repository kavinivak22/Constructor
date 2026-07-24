import { NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export async function POST(req: Request) {
  try {
    const { text, language } = await req.json();

    if (!text || typeof text !== 'string' || !text.trim()) {
      return NextResponse.json({ error: 'Text is required' }, { status: 400 });
    }

    const apiKey = process.env.GEMINI_API_KEY;
    const cleanText = text.replace(/[*#_`]/g, '').trim();

    // 1. Try Google Cloud Text-to-Speech API with Studio / Journey / Wavenet HD Neural Voices
    if (apiKey) {
      try {
        const ttsUrl = `https://texttospeech.googleapis.com/v1/text:synthesize?key=${apiKey}`;
        const voiceName = language === 'ta' ? 'ta-IN-Wavenet-A' : 'en-US-Journey-F';

        const res = await fetch(ttsUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            input: { text: cleanText },
            voice: {
              languageCode: language === 'ta' ? 'ta-IN' : 'en-US',
              name: voiceName
            },
            audioConfig: {
              audioEncoding: 'MP3',
              speakingRate: 0.98,
              pitch: 0.0
            }
          })
        });

        if (res.ok) {
          const data = await res.json();
          if (data.audioContent) {
            return NextResponse.json({
              audioBase64: data.audioContent,
              format: 'audio/mp3'
            });
          }
        }
      } catch (err) {
        console.warn('Google Cloud TTS API failed, falling back to neural audio stream:', err);
      }
    }

    // 2. Fallback to HD Neural TTS Audio Stream (Google Neural Translate Voice Stream)
    const langCode = language === 'ta' ? 'ta' : 'en';
    const streamUrl = `https://translate.google.com/translate_tts?client=tw-ob&tl=${langCode}&q=${encodeURIComponent(cleanText.slice(0, 250))}`;

    const audioStreamRes = await fetch(streamUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      }
    });

    if (audioStreamRes.ok) {
      const buffer = await audioStreamRes.arrayBuffer();
      const base64Audio = Buffer.from(buffer).toString('base64');
      return NextResponse.json({
        audioBase64: base64Audio,
        format: 'audio/mp3'
      });
    }

    return NextResponse.json({ error: 'Audio synthesis failed' }, { status: 500 });
  } catch (err: any) {
    console.error('TTS API error:', err);
    return NextResponse.json({ error: err?.message || 'Server error' }, { status: 500 });
  }
}
