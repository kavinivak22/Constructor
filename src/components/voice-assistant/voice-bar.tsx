'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
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
  Boxes,
  Wallet,
  ClipboardList,
  ArrowRight,
  RefreshCw,
  Globe,
  Radio,
  SlidersHorizontal,
  ChevronRight,
  TrendingUp
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
  const { language, setLanguage, t } = useI18n();
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
  const audioPlayerRef = useRef<HTMLAudioElement | null>(null);

  // Initialize Web Speech API Recognition
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

  // Update recognition language dynamically
  useEffect(() => {
    if (recognitionRef.current) {
      recognitionRef.current.lang = language === 'ta' ? 'ta-IN' : 'en-IN';
    }
  }, [language]);

  // Auto-scroll transcript feed
  useEffect(() => {
    if (chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
    }
  }, [messages]);

  // HD Neural Audio Synthesizer
  const speakVerbalResponse = async (text: string, onEndCallback?: () => void) => {
    if (typeof window === 'undefined') return;

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

  // Render Industry-Standard Visual Guidance Cards inside AI messages
  const renderVisualGuidanceCard = (msg: ChatMessage) => {
    if (!msg.toolName && !msg.queryData) return null;

    // 1. Contractor Payment Visual Card
    if (msg.toolName === 'query_contractor_payments' && msg.queryData) {
      const d = msg.queryData;
      if (d.recentPayments && d.recentPayments.length > 0) {
        return (
          <Card className="mt-2.5 bg-card/90 border-primary/20 p-3 rounded-2xl space-y-2 shadow-lg backdrop-blur-md">
            <div className="flex items-center justify-between text-xs font-bold text-foreground">
              <span className="flex items-center gap-1.5 text-primary">
                <Wallet className="h-4 w-4" /> {language === 'ta' ? 'சமீபத்திய பரிவர்த்தனைகள்' : 'Recent Transactions'}
              </span>
              <Badge className="bg-primary/20 text-primary border-primary/30 text-[10px]">
                {d.recentPayments.length} Payments
              </Badge>
            </div>
            <div className="space-y-1.5">
              {d.recentPayments.map((p: any, idx: number) => (
                <div key={idx} className="p-2 rounded-xl bg-muted/40 text-[11px] flex items-center justify-between">
                  <div>
                    <p className="font-semibold text-foreground">{p.contractors?.name || 'Contractor'}</p>
                    <p className="text-[10px] text-muted-foreground">{p.notes || 'Payment Entry'}</p>
                  </div>
                  <span className="font-bold text-emerald-500">₹{(p.paid_amount || 0).toLocaleString('en-IN')}</span>
                </div>
              ))}
            </div>
          </Card>
        );
      }

      const progress = d.totalEarned > 0 ? Math.min(100, Math.round((d.totalPaid / d.totalEarned) * 100)) : 100;
      return (
        <Card className="mt-2.5 bg-card/90 border-primary/20 p-3 rounded-2xl space-y-2 shadow-lg backdrop-blur-md">
          <div className="flex items-center justify-between text-xs font-bold text-foreground">
            <span className="flex items-center gap-1.5 text-primary">
              <Wallet className="h-4 w-4" /> {d.contractorName || 'Contractor Ledger'}
            </span>
            <Badge variant={d.outstanding > 0 ? "destructive" : "outline"} className="text-[10px]">
              ₹{d.outstanding?.toLocaleString('en-IN')} Due
            </Badge>
          </div>
          <div className="space-y-1.5">
            <div className="flex justify-between text-[11px] text-muted-foreground">
              <span>Paid: <strong className="text-foreground">₹{d.totalPaid?.toLocaleString('en-IN')}</strong></span>
              <span>Earned: <strong className="text-foreground">₹{d.totalEarned?.toLocaleString('en-IN')}</strong></span>
            </div>
            <Progress value={progress} className="h-2 rounded-full" />
          </div>
        </Card>
      );
    }

    // 2. Daily Worklogs / Recent Activity Visual Card
    if (msg.toolName === 'query_daily_worklogs' && msg.queryData) {
      const logs = msg.queryData.worklogs || [];
      if (logs.length === 0) return null;

      return (
        <Card className="mt-2.5 bg-card/90 border-primary/20 p-3 rounded-2xl space-y-2 shadow-lg backdrop-blur-md">
          <div className="flex items-center justify-between text-xs font-bold text-foreground border-b border-border/40 pb-1.5">
            <span className="flex items-center gap-1.5 text-primary">
              <ClipboardList className="h-4 w-4" /> {language === 'ta' ? 'சமீபத்திய தள பணிகள்' : 'Site Activity Log'}
            </span>
            <Badge variant="outline" className="text-[10px] bg-primary/10 text-primary border-primary/30">
              {logs.length} Logged
            </Badge>
          </div>
          <div className="space-y-1.5">
            {logs.slice(0, 3).map((w: any, idx: number) => (
              <div key={idx} className="p-2 rounded-xl bg-muted/40 text-[11px] flex items-center justify-between">
                <div>
                  <p className="font-semibold text-foreground">{w.title || 'Site Work'}</p>
                  <p className="text-[10px] text-muted-foreground">{w.projects?.name || 'Project'} • {w.date}</p>
                </div>
                <Badge variant="secondary" className="text-[9px] bg-emerald-500/10 text-emerald-500 border-emerald-500/30">
                  Verified
                </Badge>
              </div>
            ))}
          </div>
        </Card>
      );
    }

    // 3. Material Stock Visual Card
    if (msg.toolName === 'query_material_stock' && msg.queryData) {
      const item = msg.queryData;
      if (item.inventory) {
        return (
          <Card className="mt-2.5 bg-card/90 border-primary/20 p-3 rounded-2xl space-y-2 shadow-lg backdrop-blur-md">
            <div className="flex items-center justify-between text-xs font-bold text-foreground border-b border-border/40 pb-1.5">
              <span className="flex items-center gap-1.5 text-amber-500">
                <Boxes className="h-4 w-4" /> {language === 'ta' ? 'பொருட்கள் இருப்பு' : 'Material Inventory Stock'}
              </span>
            </div>
            <div className="space-y-1.5">
              {item.inventory.slice(0, 3).map((m: any, idx: number) => (
                <div key={idx} className="p-2 rounded-xl bg-muted/40 text-[11px] flex items-center justify-between">
                  <span className="font-semibold text-foreground">{m.name}</span>
                  <Badge className="bg-emerald-500 text-white text-[10px]">{m.quantity || 0} {m.unit || 'units'}</Badge>
                </div>
              ))}
            </div>
          </Card>
        );
      }

      return (
        <Card className="mt-2.5 bg-card/90 border-primary/20 p-3 rounded-2xl space-y-1.5 shadow-lg backdrop-blur-md">
          <div className="flex items-center justify-between text-xs font-bold text-foreground">
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
        <Card className="mt-2.5 bg-amber-500/10 border-amber-500/30 p-3 rounded-2xl space-y-2 shadow-lg">
          <div className="flex items-center justify-between text-xs font-bold text-amber-600 dark:text-amber-400">
            <span className="flex items-center gap-1">
              <CheckCircle2 className="h-4 w-4" /> Staged Action Ready
            </span>
            <Button
              size="sm"
              variant="outline"
              className="h-6 text-[10px] px-2.5 border-amber-500/40 text-amber-600 hover:bg-amber-500/20 rounded-full"
              onClick={() => setIsBatchModalOpen(true)}
            >
              Confirm ({stagedActions.length})
            </Button>
          </div>
        </Card>
      );
    }

    return null;
  };

  const samplePrompts = [
    language === 'ta' ? "சமீபத்திய பரிவர்த்தனை என்ன?" : "What's the recent transaction made?",
    language === 'ta' ? "இன்று ஏதாவது பணிப்பதிவு உள்ளதா?" : "What's the recent activity logged?",
    language === 'ta' ? "சிமெண்ட் இருப்பு சரிபார்" : "Check cement stock level",
    language === 'ta' ? "நாளை பணி தயாரிப்பு பட்டியல்" : "Show work prep checklist"
  ];

  return (
    <>
      {/* Floating Fluid Orb & Controller */}
      <div className="fixed bottom-24 right-4 sm:bottom-6 sm:right-6 z-[100] flex flex-col items-end gap-3">
        {/* Industry Standard Gemini Live Window */}
        {isOpen && (
          <Card className="w-[calc(100vw-2rem)] max-w-sm sm:w-[420px] glass-card border-primary/30 shadow-2xl overflow-hidden backdrop-blur-3xl animate-in slide-in-from-bottom-6 rounded-3xl">
            {/* Header Toolbar */}
            <CardHeader className="p-3.5 border-b border-border/40 flex flex-row items-center justify-between space-y-0 bg-muted/20">
              <div className="flex items-center gap-2.5">
                <div className="relative flex h-9 w-9 items-center justify-center rounded-2xl bg-gradient-to-tr from-primary via-purple-500 to-amber-500 text-primary-foreground shadow-lg">
                  <Sparkles className="h-4 w-4 animate-spin-slow" />
                  {isListening && (
                    <span className="absolute inset-0 rounded-2xl bg-primary/40 animate-ping" />
                  )}
                </div>
                <div>
                  <CardTitle className="text-sm font-bold font-headline text-foreground flex items-center gap-1.5">
                    Constructor Live AI
                    <Badge className="bg-emerald-500 text-white text-[9px] px-1.5 py-0 h-4 rounded-full font-bold">
                      LIVE
                    </Badge>
                  </CardTitle>
                  <span className="text-[10px] text-muted-foreground font-semibold flex items-center gap-1">
                    <Radio className="h-3 w-3 text-emerald-500 animate-pulse" />
                    {language === 'ta' ? 'தமிழ் குரல் உதவி' : 'HD Neural Voice Engine'}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-1.5">
                {/* Language Switcher Pill */}
                <Button
                  size="sm"
                  variant="ghost"
                  className="h-7 text-[10px] px-2 rounded-full border border-border/50 text-muted-foreground hover:text-foreground"
                  onClick={() => setLanguage(language === 'ta' ? 'en' : 'ta')}
                >
                  <Globe className="h-3 w-3 mr-1" />
                  {language === 'ta' ? 'தமிழ்' : 'English'}
                </Button>

                {stagedActions.length > 0 && (
                  <Badge
                    variant="secondary"
                    className="cursor-pointer bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30 gap-1 text-[10px] px-2 py-0.5 rounded-full"
                    onClick={() => setIsBatchModalOpen(true)}
                  >
                    <CheckCircle2 className="h-3 w-3" />
                    <span>{stagedActions.length}</span>
                  </Badge>
                )}

                <Button size="icon" variant="ghost" className="h-7 w-7 rounded-full" onClick={handleCloseAssistantMode}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </CardHeader>

            <CardContent className="p-3.5 space-y-3">
              {/* Dynamic 3D Equalizer Visualizer */}
              <div className="flex items-center justify-between px-4 py-2.5 bg-primary/5 rounded-2xl border border-primary/10 relative overflow-hidden">
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1 h-6">
                    <span className={`w-1 bg-primary rounded-full transition-all duration-300 ${isListening ? 'animate-bounce h-6' : 'h-2'}`} />
                    <span className={`w-1 bg-purple-500 rounded-full transition-all duration-300 ${isListening ? 'animate-bounce [animation-delay:0.2s] h-8' : 'h-3'}`} />
                    <span className={`w-1 bg-amber-500 rounded-full transition-all duration-300 ${isListening ? 'animate-bounce [animation-delay:0.4s] h-5' : 'h-2'}`} />
                    <span className={`w-1 bg-emerald-500 rounded-full transition-all duration-300 ${isListening ? 'animate-bounce [animation-delay:0.1s] h-7' : 'h-3'}`} />
                  </div>
                  <span className="text-xs font-semibold text-foreground">
                    {isListening
                      ? (language === 'ta' ? 'கேட்கிறது...' : 'Gemini Listening...')
                      : (language === 'ta' ? 'குரல் உதவி தயாராக உள்ளது' : 'Gemini Live Ready')}
                  </span>
                </div>

                <Button
                  size="sm"
                  variant="ghost"
                  className={`h-6 text-[10px] px-2 rounded-full gap-1 ${
                    autoListenFollowup ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/30' : 'text-muted-foreground'
                  }`}
                  onClick={() => setAutoListenFollowup(!autoListenFollowup)}
                >
                  <RefreshCw className={`h-3 w-3 ${autoListenFollowup ? 'animate-spin-slow' : ''}`} />
                  <span>{autoListenFollowup ? 'Auto-Live' : 'Push-to-Talk'}</span>
                </Button>
              </div>

              {/* Scrollable Conversation Thread */}
              <div ref={chatScrollRef} className="h-[240px] overflow-y-auto pr-1 space-y-3 scrollbar-thin">
                {messages.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-center p-3 text-muted-foreground space-y-3">
                    <div className="p-3 rounded-2xl bg-gradient-to-tr from-primary/20 to-purple-500/20 text-primary shadow-inner">
                      <Sparkles className="h-6 w-6 animate-pulse" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-foreground">
                        {language === 'ta' ? 'வணக்கம்! நான் உங்கள் குரல் உதவி.' : 'Hey friend! Ask me anything about your site.'}
                      </p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        {language === 'ta' ? 'தள வேலைகள், பணம் செலுத்துதல் அல்லது இருப்புகளை கேட்கவும்' : 'Speak naturally or tap any sample prompt below'}
                      </p>
                    </div>

                    {/* Quick Suggestion Chips */}
                    <div className="grid grid-cols-1 gap-1.5 w-full pt-1">
                      {samplePrompts.map((p, idx) => (
                        <button
                          key={idx}
                          onClick={() => processVoiceCommand(p)}
                          className="p-2 rounded-xl bg-muted/40 hover:bg-primary/10 border border-border/40 text-[11px] text-left font-medium text-foreground transition-all flex items-center justify-between group"
                        >
                          <span>{p}</span>
                          <ChevronRight className="h-3 w-3 text-muted-foreground group-hover:text-primary transition-all" />
                        </button>
                      ))}
                    </div>
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
                        <div className="h-6 w-6 rounded-xl bg-gradient-to-tr from-primary to-purple-600 text-white flex items-center justify-center shrink-0 mt-0.5 shadow-sm">
                          <Bot className="h-3.5 w-3.5" />
                        </div>
                      )}

                      <div
                        className={`max-w-[85%] rounded-2xl p-3 shadow-sm space-y-1 ${
                          msg.sender === 'user'
                            ? 'bg-primary text-primary-foreground rounded-br-none'
                            : 'bg-muted/70 text-foreground border border-border/50 rounded-bl-none'
                        }`}
                      >
                        <p className="leading-relaxed font-sans text-xs">
                          {language === 'ta' ? msg.textTa || msg.text : msg.text}
                        </p>

                        {/* Interactive Visual Guidance Widget */}
                        {msg.sender === 'ai' && renderVisualGuidanceCard(msg)}

                        <span className="text-[9px] opacity-60 block text-right">
                          {msg.timestamp}
                        </span>
                      </div>

                      {msg.sender === 'user' && (
                        <div className="h-6 w-6 rounded-xl bg-amber-500/20 text-amber-500 flex items-center justify-center shrink-0 mt-0.5">
                          <User className="h-3.5 w-3.5" />
                        </div>
                      )}
                    </div>
                  ))
                )}

                {isProcessing && (
                  <div className="flex items-center gap-2 text-xs text-primary bg-primary/10 p-2.5 rounded-2xl w-fit border border-primary/20">
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    <span>{language === 'ta' ? 'யோசிக்கிறது...' : 'Processing Live Audio...'}</span>
                  </div>
                )}
              </div>

              {/* Input Form & Mic Button */}
              <form onSubmit={handleManualSubmit} className="flex gap-2 items-center">
                <Input
                  value={transcript}
                  onChange={(e) => setTranscript(e.target.value)}
                  placeholder={language === 'ta' ? 'கேள்வி அல்லது கட்டளை...' : 'Type or ask follow-up question...'}
                  className="h-9 text-xs flex-1 rounded-2xl bg-muted/30 border-border/50"
                />
                <Button type="submit" size="sm" disabled={isProcessing || !transcript.trim()} className="h-9 px-3 rounded-2xl">
                  <Send className="h-3.5 w-3.5" />
                </Button>
              </form>
            </CardContent>
          </Card>
        )}

        {/* Glowing Floating Mic Orb */}
        <Button
          size="icon"
          onClick={toggleListening}
          className={`h-14 w-14 rounded-3xl shadow-2xl transition-all duration-300 ${
            isListening
              ? 'bg-red-500 hover:bg-red-600 ring-4 ring-red-500/30 scale-110'
              : 'bg-gradient-to-tr from-primary via-purple-600 to-amber-500 hover:opacity-90 ring-4 ring-primary/20'
          }`}
        >
          {isListening ? (
            <MicOff className="h-6 w-6 text-white animate-pulse" />
          ) : (
            <Mic className="h-6 w-6 text-white" />
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
