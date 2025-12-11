'use client';

import { useState, useRef, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Mic, MicOff, Loader2, X, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { processVoiceCommand } from '@/app/actions/voice';
import { useToast } from '@/hooks/use-toast';

export function VoiceAssistant() {
    const [isOpen, setIsOpen] = useState(false);
    const [isRecording, setIsRecording] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [transcript, setTranscript] = useState('');
    const [partialTranscript, setPartialTranscript] = useState('');
    const [commandResult, setCommandResult] = useState<{ intent: string, message: string } | null>(null);

    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const chunksRef = useRef<Blob[]>([]);
    const recognitionRef = useRef<any>(null); // For Web Speech API
    const { toast } = useToast();

    // Initialize Web Speech API if available (Progressive Enhancement)
    useEffect(() => {
        if (typeof window !== 'undefined') {
            const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
            if (SpeechRecognition) {
                const recognition = new SpeechRecognition();
                recognition.continuous = true;
                recognition.interimResults = true;
                recognition.lang = 'en-US';

                recognition.onresult = (event: any) => {
                    let interimTranscript = '';
                    for (let i = event.resultIndex; i < event.results.length; ++i) {
                        if (event.results[i].isFinal) {
                            setTranscript(prev => prev + event.results[i][0].transcript);
                        } else {
                            interimTranscript += event.results[i][0].transcript;
                        }
                    }
                    setPartialTranscript(interimTranscript);
                };

                recognitionRef.current = recognition;
            }
        }
    }, []);

    const startRecording = async () => {
        try {
            setIsOpen(true); // Open the overlay
            setCommandResult(null);
            setTranscript('');
            setPartialTranscript('');

            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
            mediaRecorderRef.current = mediaRecorder;
            chunksRef.current = [];

            mediaRecorder.ondataavailable = (e) => {
                if (e.data.size > 0) chunksRef.current.push(e.data);
            };

            mediaRecorder.onstop = async () => {
                setIsProcessing(true);
                // Stop speech recognition visualizer
                if (recognitionRef.current) recognitionRef.current.stop();

                const audioBlob = new Blob(chunksRef.current, { type: 'audio/webm' });
                stream.getTracks().forEach(track => track.stop());

                const formData = new FormData();
                formData.append('audio', audioBlob);

                try {
                    const result = await processVoiceCommand(formData);

                    if (result.success) {
                        setTranscript(result.transcript || transcript); // Update with server transcript if better
                        setCommandResult({ intent: result.intent || 'Success', message: result.message || 'Done' });

                        // Dispatch event
                        if (typeof window !== 'undefined') {
                            window.dispatchEvent(new CustomEvent('voice-command', {
                                detail: { intent: result.intent, data: result.data }
                            }));
                        }

                        // Auto-close after delay? Or let user close.
                        // Let's keep it open for a moment to show success
                        setTimeout(() => {
                            // Optional: Auto close or clear
                        }, 3000);

                    } else {
                        setCommandResult({ intent: 'Error', message: result.error || "Failed" });
                    }
                } catch (error) {
                    console.error("Voice Error", error);
                    setCommandResult({ intent: 'Error', message: "System error occurred." });
                } finally {
                    setIsProcessing(false);
                    setIsRecording(false);
                }
            };

            mediaRecorder.start();
            if (recognitionRef.current) {
                try {
                    recognitionRef.current.start();
                } catch (e) {
                    console.warn("Speech recognition already started or failed", e);
                }
            }
            setIsRecording(true);

        } catch (err) {
            console.error('Error accessing microphone:', err);
            toast({ title: "Microphone Access Denied", variant: "destructive" });
            setIsOpen(false);
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && isRecording) {
            mediaRecorderRef.current.stop();
        }
    };

    const closeOverlay = () => {
        if (isRecording) stopRecording();
        setIsOpen(false);
    };

    if (!isOpen) {
        return (
            <div className="fixed bottom-24 right-6 z-50 md:bottom-10">
                <Button
                    onClick={startRecording}
                    size="lg"
                    className="rounded-full w-14 h-14 shadow-lg bg-primary hover:bg-primary/90 transition-transform hover:scale-105"
                >
                    <Mic className="h-6 w-6 text-white" />
                </Button>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="relative w-full max-w-md p-6 mx-4 bg-background/90 border border-border/50 rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                {/* Background ambient glow */}
                <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500" />

                <div className="flex flex-col items-center justify-center space-y-8 py-8">

                    {/* Status & Transcript */}
                    <div className="text-center space-y-2 w-full min-h-[100px] flex flex-col justify-center">
                        {commandResult ? (
                            <div className="space-y-2 animate-in slide-in-from-bottom-2">
                                <div className={cn("inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm font-medium",
                                    commandResult.intent === 'Error' ? "bg-red-100 text-red-700" : "bg-green-100 text-green-700")}>
                                    {commandResult.intent === 'Error' ? <X className="w-4 h-4" /> : <Check className="w-4 h-4" />}
                                    {commandResult.intent}
                                </div>
                                <p className="text-lg font-medium text-foreground">{commandResult.message}</p>
                            </div>
                        ) : (
                            <>
                                <h3 className="text-muted-foreground font-medium text-sm">
                                    {isProcessing ? "Processing..." : isRecording ? "Listening..." : "Ready"}
                                </h3>
                                <p className="text-xl font-semibold text-foreground px-4">
                                    {transcript || partialTranscript || (isRecording ? "..." : "")}
                                </p>
                            </>
                        )}
                    </div>

                    {/* Microphone Visualizer / Button */}
                    <div className="relative">
                        {isRecording && (
                            <>
                                <div className="absolute inset-0 rounded-full bg-primary/20 animate-ping" />
                                <div className="absolute inset-[-12px] rounded-full bg-primary/10 animate-pulse delay-75" />
                            </>
                        )}

                        <Button
                            onClick={isRecording ? stopRecording : startRecording}
                            disabled={isProcessing}
                            className={cn(
                                "relative w-24 h-24 rounded-full flex items-center justify-center shadow-xl transition-all duration-300",
                                isRecording ? "bg-red-500 hover:bg-red-600 scale-110" : "bg-primary hover:bg-primary/90",
                                isProcessing && "opacity-80"
                            )}
                        >
                            {isProcessing ? (
                                <Loader2 className="w-10 h-10 text-white animate-spin" />
                            ) : isRecording ? (
                                <MicOff className="w-10 h-10 text-white" />
                            ) : (
                                <Mic className="w-10 h-10 text-white" />
                            )}
                        </Button>
                    </div>

                    <p className="text-xs text-muted-foreground text-center max-w-[200px]">
                        {isProcessing
                            ? "AI is analyzing your command..."
                            : isRecording
                                ? "Tap to stop. Speak clearly."
                                : "Tap microphone to speak."}
                    </p>
                </div>

                {/* Close Button */}
                {!isRecording && !isProcessing && (
                    <button
                        onClick={closeOverlay}
                        className="absolute top-4 right-4 p-2 rounded-full hover:bg-muted/50 transition-colors"
                    >
                        <X className="w-5 h-5 text-muted-foreground" />
                    </button>
                )}
            </div>
        </div>
    );
}
