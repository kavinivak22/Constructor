'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Mic, MicOff, Volume2, Sparkles, X, CheckCircle2, ChevronUp, ChevronDown, Loader2 } from 'lucide-react';
import { useI18n } from '@/lib/i18n-context';
import { BatchConfirmModal } from './batch-confirm-modal';
import type { StagedVoiceAction } from '@/lib/ai-tools/registry';
import { executeBatchVoiceActions } from '@/app/actions/voice-assistant';
import { useToast } from '@/hooks/use-toast';

export function VoiceBar() {
  const { language, t } = useI18n();
  const router = useRouter();
  const { toast } = useToast();

  const [isOpen, setIsOpen] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [lastResponse, setLastResponse] = useState<{ summaryEn: string; summaryTa: string; type: string } | null>(null);
  
  const [stagedActions, setStagedActions] = useState<StagedVoiceAction[]>([]);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false);
  const [isExecutingBatch, setIsExecutingBatch] = useState(false);

  const recognitionRef = useRef<any>(null);

  // Initialize SpeechRecognition
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    const recog = new SpeechRecognition();
    recog.continuous = false;
    recog.interimResults = true;
    recog.lang = language === 'ta' ? 'ta-IN' : 'en-IN';

    recog.onresult = (event: any) => {
      let current = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        current += event.results[i][0].transcript;
      }
      setTranscript(current);
    };

    recog.onend = () => {
      setIsListening(false);
    };

    recog.onerror = (event: any) => {
      console.error('Speech recognition error:', event.error);
      setIsListening(false);
    };

    recognitionRef.current = recog;
  }, [language]);

  // Update speech recognition language when i18n language changes
  useEffect(() => {
    if (recognitionRef.current) {
      recognitionRef.current.lang = language === 'ta' ? 'ta-IN' : 'en-IN';
    }
  }, [language]);

  // Handle Speech-to-Speech (TTS) verbal answer
  const speakVerbalResponse = (text: string) => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = language === 'ta' ? 'ta-IN' : 'en-IN';
    utterance.rate = 0.95;
    window.speechSynthesis.speak(utterance);
  };

  const toggleListening = () => {
    if (!recognitionRef.current) {
      toast({
        title: 'Voice Search Unsupported',
        description: 'Web Speech API is not supported in this browser. Please use Chrome/Edge.',
        variant: 'destructive'
      });
      return;
    }

    if (isListening) {
      recognitionRef.current.stop();
      setIsListening(false);
      if (transcript.trim()) {
        processVoiceCommand(transcript.trim());
      }
    } else {
      setTranscript('');
      setLastResponse(null);
      setIsOpen(true);
      setIsListening(true);
      try {
        recognitionRef.current.start();
      } catch (err) {
        console.error('Recognition start error:', err);
      }
    }
  };

  const processVoiceCommand = async (text: string) => {
    setIsProcessing(true);
    try {
      const res = await fetch('/api/voice-assistant', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ transcript: text, language })
      });

      const data = await res.json();
      setIsProcessing(false);

      if (data.error) {
        toast({ title: 'AI Error', description: data.error, variant: 'destructive' });
        return;
      }

      setLastResponse({
        summaryEn: data.summaryEn || '',
        summaryTa: data.summaryTa || '',
        type: data.type || 'general_chat'
      });

      // Speak response verbally in current language
      const verbalText = language === 'ta' ? data.summaryTa || data.summaryEn : data.summaryEn || data.summaryTa;
      if (verbalText) {
        speakVerbalResponse(verbalText);
      }

      // If navigation command, trigger router push
      if (data.type === 'navigation' && data.params?.targetPage) {
        router.push(data.params.targetPage);
      }

      // If mutation tool, stage action for final batch confirmation upon closing Assistant mode
      if (data.type === 'stage_action' && data.toolName) {
        const newAction: StagedVoiceAction = {
          id: String(Date.now()),
          toolName: data.toolName,
          summary: data.summaryEn || text,
          summaryTa: data.summaryTa || text,
          params: data.params || {},
          timestamp: new Date().toLocaleTimeString()
        };
        setStagedActions(prev => [newAction, ...prev]);
      }
    } catch (err: any) {
      setIsProcessing(false);
      console.error('Failed to process voice command:', err);
    }
  };

  const handleCloseAssistantMode = () => {
    if (stagedActions.length > 0) {
      setIsBatchModalOpen(true);
    } else {
      setIsOpen(false);
    }
  };

  const handleRemoveStagedAction = (id: string) => {
    setStagedActions(prev => prev.filter(a => a.id !== id));
  };

  const handleConfirmAllBatchActions = async () => {
    setIsExecutingBatch(true);
    const res = await executeBatchVoiceActions(stagedActions);
    setIsExecutingBatch(false);

    if (res.success) {
      toast({
        title: language === 'ta' ? 'அனைத்து செயல்களும் சேமிக்கப்பட்டன!' : 'All Voice Actions Executed!',
        description: language === 'ta' ? `${stagedActions.length} குரல் பதிவுகள் வெற்றி அடைந்தன.` : `${stagedActions.length} staged voice changes committed successfully.`
      });
      setStagedActions([]);
      setIsBatchModalOpen(false);
      setIsOpen(false);
    } else {
      toast({
        title: 'Error',
        description: 'Failed to execute some voice actions.',
        variant: 'destructive'
      });
    }
  };

  return (
    <>
      {/* Floating Mic Orb / Assistant Controller */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-3">
        {/* Expanded Voice Control Card */}
        {isOpen && (
          <Card className="w-80 sm:w-96 glass border-primary/30 shadow-2xl overflow-hidden backdrop-blur-2xl animate-in slide-in-from-bottom-5">
            <CardContent className="p-4 space-y-3">
              <div className="flex items-center justify-between border-b border-border/40 pb-2">
                <div className="flex items-center gap-2">
                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary text-primary-foreground">
                    <Sparkles className="h-4 w-4" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold font-headline text-foreground">Constructor Voice AI</h4>
                    <span className="text-[10px] text-muted-foreground uppercase tracking-wider font-semibold">
                      {language === 'ta' ? 'தமிழ் குரல் உதவி' : 'Bilingual Assistant'}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  {stagedActions.length > 0 && (
                    <Badge
                      variant="secondary"
                      className="cursor-pointer bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30 gap-1 text-xs"
                      onClick={() => setIsBatchModalOpen(true)}
                    >
                      <CheckCircle2 className="h-3 w-3" />
                      <span>{stagedActions.length} {language === 'ta' ? 'நிலுவை' : 'Staged'}</span>
                    </Badge>
                  )}
                  <Button size="icon" variant="ghost" className="h-7 w-7" onClick={handleCloseAssistantMode}>
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              </div>

              {/* Dynamic Equalizer / Speech Visualizer */}
              <div className="flex items-center justify-center py-3 bg-muted/20 rounded-xl border border-muted/20 relative overflow-hidden">
                {isListening ? (
                  <div className="flex items-center gap-1.5 h-10">
                    <span className="w-1.5 bg-primary rounded-full animate-bounce h-8" />
                    <span className="w-1.5 bg-amber-500 rounded-full animate-bounce [animation-delay:0.2s] h-10" />
                    <span className="w-1.5 bg-primary rounded-full animate-bounce [animation-delay:0.4s] h-6" />
                    <span className="w-1.5 bg-emerald-500 rounded-full animate-bounce [animation-delay:0.1s] h-9" />
                  </div>
                ) : isProcessing ? (
                  <div className="flex items-center gap-2 text-xs font-semibold text-primary py-2">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>{language === 'ta' ? 'குரல் செயலாக்கப்படுகிறது...' : 'Processing AI Tools...'}</span>
                  </div>
                ) : (
                  <p className="text-xs text-muted-foreground text-center py-2 px-4">
                    {language === 'ta'
                      ? 'பேச மைக்கை அழுத்தவும் (எ.கா: "மணி கொத்தனாருக்கு எவ்வளவு கொடுத்தோம்?")'
                      : 'Tap Mic to Speak (e.g. "How much did Mani Mason get paid?")'}
                  </p>
                )}
              </div>

              {/* Transcript Display */}
              {transcript && (
                <div className="bg-background/80 p-2.5 rounded-lg border border-border/50 text-xs italic text-foreground">
                  "{transcript}"
                </div>
              )}

              {/* Verbal AI Response Display */}
              {lastResponse && (
                <div className="bg-primary/10 border border-primary/20 p-2.5 rounded-lg text-xs text-foreground space-y-1">
                  <div className="flex items-center gap-1.5 font-bold text-primary text-[11px] uppercase">
                    <Volume2 className="h-3.5 w-3.5" />
                    <span>{language === 'ta' ? 'பதில்:' : 'AI Response:'}</span>
                  </div>
                  <p>{language === 'ta' ? lastResponse.summaryTa || lastResponse.summaryEn : lastResponse.summaryEn || lastResponse.summaryTa}</p>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Floating Glowing Mic Button */}
        <Button
          size="icon"
          onClick={toggleListening}
          className={`h-14 w-14 rounded-full shadow-2xl transition-all duration-300 ${
            isListening
              ? 'bg-red-500 hover:bg-red-600 ring-4 ring-red-500/30 scale-110'
              : 'bg-primary hover:bg-primary/90 ring-4 ring-primary/20'
          }`}
        >
          {isListening ? (
            <MicOff className="h-6 w-6 text-white animate-pulse" />
          ) : (
            <Mic className="h-6 w-6 text-primary-foreground" />
          )}
        </Button>
      </div>

      {/* Batch Confirm Modal on Mode Close */}
      <BatchConfirmModal
        isOpen={isBatchModalOpen}
        onClose={() => {
          setIsBatchModalOpen(false);
          setIsOpen(false);
        }}
        stagedActions={stagedActions}
        onRemoveAction={handleRemoveStagedAction}
        onConfirmAll={handleConfirmAllBatchActions}
        isExecuting={isExecutingBatch}
      />
    </>
  );
}
