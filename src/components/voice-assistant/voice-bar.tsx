'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Progress } from '@/components/ui/progress';
import {
  Mic,
  MicOff,
  Volume2,
  Sparkles,
  X,
  CheckCircle2,
  Loader2,
  Send,
  User,
  Bot,
  Building2,
  Boxes,
  Wallet,
  ClipboardList,
  ArrowRight,
  RefreshCw
} from 'lucide-react';
import { useI18n } from '@/lib/i18n-context';
import { BatchConfirmModal } from './batch-confirm-modal';
import type { StagedVoiceAction } from '@/lib/ai-tools/registry';
import { executeBatchVoiceActions } from '@/app/actions/voice-assistant';
import { useToast } from '@/hooks/use-toast';

export interface ChatMessage {
  id: string;
  sender: 'user' | 'ai';
  text: string;
  textTa?: string;
  timestamp: string;
  type?: string;
  toolName?: string;
  queryData?: any;
  params?: any;
}

export function VoiceBar() {
  const { language, t } = useI18n();
  const router = useRouter();
  const { toast } = useToast();

  const [isOpen, setIsOpen] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [autoListenFollowup, setAutoListenFollowup] = useState(true);

  const [stagedActions, setStagedActions] = useState<StagedVoiceAction[]>([]);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false);
  const [isExecutingBatch, setIsExecutingBatch] = useState(false);

  const recognitionRef = useRef<any>(null);
  const latestTranscriptRef = useRef<string>('');
  const chatScrollRef = useRef<HTMLDivElement>(null);

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
      latestTranscriptRef.current = current;
    };

    recog.onend = () => {
      setIsListening(false);
      const capturedText = latestTranscriptRef.current.trim();
      if (capturedText) {
        processVoiceCommand(capturedText);
      }
    };

    recog.onerror = (event: any) => {
      console.error('Speech recognition error:', event.error);
      setIsListening(false);
    };

    recognitionRef.current = recog;
  }, [language]);

  // Update speech recognition language when language changes
  useEffect(() => {
    if (recognitionRef.current) {
      recognitionRef.current.lang = language === 'ta' ? 'ta-IN' : 'en-IN';
    }
  }, [language]);

  // Scroll to bottom when messages update
  useEffect(() => {
    if (chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
    }
  }, [messages]);

  // Handle Real Neural Human Audio Playback (Google Cloud / HD MP3 Audio Stream)
  const audioPlayerRef = useRef<HTMLAudioElement | null>(null);

  const speakVerbalResponse = async (text: string, onEndCallback?: () => void) => {
    if (typeof window === 'undefined') return;

    // Stop any currently playing audio
    if (audioPlayerRef.current) {
      audioPlayerRef.current.pause();
      audioPlayerRef.current = null;
    }
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }

    try {
      const res = await fetch('/api/voice-assistant/tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text, language })
      });

      const data = await res.json();
      if (data.audioBase64) {
        const audioSrc = `data:${data.format || 'audio/mp3'};base64,${data.audioBase64}`;
        const audio = new Audio(audioSrc);
        audioPlayerRef.current = audio;

        audio.onended = () => {
          if (onEndCallback) onEndCallback();
        };
        audio.onerror = () => {
          fallbackBrowserSpeech(text, onEndCallback);
        };

        await audio.play();
        return;
      }
    } catch (err) {
      console.warn('Neural audio fetch failed, falling back to browser voice:', err);
    }

    fallbackBrowserSpeech(text, onEndCallback);
  };

  const fallbackBrowserSpeech = (text: string, onEndCallback?: () => void) => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = language === 'ta' ? 'ta-IN' : 'en-IN';
    utterance.rate = 0.98;
    utterance.onend = () => {
      if (onEndCallback) onEndCallback();
    };
    window.speechSynthesis.speak(utterance);
  };

  const startListening = () => {
    if (!recognitionRef.current) {
      toast({
        title: 'Voice Search Unsupported',
        description: 'Web Speech API is not supported in this browser. Please use Chrome/Edge.',
        variant: 'destructive'
      });
      return;
    }
    setTranscript('');
    latestTranscriptRef.current = '';
    setIsOpen(true);
    setIsListening(true);
    try {
      recognitionRef.current.start();
    } catch (err) {
      console.error('Recognition start error:', err);
    }
  };

  const toggleListening = () => {
    if (isListening) {
      if (recognitionRef.current) recognitionRef.current.stop();
      setIsListening(false);
    } else {
      startListening();
    }
  };

  const processVoiceCommand = async (text: string) => {
    if (!text || !text.trim()) return;

    const userMsgId = String(Date.now());
    const userMsg: ChatMessage = {
      id: userMsgId,
      sender: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages(prev => [...prev, userMsg]);
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

      const verbalText = language === 'ta' ? data.summaryTa || data.summaryEn : data.summaryEn || data.summaryTa;

      const aiMsg: ChatMessage = {
        id: String(Date.now() + 1),
        sender: 'ai',
        text: data.summaryEn || text,
        textTa: data.summaryTa || text,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        type: data.type,
        toolName: data.toolName,
        queryData: data.queryData,
        params: data.params
      };

      setMessages(prev => [...prev, aiMsg]);

      // Speak response verbally with live follow-up auto-listen
      speakVerbalResponse(verbalText, () => {
        if (autoListenFollowup) {
          setTimeout(() => {
            startListening();
          }, 400);
        }
      });

      // Navigation handler
      if (data.type === 'navigation' && data.params?.targetPage) {
        router.push(data.params.targetPage);
      }

      // Stage Action handler
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

  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (transcript.trim()) {
      processVoiceCommand(transcript.trim());
      setTranscript('');
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

  // Render Visual Card Guidance Widgets inside AI messages
  const renderVisualGuidanceCard = (msg: ChatMessage) => {
    if (!msg.toolName && !msg.queryData) return null;

    // 1. Contractor Payment Visual Card
    if (msg.toolName === 'query_contractor_payments' && msg.queryData) {
      const d = msg.queryData;
      const progress = d.totalEarned > 0 ? Math.min(100, Math.round((d.totalPaid / d.totalEarned) * 100)) : 100;

      return (
        <Card className="mt-2 bg-background/80 border-primary/20 p-3 rounded-xl space-y-2 shadow-sm">
          <div className="flex items-center justify-between text-xs font-semibold text-foreground">
            <span className="flex items-center gap-1.5 text-primary">
              <Wallet className="h-4 w-4" /> {d.contractorName || 'Contractor'}
            </span>
            <Badge variant={d.outstanding > 0 ? "destructive" : "outline"} className="text-[10px]">
              ₹{d.outstanding?.toLocaleString('en-IN')} Due
            </Badge>
          </div>
          <div className="space-y-1">
            <div className="flex justify-between text-[11px] text-muted-foreground">
              <span>Paid: ₹{d.totalPaid?.toLocaleString('en-IN')}</span>
              <span>Earned: ₹{d.totalEarned?.toLocaleString('en-IN')}</span>
            </div>
            <Progress value={progress} className="h-2" />
          </div>
        </Card>
      );
    }

    // 2. Daily Worklogs / Recent Activity Visual Card
    if (msg.toolName === 'query_daily_worklogs' && msg.queryData) {
      const logs = msg.queryData.worklogs || [];
      if (logs.length === 0) return null;

      return (
        <Card className="mt-2 bg-background/80 border-primary/20 p-3 rounded-xl space-y-2 shadow-sm">
          <div className="flex items-center justify-between text-xs font-semibold text-foreground border-b border-border/40 pb-1.5">
            <span className="flex items-center gap-1.5 text-primary">
              <ClipboardList className="h-4 w-4" /> {language === 'ta' ? 'சமீபத்திய பணிகள்' : 'Recent Worklogs'}
            </span>
            <Badge variant="outline" className="text-[10px]">{logs.length} Logged</Badge>
          </div>
          <div className="space-y-1.5">
            {logs.slice(0, 3).map((w: any, idx: number) => (
              <div key={idx} className="p-2 rounded-lg bg-muted/40 text-[11px] flex items-center justify-between">
                <div>
                  <p className="font-semibold text-foreground">{w.title || 'Site Work'}</p>
                  <p className="text-[10px] text-muted-foreground">{w.projects?.name || 'Project'} • {w.date}</p>
                </div>
                <Badge variant="secondary" className="text-[9px] bg-primary/10 text-primary">Verified</Badge>
              </div>
            ))}
          </div>
        </Card>
      );
    }

    // 3. Material Stock Visual Card
    if (msg.toolName === 'query_material_stock' && msg.queryData) {
      const item = msg.queryData;
      return (
        <Card className="mt-2 bg-background/80 border-primary/20 p-3 rounded-xl space-y-1.5 shadow-sm">
          <div className="flex items-center justify-between text-xs font-semibold text-foreground">
            <span className="flex items-center gap-1.5 text-amber-500">
              <Boxes className="h-4 w-4" /> {item.name || 'Material'}
            </span>
            <Badge className="bg-emerald-500 text-white text-[10px]">
              {item.quantity || 0} {item.unit || 'Units'}
            </Badge>
          </div>
          <p className="text-[11px] text-muted-foreground">Unit Price: ₹{item.unit_price || 0} / {item.unit || 'unit'}</p>
        </Card>
      );
    }

    // 4. Staged Action Confirmation Chip Card
    if (msg.type === 'stage_action') {
      return (
        <Card className="mt-2 bg-amber-500/10 border-amber-500/30 p-2.5 rounded-xl space-y-1.5 shadow-sm">
          <div className="flex items-center justify-between text-xs font-semibold text-amber-600 dark:text-amber-400">
            <span className="flex items-center gap-1">
              <CheckCircle2 className="h-3.5 w-3.5" /> Staged Action
            </span>
            <Button
              size="sm"
              variant="outline"
              className="h-6 text-[10px] px-2 border-amber-500/40 text-amber-600 hover:bg-amber-500/20"
              onClick={() => setIsBatchModalOpen(true)}
            >
              Review ({stagedActions.length})
            </Button>
          </div>
        </Card>
      );
    }

    return null;
  };

  return (
    <>
      {/* Floating Mic Orb / Assistant Controller */}
      <div className="fixed bottom-24 right-4 sm:bottom-6 sm:right-6 z-[100] flex flex-col items-end gap-3">
        {/* Gemini Live Interactive Conversation Window */}
        {isOpen && (
          <Card className="w-[calc(100vw-2rem)] max-w-sm sm:w-[420px] glass border-primary/40 shadow-2xl overflow-hidden backdrop-blur-2xl animate-in slide-in-from-bottom-5">
            <CardHeader className="p-3 border-b border-border/40 flex flex-row items-center justify-between space-y-0">
              <div className="flex items-center gap-2">
                <div className="relative flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-tr from-primary to-amber-500 text-primary-foreground shadow-md">
                  <Sparkles className="h-4 w-4 animate-spin-slow" />
                  {isListening && (
                    <span className="absolute inset-0 rounded-full bg-primary/40 animate-ping" />
                  )}
                </div>
                <div>
                  <CardTitle className="text-sm font-bold font-headline text-foreground flex items-center gap-1.5">
                    Constructor Live AI
                    <Badge variant="outline" className="text-[9px] px-1 bg-emerald-500/10 text-emerald-500 border-emerald-500/30">
                      Live
                    </Badge>
                  </CardTitle>
                  <span className="text-[10px] text-muted-foreground uppercase tracking-wider font-semibold">
                    {language === 'ta' ? 'தமிழ் குரல் தோழன்' : 'Conversational Co-Pilot'}
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-1">
                {stagedActions.length > 0 && (
                  <Badge
                    variant="secondary"
                    className="cursor-pointer bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30 gap-1 text-[11px]"
                    onClick={() => setIsBatchModalOpen(true)}
                  >
                    <CheckCircle2 className="h-3 w-3" />
                    <span>{stagedActions.length} Staged</span>
                  </Badge>
                )}
                <Button size="icon" variant="ghost" className="h-7 w-7" onClick={handleCloseAssistantMode}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </CardHeader>

            <CardContent className="p-3 space-y-3">
              {/* Scrollable Conversation Thread */}
              <div ref={chatScrollRef} className="h-[260px] overflow-y-auto pr-1 space-y-3 scrollbar-thin">
                {messages.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-center p-4 text-muted-foreground space-y-2">
                    <div className="p-3 rounded-full bg-primary/10 text-primary animate-pulse">
                      <Sparkles className="h-6 w-6" />
                    </div>
                    <p className="text-xs font-medium text-foreground">
                      {language === 'ta' ? 'வணக்கம்! நான் உங்கள் குரல் தோழன்.' : 'Hey there! I am your site AI co-pilot.'}
                    </p>
                    <p className="text-[11px]">
                      {language === 'ta'
                        ? 'பேச மைக்கை அழுத்தவும் அல்லது தட்டச்சு செய்யவும் (எ.கா: "மணி கொத்தனாருக்கு எவ்வளவு கொடுத்தோம்?")'
                        : 'Tap mic or speak freely like a friend (e.g. "What is the recent activity logged?")'}
                    </p>
                  </div>
                ) : (
                  messages.map(msg => (
                    <div
                      key={msg.id}
                      className={`flex gap-2 text-xs ${
                        msg.sender === 'user' ? 'justify-end' : 'justify-start'
                      }`}
                    >
                      {msg.sender === 'ai' && (
                        <div className="h-6 w-6 rounded-full bg-primary/20 text-primary flex items-center justify-center shrink-0 mt-0.5">
                          <Bot className="h-3.5 w-3.5" />
                        </div>
                      )}

                      <div
                        className={`max-w-[85%] rounded-2xl p-2.5 shadow-sm space-y-1 ${
                          msg.sender === 'user'
                            ? 'bg-primary text-primary-foreground rounded-br-none'
                            : 'bg-muted/60 text-foreground border border-border/40 rounded-bl-none'
                        }`}
                      >
                        <p className="leading-relaxed font-sans">
                          {language === 'ta' ? msg.textTa || msg.text : msg.text}
                        </p>

                        {/* Interactive Visual Guidance Widget */}
                        {msg.sender === 'ai' && renderVisualGuidanceCard(msg)}

                        <span className="text-[9px] opacity-70 block text-right">
                          {msg.timestamp}
                        </span>
                      </div>

                      {msg.sender === 'user' && (
                        <div className="h-6 w-6 rounded-full bg-amber-500/20 text-amber-500 flex items-center justify-center shrink-0 mt-0.5">
                          <User className="h-3.5 w-3.5" />
                        </div>
                      )}
                    </div>
                  ))
                )}

                {isProcessing && (
                  <div className="flex items-center gap-2 text-xs text-primary bg-primary/10 p-2 rounded-xl w-fit">
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    <span>{language === 'ta' ? 'யோசிக்கிறது...' : 'Thinking co-pilot...'}</span>
                  </div>
                )}
              </div>

              {/* Dynamic Live Visualizer Bar */}
              <div className="flex items-center justify-between px-3 py-2 bg-muted/30 rounded-xl border border-muted/30">
                <div className="flex items-center gap-2">
                  <Button
                    size="icon"
                    variant={isListening ? "destructive" : "default"}
                    onClick={toggleListening}
                    className={`h-9 w-9 rounded-full transition-all duration-300 ${
                      isListening ? 'scale-105 animate-pulse' : ''
                    }`}
                  >
                    {isListening ? <MicOff className="h-4 w-4" /> : <Mic className="h-4 w-4" />}
                  </Button>
                  <span className="text-[11px] font-semibold text-foreground">
                    {isListening
                      ? (language === 'ta' ? 'கேட்கிறது...' : 'Listening to you...')
                      : (language === 'ta' ? 'பேச மைக்கை அழுத்தவும்' : 'Tap mic for live talk')}
                  </span>
                </div>

                <Button
                  size="sm"
                  variant="ghost"
                  className={`h-7 px-2 text-[10px] gap-1 ${
                    autoListenFollowup ? 'text-emerald-500' : 'text-muted-foreground'
                  }`}
                  onClick={() => setAutoListenFollowup(!autoListenFollowup)}
                >
                  <RefreshCw className={`h-3 w-3 ${autoListenFollowup ? 'animate-spin-slow' : ''}`} />
                  <span>{autoListenFollowup ? 'Auto Live' : 'Manual'}</span>
                </Button>
              </div>

              {/* Input Form for Manual Typing / Voice Editing */}
              <form onSubmit={handleManualSubmit} className="flex gap-1.5">
                <Input
                  value={transcript}
                  onChange={(e) => setTranscript(e.target.value)}
                  placeholder={language === 'ta' ? 'கேட்க விரும்பும் கேள்வி...' : 'Type or ask follow-up question...'}
                  className="h-8 text-xs flex-1 rounded-xl"
                />
                <Button type="submit" size="sm" disabled={isProcessing || !transcript.trim()} className="h-8 px-3 rounded-xl">
                  <Send className="h-3.5 w-3.5" />
                </Button>
              </form>
            </CardContent>
          </Card>
        )}

        {/* Floating Mic Button */}
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

      {/* Batch Confirm Modal */}
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
