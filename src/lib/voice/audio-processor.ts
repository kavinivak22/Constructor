/**
 * Web Audio API Processor for Gemini Live Multimodal Audio Streaming
 * Handles 16kHz PCM audio recording from microphone & 24kHz PCM audio playback streaming
 */

export class AudioStreamProcessor {
  private audioCtx: AudioContext | null = null;
  private mediaStream: MediaStream | null = null;
  private scriptProcessor: ScriptProcessorNode | null = null;
  private isRecording = false;

  constructor(private onPcmAudioData: (pcmBase64: string) => void) {}

  async startRecording(): Promise<boolean> {
    try {
      this.audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)({ sampleRate: 16000 });
      this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });

      const source = this.audioCtx.createMediaStreamSource(this.mediaStream);
      this.scriptProcessor = this.audioCtx.createScriptProcessor(4096, 1, 1);

      this.scriptProcessor.onaudioprocess = (event) => {
        if (!this.isRecording) return;
        const inputBuffer = event.inputBuffer.getChannelData(0);
        const pcm16 = this.floatTo16BitPCM(inputBuffer);
        const base64 = this.arrayBufferToBase64(pcm16.buffer as ArrayBuffer);
        this.onPcmAudioData(base64);
      };

      source.connect(this.scriptProcessor);
      this.scriptProcessor.connect(this.audioCtx.destination);
      this.isRecording = true;
      return true;
    } catch (err) {
      console.error('Failed to start audio recording:', err);
      return false;
    }
  }

  stopRecording() {
    this.isRecording = false;
    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect();
      this.scriptProcessor = null;
    }
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
    }
    if (this.audioCtx) {
      this.audioCtx.close();
      this.audioCtx = null;
    }
  }

  private floatTo16BitPCM(output: Float32Array): Int16Array {
    const result = new Int16Array(output.length);
    for (let i = 0; i < output.length; i++) {
      const s = Math.max(-1, Math.min(1, output[i]));
      result[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
    }
    return result;
  }

  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }
}
